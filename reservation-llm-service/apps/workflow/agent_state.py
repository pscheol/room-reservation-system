

import operator
from typing import Annotated, Any, Sequence

from langchain_core.messages import BaseMessage
from typing_extensions import TypedDict


class AgentState(TypedDict):
    """LangGraph State 정의"""

    # 메시지 히스토리 (누적) - 대화 컨텍스트
    messages: Annotated[Sequence[BaseMessage], operator.add]

    # 사용자 입력
    user_input: str

    # 사용자 이메일 (예약자 이메일로 사용)
    user_email: str | None

    # 현재 라우팅된 Agent 타입
    current_agent: str

    # 사용자 의도 (Router가 분류)
    intent: str

    # 추출된 엔티티 (날짜, 시간, 이메일, roomId 등)
    entities: dict[str, Any]

    # 도구 실행 결과
    tool_results: list[dict[str, Any]]

    # 최종 응답
    final_response: str

    # 오류 발생 시
    error: str | None

    # 대화 단계: "initial" | "collecting" | "confirming" | "completed"
    # 다음 사용자 입력 시 이전 상태 복원에 사용
    conversation_stage: str

    # 수집된 정보 (예약 시 필요한 데이터 등)
    # 예: {"roomId": 1, "date": "2024-01-20"}
    collected_info: dict[str, Any]

    # 다음 액션 (다음에 수행할 작업)
    next_action: str