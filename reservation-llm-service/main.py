import uvicorn

from apps.config.settings import settings
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


def main():
    """Run the FastAPI server."""
    logger.info("=" * 80)
    logger.info("Starting Meeting Room Reservation LLM Service")
    logger.info("=" * 80)
    logger.info(f"LLM Provider: {settings.llm_provider.value}")
    logger.info(f"LLM Model: {settings.llm_model}")
    logger.info(f"Backend URL: {settings.backend_api_url}")
    logger.info(f"API Server: http://{settings.api_host}:{settings.api_port}")
    logger.info(f"API Docs: http://{settings.api_host}:{settings.api_port}/docs")
    logger.info("=" * 80)

    uvicorn.run(
        "apps.api.app:app",
        host=settings.api_host,
        port=settings.api_port,
        reload=settings.api_reload,
        log_level=settings.log_level.lower(),
    )


if __name__ == "__main__":
    main()
