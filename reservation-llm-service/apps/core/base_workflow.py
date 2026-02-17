from abc import ABC, abstractmethod
from typing import Any, List

from langgraph.graph import StateGraph
from langgraph.graph.state import CompiledStateGraph

from apps.core.workflow_dto import (
    ConditionalEdgeNode,
    EdgeNode,
    GraphNode,
    GraphEdge,
)
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class BaseWorkflowFactory(ABC):
    """
    설정 객체를 사용하여 워크플로우를 생성하는 템플릿 기반 팩토리.
    """

    def __init__(self, state_class: StateGraph):
        self.workflow_factory = state_class

    @abstractmethod
    def _get_nodes(self) -> List[GraphNode]:
        """노드 설정 객체의 리스트를 반환합니다."""
        pass

    @abstractmethod
    def _get_edges(self) -> List[GraphEdge]:
        """엣지 설정 객체의 리스트를 반환합니다."""
        pass

    def create_workflow(self) -> CompiledStateGraph[Any, Any, Any, Any]:
        """설정 객체를 바탕으로 워크플로우를 생성하고 컴파일합니다."""
        logger.info("Creating workflow...")
        self.__add_nodes()
        self.__add_edges()

        compiled_workflow = self.workflow_factory.compile()
        logger.info("Workflow created and compiled successfully.")
        return compiled_workflow

    @staticmethod
    def draw_diagram(compiled_workflow: CompiledStateGraph, output_path: str):
        """컴파일된 워크플로우의 다이어그램을 그립니다."""
        try:
            compiled_workflow.get_graph().draw_mermaid_png(output_file_path=output_path)
            logger.info(f"Workflow diagram saved to {output_path}")
        except Exception as e:
            logger.warning(f"Failed to draw workflow diagram: {e}")


    def __add_nodes(self):
        """설정에 따라 워크플로우에 노드를 추가합니다."""
        [self.__add_node(node) for node in self._get_nodes()]

    def __add_edges(self):
        """Edges 추가"""
        for edge in self._get_edges():
            if isinstance(edge, EdgeNode):
                self.__add_edge(edge)
            elif isinstance(edge, ConditionalEdgeNode):
                self._add_conditional_edge(edge)

    def __add_node(self, node_config: GraphNode):
        self.workflow_factory.add_node(node_config.name, node_config.action)
        logger.debug(f"Node '{node_config.name}' added.")

    def __add_edge(self, graph_edge: EdgeNode):
        """기본 Edge 추가"""
        self.workflow_factory.add_edge(graph_edge.source, graph_edge.target)
        logger.debug(f"Edge from '{graph_edge.source}' to '{graph_edge.target}' added.")

    def _add_conditional_edge(self, edge_config: ConditionalEdgeNode):
        """Conditional edge 추가"""
        self.workflow_factory.add_conditional_edges(
            edge_config.source, edge_config.condition, edge_config.mapping
        )
        logger.debug(f"Conditional edge from '{edge_config.source}' added.")