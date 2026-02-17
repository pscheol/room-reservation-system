"""
웹 서버 실행 모듈.

이 모듈은 uvicorn을 사용하여 FastAPI 애플리케이션을 실행합니다.
애플리케이션 로직은 app.py에 정의되어 있습니다.
"""

from apps.config.settings import settings


def run_server() -> None:
    """
    FastAPI 애플리케이션을 uvicorn으로 실행.

    settings에 정의된 호스트, 포트, 리로드 설정을 사용합니다.
    """
    import uvicorn

    uvicorn.run(
        "apps.api.app:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=settings.api_reload,
    )


if __name__ == "__main__":
    run_server()