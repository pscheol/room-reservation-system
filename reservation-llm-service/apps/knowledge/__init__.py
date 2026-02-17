"""지식 베이스 모듈.

시스템 정보, FAQ, 문서 등을 관리하는 지식 베이스를 제공합니다.
향후 RAG(Retrieval-Augmented Generation) 확장 가능.
"""

from apps.core.base_knowledge import BaseKnowledge
from apps.knowledge.static_knowledge import StaticKnowledge

__all__ = ["BaseKnowledge", "StaticKnowledge"]