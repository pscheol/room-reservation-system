"""Application configuration settings."""

from enum import Enum
from typing import Optional

from pydantic import Field
from pydantic_settings import BaseSettings, SettingsConfigDict


class LLMProvider(str, Enum):
    """Supported LLM providers."""

    OLLAMA = "ollama"
    UPSTAGE = "upstage"
    OPENAI = "openai"


class Settings(BaseSettings):
    """Application settings loaded from environment variables."""

    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    # LLM Configuration
    llm_provider: LLMProvider = Field(default=LLMProvider.OLLAMA, alias="LLM_PROVIDER")
    llm_model: str = Field(default="llama3.1:8b", alias="LLM_MODEL")
    llm_temperature: float = Field(default=0.7, alias="LLM_TEMPERATURE")
    llm_streaming: bool = Field(default=True, alias="LLM_STREAMING")
    llm_max_tokens: int = Field(default=4000, alias="LLM_MAX_TOKENS")

    embedding_model: str = Field(default="text-embedding-ada-002", alias="EMBEDDING_MODEL")
    embedding_chunk_size: int = Field(default=1000, alias="EMBEDDING_CHUNK_SIZE")

    ollama_base_url: str = Field(default="http://localhost:11434", alias="OLLAMA_BASE_URL")
    upstage_api_key: Optional[str] = Field(default=None, alias="UPSTAGE_API_KEY")
    openai_api_key: Optional[str] = Field(default=None, alias="OPENAI_API_KEY")

    # Backend Service
    backend_api_url: str = Field(default="http://localhost:8080", alias="BACKEND_API_URL")
    backend_timeout: int = Field(default=30, alias="BACKEND_TIMEOUT")

    # Redis
    redis_host: str = Field(default="localhost", alias="REDIS_HOST")
    redis_port: int = Field(default=6379, alias="REDIS_PORT")
    redis_db: int = Field(default=0, alias="REDIS_DB")
    redis_password: Optional[str] = Field(default=None, alias="REDIS_PASSWORD")

    # API Server
    api_host: str = Field(default="0.0.0.0", alias="API_HOST")
    api_port: int = Field(default=8000, alias="API_PORT")
    api_reload: bool = Field(default=True, alias="API_RELOAD")

    # Logging
    log_level: str = Field(default="INFO", alias="LOG_LEVEL")

    @property
    def redis_url(self) -> str:
        """Redis connection URL."""
        if self.redis_password:
            return f"redis://:{self.redis_password}@{self.redis_host}:{self.redis_port}/{self.redis_db}"
        return f"redis://{self.redis_host}:{self.redis_port}/{self.redis_db}"


# Global settings instance
settings = Settings()