

import asyncio
import json
from typing import Optional

import requests
from langchain_core.tools import tool
from pydantic import BaseModel, Field

from apps.config.settings import settings
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)

# 백엔드 API URL
BACKEND_API_URL = settings.backend_api_url


# ============================================================================
# 도구 입력 스키마 정의
# ============================================================================


class GetReservationInput(BaseModel):
    """예약 조회 입력 스키마."""

    reservation_id: int = Field(..., description="조회할 예약 ID")


class SearchReservationsInput(BaseModel):
    """예약 검색 입력 스키마."""

    email: Optional[str] = Field(None, description="예약자 이메일")
    room_name: Optional[str] = Field(None, description="회의실 이름")
    building_name: Optional[str] = Field(None, description="건물 이름")
    start_date: Optional[str] = Field(None, description="시작 날짜 (YYYY-MM-DD)")
    end_date: Optional[str] = Field(None, description="종료 날짜 (YYYY-MM-DD)")
    status: Optional[str] = Field(None, description="예약 상태 (CONFIRMED, CANCELLED 등)")


class GetUpcomingReservationsInput(BaseModel):
    """다가오는 예약 조회 입력 스키마."""

    email: str = Field(..., description="조회할 사용자의 이메일")


# ============================================================================
# @tool 데코레이터를 사용한 도구 정의
# ============================================================================


@tool(args_schema=GetReservationInput)
async def get_reservation(reservation_id: int) -> str:
    """
    특정 예약의 상세 정보를 조회.

    Args:
        reservation_id: 조회할 예약 ID

    Returns:
        예약 상세 정보 (JSON 문자열)
    """
    try:
        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations/{reservation_id}"
            logger.info(f"예약 조회: {url}")
            response = requests.get(url, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"예약 조회 완료: ID={reservation_id}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 조회 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"예약 조회 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=SearchReservationsInput)
async def search_reservations(
    email: Optional[str] = None,
    room_name: Optional[str] = None,
    building_name: Optional[str] = None,
    start_date: Optional[str] = None,
    end_date: Optional[str] = None,
    status: Optional[str] = None,
) -> str:
    """
    조건에 맞는 예약 목록을 검색.

    여러 조건을 조합하여 예약을 검색할 수 있다.

    Args:
        email: 예약자 이메일 (선택사항)
        room_name: 회의실 이름 (선택사항)
        building_name: 건물 이름 (선택사항)
        start_date: 시작 날짜 (YYYY-MM-DD 형식, 선택사항)
        end_date: 종료 날짜 (YYYY-MM-DD 형식, 선택사항)
        status: 예약 상태 (선택사항)

    Returns:
        검색된 예약 목록 (JSON 문자열)
    """
    try:
        # 쿼리 파라미터 구성
        params = {}
        if email:
            params["email"] = email
        if room_name:
            params["roomName"] = room_name
        if building_name:
            params["buildingName"] = building_name
        if start_date:
            params["startDate"] = start_date
        if end_date:
            params["endDate"] = end_date
        if status:
            params["status"] = status

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations"
            logger.info(f"예약 검색 요청: {url}, params: {params}")
            response = requests.get(url, params=params, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"예약 검색 완료: {len(result)}개 발견")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 검색 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"예약 검색 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=GetUpcomingReservationsInput)
async def get_upcoming_reservations(email: str) -> str:
    """
    사용자의 다가오는 예약 목록을 조회.

    현재 시각 이후의 예약만 반환ㄴ.

    Args:
        email: 조회할 사용자의 이메일

    Returns:
        다가오는 예약 목록 (JSON 문자열)
    """
    try:
        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations/upcoming"
            logger.info(f"다가오는 예약 조회: {url}, email={email}")
            response = requests.get(url, params={"email": email}, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"다가오는 예약 조회 완료: {len(result)}개")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"다가오는 예약 조회 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"다가오는 예약 조회 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


# ============================================================================
# 도구 리스트
# ============================================================================

QUERY_TOOLS = [
    get_reservation,
    search_reservations,
    get_upcoming_reservations,
]