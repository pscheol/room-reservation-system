
from typing import Optional, Dict, Any

from langchain_core.language_models import BaseChatModel
from langchain_core.messages import HumanMessage, SystemMessage

from apps.core import BaseAgent
from apps.prompts.templates import RESULT_PROMPT
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class ResultAgent(BaseAgent):
    """도구 실행 결과를 자연스러운 한국어로 변환하는 Agent.

    이전 대화 기록을 참조하여 맥락에 맞는 응답 생성.
    """

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """Result Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)
        self.system_prompt = RESULT_PROMPT
        self.logger.info("ResultAgent 초기화 완료")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """도구 실행 결과를 사용자 친화적인 텍스트로 변환.

        Args:
            user_input: 사용자 원본 입력
            context: 컨텍스트 (tool_results, intent, messages, current_response)

        Returns:
            변환된 응답
        """
        global current_response
        try:
            self.logger.info("ResultAgent 처리 시작")

            context = context or {}
            tool_results = context.get("tool_results", [])
            intent = context.get("intent", "general")
            messages = context.get("messages", [])
            current_response = context.get("current_response", "")

            # 도구 실행 결과가 없으면 기존 응답 그대로 반환
            if not tool_results:
                self.logger.info("도구 실행 결과 없음, 기존 응답 사용")
                return {"response": current_response}

            # 도구 결과를 텍스트로 정리
            tool_results_text = ""
            for idx, tr in enumerate(tool_results, 1):
                tool_name = tr.get("tool", "알 수 없는 도구")
                result = tr.get("result", "")
                tool_results_text += f"\n{idx}. 도구: {tool_name}\n결과:\n{result}\n"

            # 이전 대화 기록을 컨텍스트로 정리
            conversation_context = ""
            if messages:
                recent_messages = messages[-4:] if len(messages) > 4 else messages
                conversation_context = "\n이전 대화:\n"
                for msg in recent_messages:
                    role = "사용자" if msg.__class__.__name__ == "HumanMessage" else "AI"
                    conversation_context += f"{role}: {msg.content}\n"

            # 프롬프트 구성
            prompt_text = f"""사용자 요청: {user_input}
                요청 의도: {intent}
                {conversation_context}

                도구 실행 결과:
                {tool_results_text}"""

            # LLM 호출
            messages_to_llm = [
                SystemMessage(content=self.system_prompt),
                HumanMessage(content=prompt_text)
            ]

            self.logger.debug(f"LLM 호출 - prompt 길이: {len(prompt_text)}")
            # 서로게이트 문자 제거 (UTF-8 인코딩 오류 방지)
            response = await self.llm.ainvoke(messages_to_llm)
            formatted_response = response.content

            self.logger.info("응답 변환 완료")

            return {
                "response": formatted_response,
            }

        except Exception as e:
            self.logger.error(f"ResultAgent 처리 중 오류: {e}", exc_info=True)
            # 오류 발생 시 기본 응답 반환
            fallback = current_response or "응답을 생성하는 중 오류가 발생했습니다."
            return {
                "response": fallback,
                "error": str(e),
            }