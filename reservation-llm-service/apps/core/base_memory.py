"""메모리 시스템 기본 추상 클래스."""

from abc import ABC, abstractmethod
from typing import Any, Dict, List


class BaseMemory(ABC):
    """메모리 시스템의 추상 기본 클래스.
    """

    @abstractmethod
    async def add_message(
        self, session_id: str, role: str, content: str, metadata: Dict[str, Any] = None
    ) -> None:
        """세션에 메시지를 추가합니다.

        Args:
            session_id: 세션 식별자
            role: 메시지 역할 ("user" | "assistant" | "system")
            content: 메시지 내용
            metadata: 추가 메타데이터 (선택사항)
        """
        pass

    @abstractmethod
    async def get_messages(
        self, session_id: str, limit: int = 10
    ) -> List[Dict[str, Any]]:
        """세션의 메시지 기록을 조회합니다.

        Args:
            session_id: 세션 식별자
            limit: 조회할 최대 메시지 수 (최근 메시지부터)

        Returns:
            메시지 목록 (시간순 정렬)
        """
        pass

    @abstractmethod
    async def clear_session(self, session_id: str) -> None:
        """세션의 모든 메시지를 삭제합니다.

        Args:
            session_id: 세션 식별자
        """
        pass

    @abstractmethod
    async def get_session_summary(self, session_id: str) -> Dict[str, Any]:
        """세션의 요약 정보를 반환합니다.

        Args:
            session_id: 세션 식별자

        Returns:
            세션 요약 정보 (메시지 수, 생성 시간 등)
        """
        pass

    @abstractmethod
    async def session_exists(self, session_id: str) -> bool:
        """세션이 존재하는지 확인합니다.

        Args:
            session_id: 세션 식별자

        Returns:
            세션 존재 여부
        """
        pass