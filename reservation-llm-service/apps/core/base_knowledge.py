"""지식 베이스의 기본 추상 클래스."""

from abc import ABC, abstractmethod
from typing import Any, Dict, List, Optional


class BaseKnowledge(ABC):
    """지식 베이스의 추상 기본 클래스.

        정적 데이터 제공 및 RAG 확장
    """
    @abstractmethod
    async def search(
        self, query: str, top_k: int = 5, filters: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """쿼리에 맞는 관련 정보를 검색.

        Args:
            query: 검색 쿼리
            top_k: 반환할 최대 결과 수
            filters: 검색 필터 (카테고리, 태그 등)

        Returns:
            관련 정보 목록
        """
        pass

    @abstractmethod
    async def get_by_id(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """ID로 특정 문서를 조회합니다.

        Args:
            doc_id: 문서 ID

        Returns:
            문서 정보 또는 None
        """
        pass