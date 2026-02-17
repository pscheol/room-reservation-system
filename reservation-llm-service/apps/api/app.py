"""FastAPI 애플리케이션 팩토리."""

from contextlib import asynccontextmanager
from pathlib import Path
from typing import AsyncGenerator

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from fastapi.staticfiles import StaticFiles

from apps.api.routes import chat_router, health_router, session_router, ui_router
from apps.config.settings import settings
from apps.orchestrations.reservation_orchestrator import ReservationOrchestrator
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)

# 현재 파일 위치를 기준으로 절대 경로 설정
BASE_DIR = Path(__file__).resolve().parent.parent.parent


@asynccontextmanager
async def lifespan(app: FastAPI) -> AsyncGenerator[None, None]:
    """
    애플리케이션 라이프사이클 관리.

    시작 시 Orchestrator를 초기화하고,
    종료 시 리소스를 정리합니다.

    Args:
        app: FastAPI 애플리케이션 인스턴스

    Yields:
        None
    """
    # 시작
    logger.info("챗봇 서비스를 시작합니다...")
    logger.info(f"LLM 제공자: {settings.llm_provider.value}")
    logger.info(f"LLM 모델: {settings.llm_model}")
    logger.info(f"백엔드 URL: {settings.backend_api_url}")

    # Orchestrator 초기화
    orchestrator = ReservationOrchestrator()
    app.state.orchestrator = orchestrator
    logger.info("ReservationOrchestrator 초기화 완료")

    yield

    # 종료
    logger.info("챗봇 서비스를 종료합니다...")


def create_app() -> FastAPI:
    """
    FastAPI 애플리케이션 생성 및 설정.

    애플리케이션 팩토리 패턴을 사용하여
    FastAPI 인스턴스를 생성하고 모든 설정을 적용합니다.

    Returns:
        설정된 FastAPI 애플리케이션 인스턴스
    """
    # FastAPI 앱 생성
    app = FastAPI(
        title="회의실 예약 LLM 서비스",
        description="회의실 예약 시스템을 위한 멀티 에이전트 AI 챗봇",
        version="0.1.0",
        lifespan=lifespan,
    )

    # CORS 미들웨어 추가
    app.add_middleware(
        CORSMiddleware,
        allow_origins=["*"],  # 프로덕션에서는 특정 도메인으로 제한
        allow_credentials=True,
        allow_methods=["*"],
        allow_headers=["*"],
    )

    # 정적 파일 마운트
    static_dir = BASE_DIR / "webapps/static"
    if static_dir.exists():
        app.mount("/static", StaticFiles(directory=str(static_dir)), name="static")
    else:
        logger.warning(f"정적 파일 디렉토리를 찾을 수 없습니다: {static_dir}")

    # 라우터 등록
    # UI 라우터는 prefix 없이 등록 (루트 경로 사용)
    app.include_router(ui_router)

    # API 라우터들 등록
    app.include_router(health_router)
    app.include_router(chat_router)
    app.include_router(session_router)

    logger.info("FastAPI 애플리케이션 초기화 완료")

    return app


# 애플리케이션 인스턴스 생성
app = create_app()