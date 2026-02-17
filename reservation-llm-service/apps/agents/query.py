
from typing import Any, Dict, List, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage, ToolMessage

from apps.core import BaseAgent
from apps.prompts.templates import QUERY_AGENT_PROMPT
from apps.tools.query_tools import QUERY_TOOLS
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class QueryAgent(BaseAgent):
    """예약 조회, 검색을 담당하는 전문 Agent."""

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """Query Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)

        # LLM에 도구 바인딩 (조회 도구)
        all_tools = QUERY_TOOLS
        self.llm_with_tools = self.llm.bind_tools(all_tools)
        self.tools_map = {tool.name: tool for tool in all_tools}

        self.system_prompt_template = QUERY_AGENT_PROMPT

        self.logger.info("QueryAgent 초기화 완료")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """예약 조회 및 검색을 처리합니다.

        Args:
            user_input: 사용자 입력
            context: 대화 컨텍스트 (messages, entities, collected_info 등)

        Returns:
            응답 및 도구 실행 결과를 포함한 딕셔너리
        """
        try:
            self.logger.info(f"QueryAgent 처리 시작: {user_input}")

            # 컨텍스트 정보 추출
            context = context or {}
            entities = context.get("entities", {})
            collected_info = context.get("collected_info", {})
            conversation_stage = context.get("conversation_stage", "initial")
            user_email = context.get("user_email")
            if user_email and not entities.get("email"):
                entities["email"] = user_email


            self.logger.debug(
                f"컨텍스트 - entities: {entities}, "
                f"collected_info: {collected_info}, "
                f"stage: {conversation_stage}, "
                f"user_email: {user_email}"
            )

            # 프롬프트에 동적 변수 대입
            from datetime import datetime
            today = datetime.now().strftime("%Y-%m-%d %H:%M")
            system_prompt = self.system_prompt_template.format(today=today)

            # 이전 대화 기록 준비 (email 등 추출 위해, 최근 5개)
            chat_history: List[BaseMessage] = []
            if "messages" in context:
                recent_messages = context["messages"][-5:] if len(context["messages"]) > 5 else context["messages"]
                if recent_messages:
                    self.logger.debug(f"이전 대화 기록 추가: {len(recent_messages)}개")
                    chat_history = recent_messages

            # 컨텍스트 정보를 사용자 입력에 추가
            enhanced_input = self._enhance_input_with_context(
                user_input, entities, collected_info
            )

            # BaseAgent의 invoke_llm_with_tools 사용
            response = await self.invoke_llm_with_tools(
                system_prompt=system_prompt,
                user_input=enhanced_input,
                llm_with_tools=self.llm_with_tools,
                chat_history=chat_history
            )

            # 도구 실행 결과 저장
            tool_results = []

            # 도구 호출이 있으면 실행
            if response.tool_calls:
                self.logger.info(f"도구 호출 {len(response.tool_calls)}개 발견")

                for tool_call in response.tool_calls:
                    tool_name = tool_call["name"]
                    tool_args = tool_call["args"]

                    # 도구 호출 시 email이 없으면 user_email 자동 대입
                    if user_email:
                        # email 파라미터가 필요한 도구들
                        email_required_tools = ["search_reservations", "get_upcoming_reservations"]

                        if tool_name in email_required_tools:
                            if not tool_args.get("email"):
                                tool_args["email"] = user_email
                                self.logger.info(f"도구 호출 시 user_email 자동 대입: {user_email}")

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

                # email이 필요한지 확인 (도구 호출 없는 경우)
                response_lower = response.content.lower()
                needs_email = any(
                    keyword in response_lower
                    for keyword in ["이메일", "email", "알려주", "필요"]
                )

                if needs_email:
                    return {
                        "response": response.content,
                        "tool_results": tool_results,
                        "conversation_stage": "collecting",
                        "collected_info": collected_info,
                        "next_action": "ask_email",
                    }

            # 결과 반환
            return {
                "response": response.content,
                "tool_results": tool_results,
                "conversation_stage": "completed",  # 한 번에 완료
                "collected_info": entities.copy(),  # entities 저장
            }

        except Exception as e:
            self.logger.error(f"QueryAgent 처리 중 오류: {e}", exc_info=True)
            return {
                "response": "죄송합니다. 예약 조회 중 문제가 발생했습니다.",
                "conversation_stage": "completed",
                "error": str(e),
            }

    def _enhance_input_with_context(
        self,
        user_input: str,
        entities: Dict[str, Any],
        collected_info: Dict[str, Any],
    ) -> str:
        """사용자 입력에 컨텍스트 정보를 추가.

        Args:
            user_input: 원래 사용자 입력
            entities: 추출된 엔티티
            collected_info: 수집된 정보

        Returns:
            컨텍스트가 추가된 입력
        """
        context_parts = [user_input]

        # 엔티티 정보 추가
        if entities:
            context_parts.append(f"\n[추출된 정보: {entities}]")

        # 수집된 정보 추가 (특히 email이 중요)
        if collected_info:
            context_parts.append(f"\n[이전에 수집된 정보: {collected_info}]")

        return "".join(context_parts)