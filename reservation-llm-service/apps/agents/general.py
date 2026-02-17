"""일반 대화를 처리하는 General Agent."""

from typing import Any, Dict, List, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

from apps.core import BaseAgent
from apps.prompts.templates import GENERAL_PROMPT
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class GeneralAgent(BaseAgent):
    """일반 대화, 인사, 도움말을 담당하는 Agent."""

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """General Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)
        self.system_prompt = GENERAL_PROMPT
        self.logger.info("GeneralAgent 초기화 완료")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """일반 대화를 처리합니다.

        Args:
            user_input: 사용자 입력
            context: 대화 컨텍스트 (messages, conversation_stage 등)

        Returns:
            응답 딕셔너리
        """
        try:
            self.logger.info(f"GeneralAgent 처리 시작: {user_input}")

            # ChatPromptTemplate을 사용하여 메시지 템플릿 구성
            template = ChatPromptTemplate.from_messages([
                ("system", "{system_prompt}"),
                MessagesPlaceholder(variable_name="chat_history", optional=True),
                ("human", "{user_input}")
            ])

            # 이전 대화 기록 준비 (최근 3개만)
            chat_history: List[BaseMessage] = []
            if context and "messages" in context:
                recent_messages = context["messages"][-3:] if len(context["messages"]) > 3 else context["messages"]
                if recent_messages:
                    self.logger.debug(f"이전 대화 기록 추가: {len(recent_messages)}개")
                    chat_history = recent_messages

            # 템플릿에 변수 대입하여 메시지 생성
            messages = template.format_messages(
                system_prompt=self.system_prompt,
                chat_history=chat_history,
                user_input=user_input
            )

            # LLM 호출
            response = await self.llm.ainvoke(messages)
            response_text = response.content

            self.logger.info("GeneralAgent 처리 완료")

            return {
                "response": response_text,
                "conversation_stage": "completed",  # 일반 대화는 항상 완료
            }

        except Exception as e:
            self.logger.error(f"GeneralAgent 처리 중 오류: {e}", exc_info=True)
            return {
                "response": "죄송합니다. 요청을 처리하는 중 문제가 발생했습니다.",
                "conversation_stage": "completed",
                "error": str(e),
            }