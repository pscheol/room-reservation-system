"""@tool 데코레이터를 사용한 예약 관련 도구들."""

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


class CreateReservationInput(BaseModel):
    """예약 생성 입력 스키마."""

    room_id: int = Field(..., description="예약할 회의실 ID")
    email: str = Field(..., description="예약자 이메일")
    title: str = Field(..., description="예약 제목")
    date: str = Field(..., description="예약 날짜 (YYYY-MM-DD)")
    start_time: str = Field(..., description="시작 시간 (HH:MM)")
    end_time: str = Field(..., description="종료 시간 (HH:MM)")
    contents: str = Field(..., description="예약 내용/설명 (필수)")
    participants: list[str] = Field(..., description="참여자 이메일 목록 (필수, 최소 1명)")


class UpdateReservationInput(BaseModel):
    """예약 수정 입력 스키마."""

    reservation_id: int = Field(..., description="수정할 예약 ID")
    title: Optional[str] = Field(None, description="새 제목")
    contents: Optional[str] = Field(None, description="새 내용")
    date: Optional[str] = Field(None, description="새 날짜 (YYYY-MM-DD)")
    start_time: Optional[str] = Field(None, description="새 시작 시간 (HH:MM)")
    end_time: Optional[str] = Field(None, description="새 종료 시간 (HH:MM)")


class CancelReservationInput(BaseModel):
    """예약 취소 입력 스키마."""

    reservation_id: int = Field(..., description="취소할 예약 ID")
    email: str = Field(..., description="예약자 이메일 (확인용)")


# ============================================================================
# @tool 데코레이터를 사용한 도구 정의
# ============================================================================


@tool(args_schema=CreateReservationInput)
async def create_reservation(
    room_id: int,
    email: str,
    title: str,
    date: str,
    start_time: str,
    end_time: str,
    contents: str,
    participants: list[str],
) -> str:
    """
    [회의실 예약 생성]

    지정한 회의실과 시간에 새로운 예약을 생성.

    Args:
        room_id: 예약할 회의실 ID
        email: 예약자 이메일
        title: 예약 제목
        date: 예약 날짜 (YYYY-MM-DD)
        start_time: 시작 시간 (HH:MM)
        end_time: 종료 시간 (HH:MM)
        contents: 예약 내용/설명
        participants: 참여자 이메일 목록

    Returns:
        생성된 예약 정보 (JSON 문자열)
    """
    try:
        # 요청 데이터 구성
        request_data = {
            "roomId": room_id,
            "email": email,
            "title": title,
            "reservationDate": date,
            "startTime": start_time,
            "endTime": end_time,
            "contents": contents,
            "participants": [{"email": p, "type": "ATTENDEE"} for p in participants]
        }

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations"
            logger.info(f"예약 생성 요청: {url}, data: {request_data}")
            response = requests.post(url, json=request_data, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        reservation_id = result.get('id')
        logger.info(f"예약 생성 완료: ID={reservation_id}")


        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 생성 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"예약 생성 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=UpdateReservationInput)
async def update_reservation(
    reservation_id: int,
    title: Optional[str] = None,
    contents: Optional[str] = None,
    date: Optional[str] = None,
    start_time: Optional[str] = None,
    end_time: Optional[str] = None,
) -> str:
    """
    [기존 예약 수정]

    예약의 제목, 내용, 날짜, 시간 등을 변경.

    Args:
        reservation_id: 수정할 예약 ID
        title: 수정할 제목
        contents: 수정할 내용
        date: 변경 날짜 (YYYY-MM-DD 형식)
        start_time: 변경 시작 시간 (HH:MM)
        end_time: 변경 종료 시간 (HH:MM)

    Returns:
        수정된 예약 정보 (JSON 문자열)
    """
    try:
        # 요청 데이터 구성 (변경할 필드만 포함)
        request_data = {}

        if title:
            request_data["title"] = title
        if contents:
            request_data["contents"] = contents
        if date:
            request_data["reservationDate"] = date
        if start_time:
            request_data["startTime"] = start_time
        if end_time:
            request_data["endTime"] = end_time

        if not request_data:
            return json.dumps(
                {"error": "수정할 항목이 없습니다."}, ensure_ascii=False
            )

        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations/{reservation_id}"
            logger.info(f"예약 수정 요청: {url}, data: {request_data}")
            response = requests.put(url, json=request_data, timeout=30.0)
            response.raise_for_status()
            return response.json()

        result = await asyncio.to_thread(_make_request)
        logger.info(f"예약 수정 완료: ID={reservation_id}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 수정 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"예약 수정 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


@tool(args_schema=CancelReservationInput)
async def cancel_reservation(
    reservation_id: int,
    email: str,
) -> str:
    """
    [예약 취소]

    지정한 예약 취소 처리. 예약자의 이메일로 권한 확인.

    Args:
        reservation_id: 취소할 예약 ID
        email: 예약자 이메일 (확인용)

    Returns:
        취소 처리 결과 (JSON 문자열)
    """
    try:
        # API 호출을 비동기로 실행
        def _make_request():
            url = f"{BACKEND_API_URL}/api/reservations/{reservation_id}"
            logger.info(f"예약 취소 요청: {url}, email={email}")
            response = requests.delete(url, params={"email": email}, timeout=30.0)
            response.raise_for_status()

        await asyncio.to_thread(_make_request)

        result = {"status": "cancelled", "reservation_id": reservation_id}
        logger.info(f"예약 취소 완료: ID={reservation_id}")

        return json.dumps(result, ensure_ascii=False)

    except requests.HTTPError as e:
        error_msg = f"예약 취소 실패 ({e.response.status_code}): {e.response.text}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)
    except Exception as e:
        error_msg = f"예약 취소 중 오류 발생: {str(e)}"
        logger.error(error_msg)
        return json.dumps({"error": error_msg}, ensure_ascii=False)


# ============================================================================
# 도구 리스트
# ============================================================================

RESERVATION_TOOLS = [
    create_reservation,
    update_reservation,
    cancel_reservation,
]