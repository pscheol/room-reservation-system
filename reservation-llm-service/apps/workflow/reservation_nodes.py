from langchain_core.messages import AIMessage, HumanMessage
from typing import Any,Dict,Type
from apps.agents import (
    GeneralAgent,
    QueryAgent,
    ReservationAgent,
    ResultAgent,
    RoomAgent,
    RouterAgent,
)
from apps.core.base_agent import BaseAgent
from apps.utils.logger import setup_logger
from apps.workflow.agent_state import AgentState

logger = setup_logger(__name__)
_agent_cache: Dict[type, BaseAgent] = {}


def set_agent_cache(agents: Dict[type, BaseAgent]) -> None:
    """Orchestrator에서 생성한 Agent 인스턴스들을 캐시에 저장.

    Args:
        agents: Agent 클래스 -> 인스턴스 딕셔너리
    """
    global _agent_cache
    _agent_cache = agents
    logger.info(f"Agent 캐시 설정 완료: {len(_agent_cache)}개")


def _get_agent_instance(agent_class: type) -> BaseAgent:
    """Agent 싱글톤 인스턴스 호출.

    Args:
        agent_class: Agent 클래스

    Returns:
        Agent 인스턴스
    """
    if agent_class not in _agent_cache:
        logger.info(f"{agent_class.__name__} - 새로 생성")
        _agent_cache[agent_class] = agent_class()

    return _agent_cache[agent_class]


async def _run_agent(
    state: AgentState, agent_class: Type[BaseAgent], with_context: bool = False
) -> Dict[str, Any]:
    """지정된 Agent를 실행하고 State를 업데이트.

    Args:
        state: 현재 AgentState
        agent_class: 실행할 Agent 클래스
        with_context: Agent 실행 시 context를 전달할지 여부

    Returns:
        업데이트된 state 딕셔너리
    """
    logger.info(f"{agent_class.__name__} 노드 실행")
    user_input = state.get("user_input", "")

    # _agent_cache에서 싱글톤 인스턴스 가져오기
    agent = _get_agent_instance(agent_class)

    if with_context:
        # 대화 기록 및 상태를 컨텍스트로 전달
        context = {
            "entities": state.get("entities", {}),
            "messages": state.get("messages", []),
            "conversation_stage": state.get("conversation_stage", "initial"),
            "collected_info": state.get("collected_info", {}),
            "next_action": state.get("next_action", ""),
            "user_email": state.get("user_email"),  # ✅ 사용자 이메일 추가
        }
        logger.debug(
            f"[Context] entities={context['entities']}, "
            f"conversation_stage={context['conversation_stage']}, "
            f"collected_info={context['collected_info']}, "
            f"next_action={context['next_action']}, "
            f"user_email={context['user_email']}, "
            f"messages_count={len(context['messages'])}"
        )
        result = await agent.process(user_input, context)
    else:
        result = await agent.process(user_input)

    # Agent 응답을 메시지로 추가
    response_content = result.get("response", "")
    messages = [
        HumanMessage(content=user_input),
        AIMessage(content=response_content)
    ]

    # State 업데이트 딕셔너리 구성
    response_dict = {
        "messages": messages,
        "final_response": response_content,
    }

    # 도구 실행 결과가 있으면 추가
    if "tool_results" in result:
        response_dict["tool_results"] = result.get("tool_results", [])

    # entities가 있으면 추가
    if "entities" in result:
        response_dict["entities"] = result["entities"]

    if "error" in result:
        response_dict["error"] = result["error"]

    # 대화 상태 업데이트 (Agent가 반환한 경우)
    if "conversation_stage" in result:
        response_dict["conversation_stage"] = result["conversation_stage"]

    if "collected_info" in result:
        response_dict["collected_info"] = result["collected_info"]

    if "next_action" in result:
        response_dict["next_action"] = result["next_action"]

    return response_dict


