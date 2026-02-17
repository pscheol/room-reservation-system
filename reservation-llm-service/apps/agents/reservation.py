from typing import Any, Dict, List, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage, ToolMessage

from apps.core import BaseAgent
from apps.prompts.templates import RESERVATION_AGENT_PROMPT
from apps.tools.reservation_tools import RESERVATION_TOOLS
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class ReservationAgent(BaseAgent):
    """예약 생성, 수정, 취소를 담당하는 전문 Agent.

    필수 파라미터 6개를 점진적으로 수집하고 conversation_stage를 관리합니다:
    - roomId (숫자)
    - reservationDate (YYYY-MM-DD)
    - startTime (HH:MM)
    - endTime (HH:MM)
    - email
    - title (기본값: "회의실 예약")
    """

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """Reservation Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)

        # LLM에 도구 바인딩 (예약 도구만)
        all_tools = RESERVATION_TOOLS
        self.llm_with_tools = self.llm.bind_tools(all_tools)
        self.tools_map = {tool.name: tool for tool in all_tools}

        self.system_prompt_template = RESERVATION_AGENT_PROMPT

        self.logger.info("ReservationAgent 초기화 완료")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """예약 생성/수정/취소를 처리합니다.

        단순화: 한 번의 실행으로 처리, 정보 부족 시 사용자에게 질문

        Args:
            user_input: 사용자 입력
            context: 대화 컨텍스트 (messages, entities)

        Returns:
            응답 및 도구 실행 결과
        """
        try:
            self.logger.info(f"ReservationAgent 처리 시작: {user_input}")

            # 컨텍스트 정보 추출
            context = context or {}
            entities = context.get("entities", {})
            collected_info = context.get("collected_info", {})
            conversation_stage = context.get("conversation_stage", "initial")
            user_email = context.get("user_email")  # ✅ 사용자 이메일 추출

            # ✅ entities에 email이 없으면 user_email 자동 대입
            if user_email and not entities.get("email"):
                entities["email"] = user_email


            self.logger.info(f"=" * 80)
            self.logger.info(f"[ReservationAgent] 사용자 입력: {user_input}")
            self.logger.info(f"[ReservationAgent] entities: {entities}")
            self.logger.info(f"[ReservationAgent] collected_info: {collected_info}")
            self.logger.info(f"[ReservationAgent] conversation_stage: {conversation_stage}")
            self.logger.info(f"[ReservationAgent] user_email: {user_email}")
            self.logger.info(f"=" * 80)

            # ✅ room_id 체크: room_id 없고 room_name 있으면 회의실 검색 필요
            room_id = entities.get("room_id") or entities.get("roomId") or \
                      collected_info.get("room_id") or collected_info.get("roomId")
            room_name = entities.get("room_name") or collected_info.get("room_name")
            room_search_attempted = collected_info.get("room_search_attempted", False)

            self.logger.info(
                f"[체크] room_id={room_id}, room_name={room_name}, "
                f"room_search_attempted={room_search_attempted}"
            )

            # room_id 없고 room_name 있고 아직 검색 안 했으면 → RoomAgent로 라우팅
            if not room_id and room_name and not room_search_attempted:
                self.logger.info(f"→ RoomAgent로 라우팅 (room_name='{room_name}')")

                updated_collected_info = collected_info.copy()
                updated_collected_info.update(entities)
                updated_collected_info["room_search_attempted"] = True

                return {
                    "response": f"'{room_name}' 회의실을 찾고 있습니다...",
                    "tool_results": [],
                    "conversation_stage": "need_room_search",
                    "collected_info": updated_collected_info,
                }

            # 프롬프트에 동적 변수 대입
            from datetime import datetime
            today = datetime.now().strftime("%Y-%m-%d %H:%M")

            # 단순화된 프롬프트
            system_prompt = self.system_prompt_template.format(today=today)

            # 이전 대화 기록 준비 (최근 3개)
            chat_history: List[BaseMessage] = []
            if "messages" in context:
                recent_messages = context["messages"][-3:] if len(context["messages"]) > 3 else context["messages"]
                if recent_messages:
                    self.logger.debug(f"이전 대화 기록 추가: {len(recent_messages)}개")
                    chat_history = recent_messages

            # entities를 입력에 추가
            enhanced_input = user_input
            if entities or collected_info:
                import json
                # entities와 collected_info 병합
                merged_info = collected_info.copy()
                merged_info.update(entities)
                enhanced_input = f"{user_input}\n[수집된 정보: {json.dumps(merged_info, ensure_ascii=False)}]"

            # LLM 호출
            response = await self.invoke_llm_with_tools(
                system_prompt=system_prompt,
                user_input=enhanced_input,
                llm_with_tools=self.llm_with_tools,
                chat_history=chat_history
            )

            # 도구 실행
            tool_results = []
            if response.tool_calls:
                self.logger.info(f"도구 호출 {len(response.tool_calls)}개 발견")

                for tool_call in response.tool_calls:
                    tool_name = tool_call["name"]
                    tool_args = tool_call["args"]

                    # 도구 호출 시 email이 없으면 user_email 자동 대입
                    if user_email and "email" in tool_args and not tool_args.get("email"):
                        tool_args["email"] = user_email
                        self.logger.info(f"도구 호출 시 user_email 자동 대입: {user_email}")
                    elif user_email and "email" not in tool_args and tool_name == "create_reservation":
                        # create_reservation은 email 필수
                        tool_args["email"] = user_email
                        self.logger.info(f"create_reservation에 user_email 자동 추가: {user_email}")

                    self.logger.info(f"도구 호출: {tool_name}, 인자: {tool_args}")

                    try:
                        tool = self.tools_map.get(tool_name)
                        if not tool:
                            raise ValueError(f"알 수 없는 도구: {tool_name}")

                        result = await tool.ainvoke(tool_args)
                        self.logger.debug(f"도구 실행 결과: {result[:200]}...")

                        tool_results.append({
                            "tool": tool_name,
                            "args": tool_args,
                            "result": result,
                        })

                    except Exception as e:
                        self.logger.error(f"{tool_name} 실행 중 오류: {e}", exc_info=True)
                        tool_results.append({
                            "tool": tool_name,
                            "args": tool_args,
                            "error": f"도구 실행 실패: {str(e)}",
                        })
            else:
                self.logger.info("도구 호출 없음")

            # 대화 상태 결정 (간단한 로직)
            updated_collected_info = collected_info.copy()
            updated_collected_info.update(entities)  # entities 병합

            # 도구 실행 여부로 stage 결정
            if tool_results:
                # 도구 실행됨 = 작업 완료
                updated_stage = "completed"
                # 도구 인자도 collected_info에 추가
                for tr in tool_results:
                    if "args" in tr:
                        updated_collected_info.update(tr["args"])
            else:
                # 도구 실행 안 됨 = 정보 수집 중
                updated_stage = "collecting"

            # 결과 반환
            return {
                "response": response.content,
                "tool_results": tool_results,
                "conversation_stage": updated_stage,
                "collected_info": updated_collected_info,
            }

        except Exception as e:
            self.logger.error(f"ReservationAgent 처리 중 오류: {e}", exc_info=True)
            return {
                "response": "죄송합니다. 예약 처리 중 문제가 발생했습니다.",
                "error": str(e),
            }
