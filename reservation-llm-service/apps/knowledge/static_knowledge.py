from typing import Any, Dict, List, Optional

from apps.core.base_knowledge import BaseKnowledge
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class StaticKnowledge(BaseKnowledge):


    def __init__(self) -> None:
        """StaticKnowledge 초기화."""
        self._faq_data = self._load_faq()
        self._system_info = self._load_system_info()

    @staticmethod
    def _load_faq() -> List[Dict[str, Any]]:
        """FAQ 정보 로드.

        Returns:
            FAQ 목록
        """
        return [
            {
                "id": "faq_001",
                "category": "예약",
                "question": "회의실을 예약하려면 어떻게 해야 하나요?",
                "answer": "회의실 예약은 다음 정보가 필요합니다: 회의실 ID, 예약자 이메일, 예약 제목, 날짜, 시작 시간, 종료 시간. '내일 오후 2시에 회의실 1번 예약해줘'와 같이 요청하시면 됩니다.",
            },
            {
                "id": "faq_002",
                "category": "예약",
                "question": "예약 가능 시간은 언제인가요?",
                "answer": "회의실 예약 가능 시간은 오전 6시부터 오후 11시까지입니다. 최소 30분, 최대 8시간까지 예약할 수 있습니다.",
            },
            {
                "id": "faq_003",
                "category": "예약",
                "question": "예약을 취소하려면 어떻게 하나요?",
                "answer": "예약 ID와 예약자 이메일을 알려주시면 취소할 수 있습니다. 단, 예약 시작 10분 전까지만 취소 가능합니다.",
            },
            {
                "id": "faq_004",
                "category": "회의실",
                "question": "사용 가능한 회의실을 어떻게 찾나요?",
                "answer": "건물명, 층, 필요 인원 수, 날짜/시간을 알려주시면 조건에 맞는 회의실을 찾아드립니다.",
            },
            {
                "id": "faq_005",
                "category": "조회",
                "question": "내 예약 목록을 확인하려면?",
                "answer": "이메일 주소를 알려주시면 해당 이메일로 된 예약 목록을 확인하실 수 있습니다.",
            },
        ]

    @staticmethod
    def _load_system_info() -> Dict[str, Any]:
        """시스템 정보 로드.

        Returns:
            시스템 정보
        """
        return {
            "name": "회의실 예약 시스템",
            "version": "1.0.0",
            "description": "회의실 예약 챗봇 서비스",
            "capabilities": [
                "회의실 검색 및 추천",
                "회의실 예약 생성/수정/취소",
                "예약 조회 및 검색",
                "실시간 가용성 확인",
            ],
            "reservation_rules": {
                "available_hours": "06:00 ~ 23:00",
                "booking_period": "현재일 ~ 3개월 이내",
                "min_duration": "30분",
                "max_duration": "8시간",
                "cancellation_deadline": "시작 10분 전",
            },
            "required_info": {
                "reservation": [
                    "회의실 ID",
                    "예약자 이메일",
                    "예약 제목",
                    "날짜 (YYYY-MM-DD)",
                    "시작 시간 (HH:MM)",
                    "종료 시간 (HH:MM)",
                ],
                "search": ["건물명 (선택)", "층 (선택)", "최소 인원 (선택)"],
                "query": ["예약자 이메일", "예약 ID"],
            },
        }

    async def search(
        self, query: str, top_k: int = 5, filters: Optional[Dict[str, Any]] = None
    ) -> List[Dict[str, Any]]:
        """쿼리에 맞는 관련 정보를 검색.

        Args:
            query: 검색 쿼리
            top_k: 반환할 최대 결과 수
            filters: 검색 필터

        Returns:
            관련 정보 목록
        """
        # 간단한 키워드 매칭
        query_lower = query.lower()
        results = []

        for faq in self._faq_data:
            # 카테고리 필터
            if filters and "category" in filters:
                if faq["category"] != filters["category"]:
                    continue

            # 키워드 매칭
            if (
                query_lower in faq["question"].lower()
                or query_lower in faq["answer"].lower()
            ):
                results.append(faq)

        return results[:top_k]

    def get_by_id(self, doc_id: str) -> Optional[Dict[str, Any]]:
        """ID로 특정 문서를 조회.

        Args:
            doc_id: 문서 ID

        Returns:
            문서 정보 또는 None
        """
        for faq in self._faq_data:
            if faq["id"] == doc_id:
                return faq
        return None

    def get_faq(self, category: Optional[str] = None) -> List[Dict[str, Any]]:
        """FAQ 검색.

        Args:
            category: FAQ 카테고리

        Returns:
            FAQ 목록
        """
        if category is None:
            return self._faq_data

        return [faq for faq in self._faq_data if faq["category"] == category]

    def get_system_info(self) -> Dict[str, Any]:
        """시스템 정보 및 가이드 반환.

        Returns:
            시스템 정보
        """
        return self._system_info