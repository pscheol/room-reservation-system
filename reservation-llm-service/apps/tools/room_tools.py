"""@tool 데코레이터를 사용한 회의실 관련 도구들."""

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


class SearchRoomsInput(BaseModel):
    """회의실 검색 입력 스키마."""

    building_name: Optional[str] = Field(None, description="건물 이름 (예: A동, B동)")
    room_name: Optional[str] = Field(None, description="회의실 이름 (예: 대회의실, 소회의실)")
    floor: Optional[int] = Field(None, description="층 번호 (예: 5)")
    min_capacity: Optional[int] = Field(None, description="최소 수용 인원 (예: 10)")
    status: Optional[str] = Field(None, description="회의실 상태 (예: AVAILABLE)")


class GetAvailableRoomsInput(BaseModel):
    """사용 가능한 회의실 조회 입력 스키마."""

    date: str = Field(..., description="예약 날짜 (YYYY-MM-DD)")
    start_time: str = Field(..., description="시작 시간 (HH:MM)")
    end_time: str = Field(..., description="종료 시간 (HH:MM)")
    room_name: Optional[str] = Field(None, description="회의실 이름 (예: 대회의실, 소회의실)")
    building_name: Optional[str] = Field(None, description="건물 이름 (예: A동, B동)")
    capacity: Optional[int] = Field(None, description="최소 수용 인원")


class GetRoomInput(BaseModel):
    """회의실 상세 조회 입력 스키마."""

    room_id: int = Field(..., description="조회할 회의실 ID")


class CheckRoomAvailabilityInput(BaseModel):
    """특정 회의실의 예약 가능 여부 확인 입력 스키마."""

    room_id: int = Field(..., description="확인할 회의실 ID")
    date: str = Field(..., description="예약 날짜 (YYYY-MM-DD)")
    start_time: str = Field(..., description="시작 시간 (HH:MM)")
    end_time: str = Field(..., description="종료 시간 (HH:MM)")


# ============================================================================
# @tool 데코레이터를 사용한 도구 정의
# ============================================================================


@tool(args_schema=SearchRoomsInput)
async def search_rooms(
    building_name: Optional[str] = None,
    floor: Optional[int] = None,
    room_name: Optional[str] = None,
    min_capacity: Optional[int] = None,
    status: Optional[str] = None
) -> str:
    """
    [회의실 검색]

    조건에 맞는 회의실을 찾아서 반환.
    모든 조건은 선택사항이며, 조건 없이 호출하면 모든 회의실을 반환.

    Args:
        building_name: 건물 이름 (예: "A동", "B동")
        floor: 층 번호 (예: 5)
        room_name: 회의실 이름 (예: "대회의실", "소회의실")
        min_capacity: 최소 수용 인원 (예: 10)
        status: 회의실 상태

    Returns:
        검색된 회의실 목록 (JSON 문자열)
        [
            {
                "id": 1,
                "buildingName": "A동",
                "floor": 5,
                "roomName": "대회의실",
                "capacity": 20,
                ...
            }
        ]
    """
    try:
        # 쿼리 파라미터 구성
        params = {}
        if building_name:
            params["buildingName"] = building_name
        if floor is not None:
            params["floor"] = floor
        if room_name:
            params["roomName"] = room_name
        if min_capacity is not None:
            params["minCapacity"] = min_capacity
        if status is not None:
            params["status"] = status

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/rooms"
            logger.info(f"회의실 검색 요청: {url}, params: {params}")
            response = requests.get(url, params=params, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)

        # 결과가 리스트인지 확인
        if isinstance(result, list):
            logger.info(f"회의실 검색 완료: {len(result)}개 발견")
        else:
            logger.warning(f"예상치 못한 응답 형식: {type(result)}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"회의실 검색 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"회의실 검색 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=GetAvailableRoomsInput)
async def get_available_rooms(
    date: str,
    start_time: str,
    end_time: str,
    room_name: Optional[str] = None,
    building_name: Optional[str] = None,
    capacity: Optional[int] = None,
) -> str:
    """
    [특정 시간대 사용 가능한 회의실 목록 조회]

    지정한 날짜와 시간에 예약되지 않은 회의실을 반환

    Args:
        date: 예약 날짜 (YYYY-MM-DD 형식)
        start_time: 시작 시간 (HH:MM 형식)
        end_time: 종료 시간 (HH:MM 형식)
        room_name: 회의실명
        building_name: 건물명
        capacity: 최소 수용 인원

    Returns:
        사용 가능한 회의실 목록 (JSON 문자열)
    """
    try:
        # 쿼리 파라미터 구성
        params = {
            "date": date,
            "startTime": start_time,
            "endTime": end_time,
        }
        if capacity is not None:
            params["capacity"] = capacity
        if room_name is not None:
            params["roomName"] = room_name
        if building_name is not None:
            params["buildingName"] = building_name

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/rooms/available"
            logger.info(f"사용 가능한 회의실 조회: {url}, params: {params}")
            response = requests.get(url, params=params, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"사용 가능한 회의실 조회 완료: {len(result)}개")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"사용 가능한 회의실 조회 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"사용 가능한 회의실 조회 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=GetRoomInput)
async def get_room_details(room_id: int) -> str:
    """
    [회의실 상세 정보 조회]

    Args:
        room_id: 조회할 회의실 ID

    Returns:
        회의실 상세 정보 (JSON 문자열)
    """
    try:
        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/rooms/{room_id}"
            logger.info(f"회의실 상세 조회: {url}")
            response = requests.get(url, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"회의실 상세 조회 완료: {result.get('name')}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"회의실 조회 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"회의실 조회 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=CheckRoomAvailabilityInput)
async def check_room_availability(
    room_id: int,
    date: str,
    start_time: str,
    end_time: str,
) -> str:
    """
    [특정 회의실의 예약 가능 여부 확인]

    예약 생성 전에 해당 회의실이 지정한 시간대에 사용 가능한지 확인.

    Args:
        room_id: 확인할 회의실 ID
        date: 예약 날짜 (YYYY-MM-DD)
        start_time: 시작 시간 (HH:MM)
        end_time: 종료 시간 (HH:MM)

    Returns:
        가능 여부 및 충돌하는 예약 정보 (JSON 문자열)
        {
            "available": true/false,
            "room_id": 1,
            "room_name": "대회의실",
            "conflicts": [...],  // 충돌하는 예약들
            "message": "예약 가능합니다" 또는 "예약이 불가능합니다"
        }
    """
    try:
        # 쿼리 파라미터 구성
        params = {
            "date": date,
            "startTime": start_time,
            "endTime": end_time,
        }

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/rooms/{room_id}/availability"
            logger.info(f"회의실 예약 가능 여부 확인: {url}, params: {params}")
            response = requests.get(url, params=params, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"예약 가능 여부 확인 완료: {result.get('available')}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 가능 여부 확인 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({
            "available": False,
            "room_id": room_id,
            "message": "예약 가능 여부를 확인할 수 없습니다.",
            "error": error_msg
        }, ensure_ascii=False)

    except Exception as e:
        error_msg = f"예약 가능 여부 확인 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({
            "available": False,
            "room_id": room_id,
            "message": "예약 가능 여부를 확인할 수 없습니다.",
            "error": error_msg
        }, ensure_ascii=False)


# ============================================================================
# 도구 리스트
# ============================================================================

ROOM_TOOLS = [
    search_rooms,
    get_available_rooms,
    get_room_details,
    check_room_availability,
]