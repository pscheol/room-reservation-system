
import json
from datetime import datetime
from typing import Any, Dict, List, Optional

import redis.asyncio as redis

from apps.config.settings import settings
from apps.core.base_memory import BaseMemory
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class RedisMemory(BaseMemory):
    """레디스 기반 메모리 저장소"""
    def __init__(
        self,
        redis_client: Optional[redis.Redis] = None,
        ttl: int = 86400,  # 24시간
    ) -> None:
        """RedisMemory 초기화.

        Args:
            redis_client: Redis 클라이언트 (없으면 새로 생성)
            ttl: 세션 만료 시간 (초 단위, 기본 24시간)
        """
        self.ttl = ttl
        self._client = redis_client
        logger.info("RedisMemory 초기화 완료")

    async def _get_client(self) -> redis.Redis:
        """Redis 클라이언트 호출 (Lazy Loading).

        Returns:
            Redis 클라이언트 인스턴스
        """
        if self._client is None:
            self._client = redis.Redis(
                host=settings.redis_host,
                port=settings.redis_port,
                db=settings.redis_db,
                password=settings.redis_password,
                decode_responses=True,
            )
            logger.info("Redis 클라이언트 연결됨")
        return self._client

    def _get_messages_key(self, session_id: str) -> str:
        """메시지 리스트 키를 생성합니다.

        Args:
            session_id: 세션 식별자

        Returns:
            Redis 키
        """
        return f"chat:session:{session_id}:messages"

    def _get_metadata_key(self, session_id: str) -> str:
        """메타데이터 키를 생성합니다.

        Args:
            session_id: 세션 식별자

        Returns:
            Redis 키
        """
        return f"chat:session:{session_id}:metadata"

    async def add_message(
        self, session_id: str, role: str, content: str, metadata: Dict[str, Any] = None
    ) -> None:
        """세션에 메시지를 추가

        Args:
            session_id: 세션 식별자
            role: 메시지 역할
            content: 메시지 내용
            metadata: 추가 메타데이터
        """
        client = await self._get_client()
        messages_key = self._get_messages_key(session_id)
        metadata_key = self._get_metadata_key(session_id)

        message = {
            "role": role,
            "content": content,
            "timestamp": datetime.now().isoformat(),
            "metadata": metadata or {},
        }

        # 메시지를 리스트에 추가
        await client.rpush(messages_key, json.dumps(message, ensure_ascii=False))

        # 메타데이터 업데이트
        message_count = await client.llen(messages_key)
        session_metadata = {
            "message_count": message_count,
            "updated_at": datetime.now().isoformat(),
        }

        # 세션이 처음 생성되는 경우
        if message_count == 1:
            session_metadata["created_at"] = datetime.now().isoformat()

        await client.hset(
            metadata_key, mapping={k: str(v) for k, v in session_metadata.items()}
        )

        # TTL 설정
        await client.expire(messages_key, self.ttl)
        await client.expire(metadata_key, self.ttl)

        logger.debug(f"메시지 추가됨 - session: {session_id}, role: {role}")

    async def get_messages(
        self, session_id: str, limit: int = 10
    ) -> List[Dict[str, Any]]:
        """세션의 메시지 기록을 조회

        Args:
            session_id: 세션 식별자
            limit: 조회할 최대 메시지 수

        Returns:
            메시지 목록 (최근 메시지부터)
        """
        client = await self._get_client()
        messages_key = self._get_messages_key(session_id)

        # 최근 limit개 메시지 조회
        start = -limit if limit > 0 else 0
        messages_json = await client.lrange(messages_key, start, -1)

        messages = [json.loads(msg) for msg in messages_json]
        return messages

    async def clear_session(self, session_id: str) -> None:
        """세션의 모든 메시지를 삭제

        Args:
            session_id: 세션 식별자
        """
        client = await self._get_client()
        messages_key = self._get_messages_key(session_id)
        metadata_key = self._get_metadata_key(session_id)

        await client.delete(messages_key, metadata_key)
        logger.info(f"세션 삭제됨: {session_id}")

    async def get_session_summary(self, session_id: str) -> Dict[str, Any]:
        """세션의 요약 정보를 반환

        Args:
            session_id: 세션 식별자

        Returns:
            세션 요약 정보
        """
        client = await self._get_client()
        metadata_key = self._get_metadata_key(session_id)

        metadata = await client.hgetall(metadata_key)

        if not metadata:
            return {
                "session_id": session_id,
                "exists": False,
                "message_count": 0,
            }

        return {
            "session_id": session_id,
            "exists": True,
            "message_count": int(metadata.get("message_count", 0)),
            "created_at": metadata.get("created_at"),
            "updated_at": metadata.get("updated_at"),
        }

    async def session_exists(self, session_id: str) -> bool:
        """세션이 존재하는지 확인.

        Args:
            session_id: 세션 식별자

        Returns:
            세션 존재 여부
        """
        client = await self._get_client()
        messages_key = self._get_messages_key(session_id)
        return await client.exists(messages_key) > 0

    async def close(self) -> None:
        """Redis 연결을 종료"""
        if self._client:
            await self._client.close()
            logger.info("Redis 연결 종료됨")