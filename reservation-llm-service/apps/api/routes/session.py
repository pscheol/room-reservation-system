"""세션 관리 라우터."""

from typing import TYPE_CHECKING

from fastapi import APIRouter, Depends, HTTPException

from apps.api.dependencies import get_orchestrator
from apps.utils.logger import setup_logger

if TYPE_CHECKING:
    from apps.orchestrations.reservation_orchestrator import ReservationOrchestrator

logger = setup_logger(__name__)

router = APIRouter(prefix="/session", tags=["Session"])


@router.get("/{session_id}")
async def get_session_info(
    session_id: str,
    orchestrator: "ReservationOrchestrator" = Depends(get_orchestrator),
) -> dict:
    """
    세션 정보를 조회.

    Args:
        session_id: 세션 ID
        orchestrator: Orchestrator 인스턴스 (의존성 주입)

    Returns:
        세션 정보 딕셔너리

    Raises:
        HTTPException: 세션 정보 조회 실패 시
    """
    try:
        info = await orchestrator.get_session_info(session_id)
        return info
    except Exception as e:
        logger.error(f"세션 정보 조회 중 오류: {e}")
        raise HTTPException(status_code=500, detail=str(e))


@router.delete("/{session_id}")
async def clear_session(
    session_id: str,
    orchestrator: "ReservationOrchestrator" = Depends(get_orchestrator),
) -> dict:
    """
    세션 초기화.

    Args:
        session_id: 세션 ID
        orchestrator: Orchestrator 인스턴스 (의존성 주입)

    Returns:
        성공 메시지

    Raises:
        HTTPException: 세션 초기화 실패 시
    """
    try:
        await orchestrator.clear_session(session_id)
        return {"message": f"세션 {session_id}이(가) 초기화되었습니다."}
    except Exception as e:
        logger.error(f"세션 초기화 중 오류: {e}")
        raise HTTPException(status_code=500, detail=str(e))