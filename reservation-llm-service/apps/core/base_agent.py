from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional, Union

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import AIMessage, BaseMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

from apps.config.llm_factory import LLMFactory
from apps.utils.logger import setup_logger


class BaseAgent(ABC):

    def __init__(
        self,
        llm: Optional[BaseChatModel] = None,
    ):
        """
        에이전트 초기화

        args:
            llm: LLM 모델 인스턴스
            api_client: Back-end API 클라이언트
        """
        self.llm = llm or LLMFactory.create_llm()
        self.logger = setup_logger(self.__class__.__name__)


    @abstractmethod
    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """
        사용자가 입력을 하면 처리하고 결과를 반환해주는 추상 메서드
        args:
            user_input: 사용자 입력 메시지
            context: 처리를 위한 추가 컨텍스트

        return:
            처리결과를 딕셔너리로 반환
        """
        pass

    async def invoke_llm(
        self,
        system_prompt: str,
        user_input: str,
        chat_history: Optional[List[BaseMessage]] = None
    ) -> str:
        """
        시스템 프롬프트와 사용자 입력으로 LLM을 실행합니다.

        Args:
            system_prompt: 시스템 프롬프트
            user_input: 사용자 입력 메시지
            chat_history: 이전 대화 기록 (선택사항)

        Returns:
            LLM 응답 메시지 (문자열)
        """
        # ChatPromptTemplate을 사용하여 메시지 템플릿 구성
        template = ChatPromptTemplate.from_messages([
            ("system", "{system_prompt}"),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{user_input}")
        ])

        # 템플릿에 변수 대입하여 메시지 생성
        messages = template.format_messages(
            system_prompt=system_prompt,
            chat_history=chat_history or [],
            user_input=user_input
        )

        response = await self.llm.ainvoke(messages)
        return response.content

    async def invoke_llm_with_tools(
        self,
        system_prompt: str,
        user_input: str,
        llm_with_tools: BaseChatModel,
        chat_history: Optional[List[BaseMessage]] = None
    ) -> AIMessage:
        """
        도구가 바인딩된 LLM을 실행합니다.

        Args:
            system_prompt: 시스템 프롬프트
            user_input: 사용자 입력 메시지
            llm_with_tools: 도구가 바인딩된 LLM 인스턴스
            chat_history: 이전 대화 기록 (선택사항)

        Returns:
            AIMessage (tool_calls 포함 가능)
        """
        # ChatPromptTemplate을 사용하여 메시지 템플릿 구성
        template = ChatPromptTemplate.from_messages([
            ("system", "{system_prompt}"),
            MessagesPlaceholder(variable_name="chat_history", optional=True),
            ("human", "{user_input}")
        ])

        # 템플릿에 변수 대입하여 메시지 생성
        messages = template.format_messages(
            system_prompt=system_prompt,
            chat_history=chat_history or [],
            user_input=user_input
        )

        # 도구 바인딩된 LLM 호출
        response = await llm_with_tools.ainvoke(messages)
        return response