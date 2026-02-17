from typing import Dict, Any, List

from langchain_core.messages import BaseMessage, HumanMessage, AIMessage

from apps.workflow import AgentState


def format_response(
        result: Dict[str, Any], session_id: str
) -> Dict[str, Any]:
    """사용자 응답"""
    return {
        "response": result.get("final_response", ""),
        "intent": result.get("intent", "general"),
        "entities": result.get("entities", {}),
        "success": result.get("error") is None,
        "session_id": session_id,
        "conversation_stage": result.get("conversation_stage", "initial"),
        "collected_info": result.get("collected_info", {}),
    }


def format_error_response(error: str, session_id: str, response: str) -> Dict[str, Any]:
    """오류 발생 시 사용자 응답"""
    return {
        "response": response,
        "success": False,
        "error": error,
        "session_id": session_id,
    }


def create_initial_state(
    user_input: str,
    chat_history: List[Dict[str, Any]] = None,
    conversation_state: Dict[str, Any] = None,
    user_email: str = None,
) -> AgentState:
    """초기 상태를 생성.

    Args:
        user_input: 현재 사용자 입력
        chat_history: 이전 대화 기록 (memory에서 로드)
        conversation_state: 이전 대화 상태 (conversation_stage, collected_info 등)
        user_email: 사용자 이메일 (예약자 이메일)

    Returns:
        AgentState - 이전 대화 컨텍스트가 포함된 초기 상태
    """
    # 대화 기록을 BaseMessage로 변환 (최근 10개만)
    messages: List[BaseMessage] = []
    if chat_history:
        # 최근 10개 메시지만 사용 (너무 많으면 컨텍스트가 커짐)
        recent_history = chat_history[-10:] if len(chat_history) > 10 else chat_history
        for msg in recent_history:
            role = msg.get("role", "user")
            content = msg.get("content", "")
            if role == "user":
                messages.append(HumanMessage(content=content))
            elif role == "assistant":
                messages.append(AIMessage(content=content))

    # 이전 상태 복원 (없으면 빈 딕셔너리)
    conversation_state = conversation_state or {}

    # 대화 단계 복원 (기본값: "initial")
    stage = conversation_state.get("conversation_stage", "initial")

    # 수집된 정보 복원 (대화 진행 중이면 이전 정보 유지)
    collected_info = conversation_state.get("collected_info", {})

    # 다음 액션 복원
    next_action = conversation_state.get("next_action", "")

    return {
        "messages": messages,
        "current_agent": "",
        "intent": conversation_state.get("intent", ""),
        "entities": conversation_state.get("entities", {}),
        "tool_results": [],
        "final_response": "",
        "error": None,
        "user_input": user_input,
        "user_email": user_email or conversation_state.get("user_email"),  # 세션에서 복원 또는 새로 설정
        # 대화 상태 복원
        "conversation_stage": stage,
        "collected_info": collected_info,
        "next_action": next_action,
    }
