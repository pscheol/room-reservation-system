

from datetime import datetime
from typing import Any, Dict, List

from apps.core.base_memory import BaseMemory
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class InMemoryStore(BaseMemory):
    """인메모리 기반 메모리 저장소"""

    def __init__(self) -> None:
        """InMemoryStore 초기화."""
        self._sessions: Dict[str, List[Dict[str, Any]]] = {}
        self._metadata: Dict[str, Dict[str, Any]] = {}
        logger.info("InMemoryStore 초기화 완료")

    async def add_message(
        self, session_id: str, role: str, content: str, metadata: Dict[str, Any] = None
    ) -> None:
        """세션에 메시지를 추가.

        Args:
            session_id: 세션 식별자
            role: 메시지 역할
            content: 메시지 내용
            metadata: 추가 메타데이터
        """
        if session_id not in self._sessions:
            self._sessions[session_id] = []
            self._metadata[session_id] = {
                "created_at": datetime.now().isoformat(),
                "message_count": 0,
            }

        message = {
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat(),
            "metadata": metadata or {},
        }

        self._sessions[session_id].append(message)
        self._metadata[session_id]["message_count"] = len(self._sessions[session_id])
        self._metadata[session_id]["updated_at"] = datetime.now().isoformat()

        logger.debug(f"메시지 추가됨 - session: {session_id}, role: {role}")

    async def get_messages(
        self, session_id: str, limit: int = 10
    ) -> List[Dict[str, Any]]:
        """세션의 메시지 기록을 조회.

        Args:
            session_id: 세션 식별자
            limit: 조회할 최대 메시지 수

        Returns:
            메시지 목록 (최근 메시지부터)
        """
        if session_id not in self._sessions:
            return []

        messages = self._sessions[session_id]
        # 최근 limit개 메시지 반환
        return messages[-limit:] if limit > 0 else messages

    async def clear_session(self, session_id: str) -> None:
        """세션의 모든 메시지를 삭제.

        Args:
            session_id: 세션 식별자
        """
        if session_id in self._sessions:
            del self._sessions[session_id]
            del self._metadata[session_id]
            logger.info(f"세션 삭제됨: {session_id}")

    async def get_session_summary(self, session_id: str) -> Dict[str, Any]:
        """세션의 요약 정보를 반환.

        Args:
            session_id: 세션 식별자

        Returns:
            세션 요약 정보
        """
        if session_id not in self._metadata:
            return {
                "session_id": session_id,
                "exists": False,
                "message_count": 0,
            }

        return {
            "session_id": session_id,
            "exists": True,
            **self._metadata[session_id],
        }

    async def session_exists(self, session_id: str) -> bool:
        """세션이 존재하는지 확인.

        Args:
            session_id: 세션 식별자

        Returns:
            세션 존재 여부
        """
        return session_id in self._sessions

    async def save_conversation_state(
        self, session_id: str, state: Dict[str, Any]
    ) -> None:
        """대화 상태를 저장

        Args:
            session_id: 세션 식별자
            state: 저장할 상태 (conversation_stage, collected_info, intent 등)
        """
        if session_id not in self._metadata:
            self._metadata[session_id] = {
                "created_at": datetime.now().isoformat(),
                "message_count": 0,
            }

        self._metadata[session_id]["conversation_state"] = state
        self._metadata[session_id]["updated_at"] = datetime.now().isoformat()

        logger.debug(f"대화 상태 저장됨 - session: {session_id}, stage: {state.get('conversation_stage')}")

    async def get_conversation_state(self, session_id: str) -> Dict[str, Any]:
        """대화 상태를 복원

        Args:
            session_id: 세션 식별자

        Returns:
            저장된 대화 상태 (없으면 빈 딕셔너리)
        """
        if session_id not in self._metadata:
            return {}

        return self._metadata[session_id].get("conversation_state", {})