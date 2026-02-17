from dataclasses import dataclass
from typing import Callable, Dict


@dataclass
class GraphNode:
    """노드 구성을 위한 데이터 클래스."""
    name: str
    action: Callable


class GraphEdge:
    """엣지 구성을 위한 기본 클래스."""
    pass


@dataclass
class EdgeNode(GraphEdge):
    """일반 엣지 구성을 위한 데이터 클래스."""
    source: str
    target: str


@dataclass
class ConditionalEdgeNode(GraphEdge):
    """조건부 엣지 구성을 위한 데이터 클래스."""
    source: str
    condition: Callable
    mapping: Dict[str, str]

