from abc import ABC, abstractmethod
from typing import Any, Dict, Optional


class BaseOrchestrator(ABC):
    """오케스트레이터의 기본 인터페이스를 정의하는 추상 클래스."""

    @abstractmethod
    async def process_message(
        self,
        message: str,
        session_id: str = "default",
    ) -> Dict[str, Any]:
        """사용자 메시지를 처리하고 응답을 반환"""
        pass

    @abstractmethod
    async def get_session_info(self, session_id: str) -> Dict[str, Any]:
        """세션 정보를 조회"""
        pass

    @abstractmethod
    async def clear_session(self, session_id: str) -> None:
        """세션을 초기화"""
        pass

    @abstractmethod
    async def get_system_info(self) -> Dict[str, Any]:
        """시스템 정보를 반환"""
        pass
