
from typing import List

from langgraph.graph import START, END
from langgraph.graph import StateGraph

from apps.core.base_workflow import BaseWorkflowFactory
from apps.core.workflow_dto import (
    ConditionalEdgeNode,
    GraphNode,
    EdgeNode,
    GraphEdge
)
from apps.workflow import AgentState
from apps.workflow.reservation_nodes import (
    general_agent_node,
    query_agent_node,
    reservation_agent_node,
    result_node,
    room_agent_node,
    route_to_agent,
    route_from_reservation,
    route_from_room,
    router_node,
)


class ReservationWorkflowFactory(BaseWorkflowFactory):
    """LangGraph 에이전트 워크플로우 구성."""
    def __init__(self):
        super().__init__(StateGraph(AgentState))

    def _get_nodes(self) -> List[GraphNode]:
        """에이전트 워크플로우 노드 설정"""
        return [
            GraphNode(name="router", action=router_node),
            GraphNode(name="room_agent", action=room_agent_node),
            GraphNode(name="reservation_agent", action=reservation_agent_node),
            GraphNode(name="query_agent", action=query_agent_node),
            GraphNode(name="general_agent", action=general_agent_node),
            GraphNode(name="result", action=result_node),
        ]

    def _get_edges(self) -> List[GraphEdge]:
        """에이전트 워크플로우의 엣지 설정"""
        return [
            # START → router
            EdgeNode(source=START, target="router"),

            # router → [agent 선택(room, reservation, query, general]
            ConditionalEdgeNode(
                source="router",
                condition=route_to_agent,
                mapping={
                    "room_agent": "room_agent",
                    "reservation_agent": "reservation_agent",
                    "query_agent": "query_agent",
                    "general_agent": "general_agent",
                },
            ),

            # reservation_agent → room_agent
            ConditionalEdgeNode(
                source="reservation_agent",
                condition=route_from_reservation,
                mapping={
                    "room_agent": "room_agent",     # room_id 없음 → 회의실 검색
                    "result": "result",             # room_id 있음 → 결과 생성
                },
            ),

            # room_agent → reservation_agent
            ConditionalEdgeNode(
                source="room_agent",
                condition=route_from_room,
                mapping={
                    "reservation_agent": "reservation_agent",  # 예약 프로세스 복귀
                    "result": "result",                        # 단순 검색 완료
                },
            ),


            EdgeNode(source="query_agent", target="result"),
            EdgeNode(source="general_agent", target="result"),

            # Result → END
            EdgeNode(source="result", target=END),
        ]