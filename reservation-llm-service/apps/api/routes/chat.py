"""채팅 관련 라우터."""

import json
from typing import TYPE_CHECKING, AsyncGenerator

from fastapi import APIRouter, Depends, HTTPException
from fastapi.responses import StreamingResponse

from apps.api.dependencies import get_orchestrator
from apps.api.models import ChatRequest, ChatResponse
from apps.utils.logger import setup_logger

if TYPE_CHECKING:
    from apps.orchestrations.reservation_orchestrator import ReservationOrchestrator

logger = setup_logger(__name__)

router = APIRouter(prefix="/chat", tags=["Chat"])


@router.post("", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    orchestrator: "ReservationOrchestrator" = Depends(get_orchestrator),
) -> ChatResponse:
    """
    스트리밍이 아닌 응답을 위한 채팅 엔드포인트.

    Args:
        request: 사용자 메시지와 대화 기록을 포함하는 채팅 요청
        orchestrator: Orchestrator 인스턴스 (의존성 주입)

    Returns:
        어시스턴트의 메시지를 포함하는 채팅 응답

    Raises:
        HTTPException: 채팅 요청 처리 실패 시
    """
    try:
        logger.info(f"채팅 요청 처리 중: {request.message[:100]}...")

        # Orchestrator를 통해 메시지 처리
        result = await orchestrator.process_message(
            message=request.message,
            session_id=request.session_id,
            user_email=request.user_email,
        )

        logger.info("채팅 요청이 성공적으로 처리되었습니다.")

        # 응답 구성
        return ChatResponse(
            response=result.get("response", ""),
            agent=result.get("intent", "general"),
        )

    except Exception as e:
        logger.error(f"채팅 요청 처리 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))


async def generate_streaming_response(
    message: str,
    session_id: str,
    user_email: str,
    orchestrator: "ReservationOrchestrator",
) -> AsyncGenerator[str, None]:
    """
    스트리밍 응답을 생성.

    Args:
        message: 사용자 메시지
        session_id: 세션 ID
        user_email: 사용자 이메일
        orchestrator: Orchestrator 인스턴스

    Yields:
        SSE 형식의 청크 데이터
    """
    try:
        # Orchestrator를 통해 스트리밍 응답 받기
        async for chunk in orchestrator.process_message_stream(
            message=message, session_id=session_id, user_email=user_email
        ):
            # 각 청크를 SSE 형식으로 전송
            chunk_data = json.dumps(chunk, ensure_ascii=False)
            yield f"data: {chunk_data}\n\n"

    except Exception as e:
        logger.error(f"스트리밍 응답 중 오류 발생: {e}")
        error_data = {"type": "error", "error": str(e)}
        yield f"data: {json.dumps(error_data, ensure_ascii=False)}\n\n"
    finally:
        # 스트리밍 종료를 알림
        yield "data: [DONE]\n\n"


@router.post("/stream")
async def chat_stream(
    request: ChatRequest,
    orchestrator: "ReservationOrchestrator" = Depends(get_orchestrator),
) -> StreamingResponse:
    """
    스트리밍 응답을 위한 채팅 엔드포인트.

    Args:
        request: 사용자 메시지와 대화 기록을 포함하는 채팅 요청
        orchestrator: Orchestrator 인스턴스 (의존성 주입)

    Returns:
        SSE 스트리밍 응답

    Raises:
        HTTPException: 스트리밍 채팅 요청 처리 실패 시
    """
    try:
        logger.info(f"스트리밍 채팅 요청 처리 중: {request.message[:100]}...")

        return StreamingResponse(
            generate_streaming_response(
                request.message, request.session_id, request.user_email, orchestrator
            ),
            media_type="text/event-stream",
        )

    except Exception as e:
        logger.error(f"스트리밍 채팅 요청 처리 중 오류 발생: {e}")
        raise HTTPException(status_code=500, detail=str(e))