async def router_node(state: AgentState) -> Dict[str, Any]:
    """Router 노드: 사용자 의도를 분류.

    대화 상태(conversation_stage)를 확인하여:
    - collecting/confirming: 이전 intent 유지 (대화 진행 중)
    - 그 외: 새로 분류
    """
    logger.info("Router 노드 실행")

    user_input = state.get("user_input", "")
    messages = state.get("messages", [])
    conversation_stage = state.get("conversation_stage", "initial")
    previous_intent = state.get("intent", "")
    collected_info = state.get("collected_info", {})

    # 대화 진행 중이면 이전 intent 유지
    if conversation_stage in ["collecting", "confirming"] and previous_intent:
        logger.info(
            f"대화 진행 중 (stage={conversation_stage}) - "
            f"이전 intent 유지: {previous_intent}"
        )

        # collected_info에 있는 정보를 entities에 병합 (room_id 등 유지)
        current_entities = state.get("entities", {})
        if collected_info:
            # collected_info의 room_id, roomId를 entities에 추가
            for key in ["room_id", "roomId", "date", "reservationDate", "startTime", "endTime"]:
                if key in collected_info and key not in current_entities:
                    current_entities[key] = collected_info[key]
            logger.info(f"collected_info에서 entities 병합: {current_entities}")

        # 사용자 입력을 메시지에 추가
        new_messages = [HumanMessage(content=user_input)]

        return {
            "messages": new_messages,
            "intent": previous_intent,
            "entities": current_entities,  # ✅ entities 유지!
            "current_agent": previous_intent,
            # collected_info, conversation_stage는 자동 유지
        }

    # 새로운 대화 시작: RouterAgent로 의도 분류
    logger.info("새 대화 또는 completed 상태 - 의도 분류 시작")
    router = _get_agent_instance(RouterAgent)

    if messages:
        # 대화 기록이 있으면 컨텍스트 전달 (이전 대화에서 정보 추출)
        context = {"messages": messages}
        logger.debug(f"이전 대화 기록 참조 (messages_count: {len(messages)})")
        result = await router.process(user_input, context)
    else:
        # 첫 대화
        logger.info("첫 대화 시작")
        result = await router.process(user_input)

    # 사용자 입력을 메시지에 추가
    new_messages = [HumanMessage(content=user_input)]

    # ✅ collected_info에서 유용한 정보만 entities에 병합
    previous_collected_info = state.get("collected_info", {})
    new_entities = result.get("entities", {})

    # date, time 정보가 새로 없으면 이전 것 유지 (키 이름 변형 포함)
    time_keys = [
        ("date", "date"),
        ("startTime", "start_time"),
        ("endTime", "end_time"),
    ]

    for new_key, old_key in time_keys:
        if new_key not in new_entities and old_key in previous_collected_info:
            new_entities[new_key] = previous_collected_info[old_key]
            logger.info(f"collected_info에서 {old_key} → {new_key} 재사용: {previous_collected_info[old_key]}")


    return {
        "messages": new_messages,
        "intent": result.get("intent", "general"),
        "entities": new_entities,
        "current_agent": result.get("intent", "general"),
        "conversation_stage": "initial",
        "collected_info": {},
        "next_action": "",
    }

async def result_node(state: AgentState) -> Dict[str, Any]:
    """Result 노드: ResultAgent를 통해 도구 실행 결과를 변환.
    이전 대화 기록을 참조하여 자연스러운 응답 생성.
    """
    logger.info("Result 노드 실행")

    tool_results = state.get("tool_results", [])
    user_input = state.get("user_input", "")
    intent = state.get("intent", "general")
    current_response = state.get("final_response", "")
    messages = state.get("messages", [])

    # 도구 실행 결과가 없으면 기존 응답 그대로 반환
    if not tool_results:
        logger.info("도구 실행 결과 없음, 기존 응답 사용")
        return {"final_response": current_response}

    # ResultAgent 사용
    result_agent = _get_agent_instance(ResultAgent)

    context = {
        "tool_results": tool_results,
        "intent": intent,
        "messages": messages,
        "current_response": current_response,
    }

    result = await result_agent.process(user_input, context)

    return {
        "final_response": result.get("response", current_response),
        "tool_results": [],  # tool_results 초기화
    }


