
from typing import Any, Dict, List, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage, ToolMessage

from apps.core import BaseAgent
from apps.prompts.templates import ROOM_AGENT_PROMPT
from apps.tools.room_tools import ROOM_TOOLS
from apps.utils.logger import setup_logger
from datetime import datetime
logger = setup_logger(__name__)


class RoomAgent(BaseAgent):
    """회의실 검색, 가용성 확인, 상세 조회를 담당하는 전문 Agent.

    사용 가능한 도구:
    - search_rooms: 조건에 맞는 회의실 검색
    - get_available_rooms: 특정 시간대 사용 가능한 회의실 조회
    - get_room_details: 회의실 상세 정보 조회
    """

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """Room Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)

        # LLM에 도구 바인딩 (회의실 도구)
        all_tools = ROOM_TOOLS
        self.llm_with_tools = self.llm.bind_tools(all_tools)
        self.tools_map = {tool.name: tool for tool in all_tools}

        self.system_prompt_template = ROOM_AGENT_PROMPT

        self.logger.info("RoomAgent 초기화 완료")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """회의실 검색 및 가용성 확인을 처리합니다.

        Args:
            user_input: 사용자 입력
            context: 대화 컨텍스트 (messages, entities)

        Returns:
            응답 및 도구 실행 결과
        """
        try:
            self.logger.info(f"RoomAgent 처리 시작: {user_input}")

            # 컨텍스트 정보 추출
            context = context or {}
            entities = context.get("entities", {})
            collected_info = context.get("collected_info", {})

            # ✅ collected_info의 정보를 entities에 병합 (room_name 등)
            merged_entities = collected_info.copy()
            merged_entities.update(entities)
            entities = merged_entities

            self.logger.debug(f"컨텍스트 - entities: {entities}, collected_info: {collected_info}")

            # 프롬프트에 동적 변수 대입

            today = datetime.now().strftime("%Y-%m-%d %H:%M")
            system_prompt = self.system_prompt_template.format(today=today)

            # 이전 대화 기록 준비 (컨텍스트 유지, 최근 5개)
            chat_history: List[BaseMessage] = []
            if "messages" in context:
                recent_messages = context["messages"][-5:] if len(context["messages"]) > 5 else context["messages"]
                if recent_messages:
                    self.logger.debug(f"이전 대화 기록 추가: {len(recent_messages)}개")
                    chat_history = recent_messages

            # entities를 입력에 추가
            enhanced_input = user_input
            if entities:
                import json
                enhanced_input = f"{user_input}\n[추출된 정보: {json.dumps(entities, ensure_ascii=False)}]"

            # BaseAgent의 invoke_llm_with_tools 사용
            response = await self.invoke_llm_with_tools(
                system_prompt=system_prompt,
                user_input=enhanced_input,
                llm_with_tools=self.llm_with_tools,
                chat_history=chat_history
            )

            # 도구 실행 결과 저장
            tool_results = []
            logger.debug(f"## room agent response : {response}")
            # 도구 호출이 있으면 실행
            if response.tool_calls:
                self.logger.info(f"도구 호출 {len(response.tool_calls)}개 발견")

                for tool_call in response.tool_calls:
                    tool_name = tool_call["name"]
                    tool_args = tool_call["args"]

                    self.logger.info(f"도구 호출: {tool_name}, 인자: {tool_args}")

                    # 도구 실행
                    try:
                        tool = self.tools_map.get(tool_name)
                        if not tool:
                            raise ValueError(f"알 수 없는 도구: {tool_name}")

                        result = await tool.ainvoke(tool_args)
                        self.logger.debug(f"도구 실행 결과: {result[:200]}...")

                        # 도구 결과 저장
                        tool_results.append({
                            "tool": tool_name,
                            "args": tool_args,
                            "result": result,
                        })

                    except Exception as e:
                        error_msg = f"도구 실행 실패: {str(e)}"
                        self.logger.error(f"{tool_name} 실행 중 오류: {e}", exc_info=True)

                        tool_results.append({
                            "tool": tool_name,
                            "args": tool_args,
                            "error": error_msg,
                        })
            else:
                self.logger.info("도구 호출 없음")

            # collected_info 구성: entities + 도구 결과에서 추출한 room_id
            updated_collected_info = collected_info.copy()
            updated_collected_info.update(entities)

            # 도구 실행 결과에서 room_id 추출 (예약 프로세스를 위해)
            if tool_results:
                import json
                for tr in tool_results:
                    result_str = tr.get("result", "")
                    try:
                        # JSON 파싱 시도
                        result_data = json.loads(result_str) if isinstance(result_str, str) else result_str

                        # 결과가 리스트면 첫 번째 항목의 room_id 추출
                        if isinstance(result_data, list) and len(result_data) > 0:
                            first_room = result_data[0]
                            if "id" in first_room:
                                updated_collected_info["room_id"] = first_room["id"]
                                updated_collected_info["room_name"] = first_room.get("roomName", "")
                                self.logger.info(
                                    f"회의실 검색 결과에서 room_id 추출: {first_room['id']} ({first_room.get('roomName')})"
                                )
                                break
                        # 결과가 단일 객체면 직접 room_id 추출
                        elif isinstance(result_data, dict) and "id" in result_data:
                            updated_collected_info["room_id"] = result_data["id"]
                            updated_collected_info["room_name"] = result_data.get("roomName", "")
                            self.logger.info(
                                f"회의실 검색 결과에서 room_id 추출: {result_data['id']} ({result_data.get('roomName')})"
                            )
                            break
                    except (json.JSONDecodeError, TypeError, KeyError) as e:
                        self.logger.debug(f"room_id 추출 실패: {e}")
                        continue

            # 결과 반환 (대화 상태 포함)
            return {
                "response": response.content,
                "tool_results": tool_results,
                "conversation_stage": "completed",  # 한 번에 완료
                "collected_info": updated_collected_info,  # room_id 포함
            }

        except Exception as e:
            self.logger.error(f"RoomAgent 처리 중 오류: {e}", exc_info=True)
            return {
                "response": "죄송합니다. 회의실 검색 중 문제가 발생했습니다.",
                "error": str(e),
            }