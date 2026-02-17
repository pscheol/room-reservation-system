"""헬스체크 및 시스템 정보 라우터."""

from typing import TYPE_CHECKING

from fastapi import APIRouter, Depends, HTTPException

from apps.api.dependencies import get_orchestrator
from apps.api.models import HealthResponse
from apps.config.settings import settings
from apps.utils.logger import setup_logger

if TYPE_CHECKING:
    from apps.orchestrations.reservation_orchestrator import ReservationOrchestrator

logger = setup_logger(__name__)

router = APIRouter(tags=["Health"])


@router.get("/health", response_model=HealthResponse)
async def health_check() -> HealthResponse:
    """
    서비스 헬스체크 엔드포인트.

    Returns:
        서비스 상태 정보
    """
    return HealthResponse(
        status="healthy",
        llm_provider=settings.llm_provider.value,
        llm_model=settings.llm_model,
        backend_url=settings.backend_api_url,
    )


@router.get("/system/info")
async def get_system_info(
    orchestrator: "ReservationOrchestrator" = Depends(get_orchestrator),
) -> dict:
    """
    시스템 정보 반환.

    Args:
        orchestrator: Orchestrator 인스턴스 (의존성 주입)

    Returns:
        시스템 정보 딕셔너리

    Raises:
        HTTPException: 시스템 정보 조회 실패 시
    """
    try:
        info = await orchestrator.get_system_info()
        return info
    except Exception as e:
        logger.error(f"시스템 정보 조회 중 오류: {e}")
        raise HTTPException(status_code=500, detail=str(e))