import logging
from typing import Any, Callable

from langchain_core.embeddings import Embeddings
from langchain_core.language_models.chat_models import BaseChatModel

from apps.config.settings import LLMProvider, settings


########################### Create Model ######################################

def _create_ollama_llm(**kwargs: Any) -> BaseChatModel:
    """OllamaChat 모델 인스턴스 생성."""
    from langchain_ollama import ChatOllama

    # Ollama는 streaming=False를 disable_streaming=True로 처리해야 함
    streaming = kwargs.pop("streaming", settings.llm_streaming)

    # max_tokens가 kwargs에 없으면 settings에서 가져오기 (Ollama는 num_predict로 매핑)
    if "num_predict" not in kwargs and "max_tokens" not in kwargs:
        kwargs["num_predict"] = settings.llm_max_tokens

    return ChatOllama(
        base_url=settings.ollama_base_url,
        disable_streaming=not streaming,
        **kwargs,
    )

def _create_upstage_llm(**kwargs: Any) -> BaseChatModel:
    """Upstage Chat 모델 인스턴스를 생성"""
    if not settings.upstage_api_key:
        raise ValueError("UPSTAGE_API_KEY가 필요합니다.")
    from langchain_upstage import ChatUpstage
    logging.info(f"_create_upstage_llm keyword={kwargs}")

    # max_tokens가 kwargs에 없으면 settings에서 가져오기
    if "max_tokens" not in kwargs:
        kwargs["max_tokens"] = settings.llm_max_tokens

    return ChatUpstage(api_key=settings.upstage_api_key, **kwargs)


def _create_openai_llm(**kwargs: Any) -> BaseChatModel:
    """OpenAI Chat 모델 인스턴스를 생성."""
    if not settings.openai_api_key:
        raise ValueError("OPENAI_API_KEY가 필요합니다.")
    from langchain_openai import ChatOpenAI

    # max_tokens가 kwargs에 없으면 settings에서 가져오기
    if "max_tokens" not in kwargs:
        kwargs["max_tokens"] = settings.llm_max_tokens

    return ChatOpenAI(api_key=settings.openai_api_key, **kwargs)

########################### Create Embeddings ######################################

def _create_ollama_embeddings(**kwargs: Any) -> Embeddings:
    """Ollama 임베딩 모델 인스턴스를 생성합니다."""
    from langchain_ollama import OllamaEmbeddings

    kwargs.setdefault("model", settings.embedding_model)
    return OllamaEmbeddings(base_url=settings.ollama_base_url, **kwargs)


def _create_upstage_embeddings(**kwargs: Any) -> Embeddings:
    """Upstage 임베딩 모델 인스턴스를 생성합니다."""
    if not settings.upstage_api_key:
        raise ValueError("UPSTAGE_API_KEY가 필요합니다.")
    from langchain_upstage import UpstageEmbeddings

    kwargs.setdefault("model", settings.embedding_model)
    return UpstageEmbeddings(api_key=settings.upstage_api_key, **kwargs)


def _create_openai_embeddings(**kwargs: Any) -> Embeddings:
    """OpenAI 임베딩 모델 인스턴스를 생성합니다."""
    if not settings.openai_api_key:
        raise ValueError("OPENAI_API_KEY가 필요합니다.")
    from langchain_openai import OpenAIEmbeddings

    kwargs.setdefault("model", settings.embedding_model)
    return OpenAIEmbeddings(api_key=settings.openai_api_key, **kwargs)

########################### Create Embeddings ######################################

# 프로바이더별 모델 매핑(K,V)
_LLM_CREATORS: dict[LLMProvider, Callable[..., BaseChatModel]] = {
    LLMProvider.OLLAMA: _create_ollama_llm,
    LLMProvider.UPSTAGE: _create_upstage_llm,
    LLMProvider.OPENAI: _create_openai_llm,
}

# 프로바이더별 임베딩 모델 매핑(K,V)
_EMBEDDING_CREATORS: dict[LLMProvider, Callable[..., Embeddings]] = {
    LLMProvider.OLLAMA: _create_ollama_embeddings,
    LLMProvider.UPSTAGE: _create_upstage_embeddings,
    LLMProvider.OPENAI: _create_openai_embeddings,
}


class LLMFactory:
    """LLM 및 임베딩 모델 인스턴스를 생성하는 팩토리 클래스"""

    @staticmethod
    def create_llm(**kwargs: Any) -> BaseChatModel:
        """설정된 Provider를 기반으로 LLM 인스턴스를 생성"""
        provider = settings.llm_provider
        creator = _LLM_CREATORS.get(provider)
        if not creator:
            raise ValueError(f"지원하지 않는 LLM Provider 입니다: {provider}")

        llm_args = {
            "model": settings.llm_model,
            "temperature": settings.llm_temperature,
            "streaming": settings.llm_streaming,
        }

        llm_args.update(kwargs)

        return creator(**llm_args)


    @staticmethod
    def create_embedding_model(**kwargs: Any) -> Embeddings:
        """설정된 프로바이더를 기반으로 임베딩 모델 인스턴스를 생성."""
        provider = settings.llm_provider
        creator = _EMBEDDING_CREATORS.get(provider)
        if not creator:
            raise ValueError(f"지원하지 않는 임베딩 프로바이더입니다: {provider}")

        return creator(**kwargs)
