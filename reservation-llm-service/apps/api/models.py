"""챗봇 서비스를 위한 API 모델입니다."""

from typing import List, Optional

from pydantic import BaseModel, Field


class ChatMessage(BaseModel):
    """채팅 메시지 모델입니다."""

    role: str = Field(..., description="메시지 역할 (user 또는 assistant)")
    content: str = Field(..., description="메시지 내용")


class ChatRequest(BaseModel):
    """채팅 요청 모델입니다."""

    message: str = Field(..., description="사용자 메시지", min_length=1)
    chat_history: List[ChatMessage] = Field(
        default_factory=list, description="이전 채팅 메시지"
    )
    session_id: str = Field(default="default", description="세션 ID")
    user_email: Optional[str] = Field(None, description="사용자 이메일 (예약자 이메일로 사용)")
    stream: bool = Field(default=False, description="스트리밍 응답 활성화")


class ChatResponse(BaseModel):
    """채팅 응답 모델입니다."""

    response: str = Field(..., description="어시스턴트의 응답")
    agent: Optional[str] = Field(None, description="요청을 처리한 에이전트")


class HealthResponse(BaseModel):
    """상태 확인 응답 모델입니다."""

    status: str
    llm_provider: str
    llm_model: str
    backend_url: str
    # server_id: Optional[str] = Field(None, description="서버 시작 시간 (재시작 감지용)")
