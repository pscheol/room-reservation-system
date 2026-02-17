"""FastAPI 의존성 주입 모듈."""

from typing import TYPE_CHECKING

from fastapi import Request

if TYPE_CHECKING:
    from apps.orchestrations.reservation_orchestrator import ReservationOrchestrator


def get_orchestrator(request: Request) -> "ReservationOrchestrator":
    """
    Orchestrator 의존성 주입.

    앱 상태에서 orchestrator를 가져옵니다.
    lifespan에서 app.state.orchestrator로 설정됩니다.

    Args:
        request: FastAPI Request 객체

    Returns:
        ReservationOrchestrator 인스턴스

    Raises:
        RuntimeError: Orchestrator가 초기화되지 않았을 경우
    """
    orchestrator = getattr(request.app.state, "orchestrator", None)
    if orchestrator is None:
        raise RuntimeError("Orchestrator가 초기화되지 않았습니다.")
    return orchestrator