def route_to_agent(state: AgentState) -> str:
    """라우팅 결정 함수: intent에 따라 Agent 선택.

    Args:
        state: 현재 AgentState

    Returns:
        다음 노드 이름
    """
    intent = state.get("intent", "general")

    # Intent에 따라 라우팅 (단순)
    route_map = {
        "room": "room_agent",
        "reservation": "reservation_agent",
        "query": "query_agent",
        "general": "general_agent",
    }

    next_node = route_map.get(intent, "general_agent")
    logger.info(f"라우팅 결정: {intent} -> {next_node}")

    return next_node


def route_from_reservation(state: AgentState) -> str:
    """ReservationAgent 후 라우팅 결정: room_id 유무에 따라 분기.

    Args:
        state: 현재 AgentState

    Returns:
        다음 노드 이름
        - "room_agent": room_id 없고 room_name 있음 (회의실 검색 필요)
        - "result": room_id 있음 또는 예약 완료/오류
    """
    entities = state.get("entities", {})
    collected_info = state.get("collected_info", {})
    conversation_stage = state.get("conversation_stage", "initial")

    # room_id 확인 (entities 또는 collected_info에서)
    room_id = entities.get("room_id") or collected_info.get("room_id")

    # room_name 확인
    room_name = entities.get("room_name") or collected_info.get("room_name")

    # 이미 회의실 검색을 시도했는지 확인s
    room_search_attempted = collected_info.get("room_search_attempted", False)

    logger.info(
        f"[route_from_reservation] room_id={room_id}, room_name={room_name}, "
        f"room_search_attempted={room_search_attempted}, stage={conversation_stage}"
    )

    # 조건 1: conversation_stage가 "need_room_search"이면 무조건 room_agent로
    if conversation_stage == "need_room_search":
        logger.info("→ room_agent (stage=need_room_search)")
        return "room_agent"

    # 조건 2: room_id가 없고, room_name이 있고, 아직 검색하지 않았으면 → room_agent
    if not room_id and room_name and not room_search_attempted:
        logger.info("→ room_agent (회의실 검색 필요)")
        return "room_agent"


    logger.info("→ result (예약 진행 또는 응답 생성)")
    return "result"


def route_from_room(state: AgentState) -> str:
    """RoomAgent 후 라우팅 결정

    Args:
        state: 현재 AgentState

    Returns:
        다음 노드 이름
        - "reservation_agent": ReservationAgent에서 온 경우 (예약 프로세스 복귀)
        - "result": Router에서 직접 온 경우 (단순 회의실 검색)
    """
    intent = state.get("intent", "")
    conversation_stage = state.get("conversation_stage", "initial")
    collected_info = state.get("collected_info", {})

    # room_search_attempted 플래그로 판단
    # - True이면 ReservationAgent에서 온 것 (예약 프로세스)
    # - False이면 Router에서 직접 온 것 (단순 검색)
    room_search_attempted = collected_info.get("room_search_attempted", False)

    logger.info(
        f"[route_from_room] intent={intent}, conversation_stage={conversation_stage}, room_search_attempted={room_search_attempted}"
    )

    if room_search_attempted or conversation_stage == "need_room_search":
        # ReservationAgent에서 room_agent로 온 경우 → 예약 프로세스 복귀
        logger.info("→ reservation_agent (예약 프로세스 복귀)")
        return "reservation_agent"
    else:
        # Router에서 직접 온 경우 → 단순 검색 완료
        logger.info("→ result (회의실 검색 완료)")
        return "result"


async def room_agent_node(state: AgentState) -> Dict[str, Any]:
    """Room Agent 노드: 회의실 검색을 처리."""
    return await _run_agent(state, RoomAgent, with_context=True)


async def reservation_agent_node(state: AgentState) -> Dict[str, Any]:
    """Reservation Agent 노드: 예약 생성/수정/취소 처리."""
    return await _run_agent(state, ReservationAgent, with_context=True)


async def query_agent_node(state: AgentState) -> Dict[str, Any]:
    """Query Agent 노드: 예약 조회를 처리."""
    return await _run_agent(state, QueryAgent, with_context=True)


async def general_agent_node(state: AgentState) -> Dict[str, Any]:
    """General Agent 노드: 일반 대화를 처리."""
    return await _run_agent(state, GeneralAgent, with_context=True)

