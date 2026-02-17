from typing import Any, Dict, Optional

from langgraph.graph.state import CompiledStateGraph

from apps.agents import (
    GeneralAgent,
    QueryAgent,
    ReservationAgent,
    ResultAgent,
    RoomAgent,
    RouterAgent,
)
from apps.core import BaseWorkflowFactory
from apps.core.base_agent import BaseAgent
from apps.core.base_knowledge import BaseKnowledge
from apps.core.base_memory import BaseMemory
from apps.core.base_orchestrator import BaseOrchestrator
from apps.knowledge.static_knowledge import StaticKnowledge
from apps.memory.in_memory import InMemoryStore
from apps.orchestrations.reservation_dto import create_initial_state, format_response, format_error_response
from apps.utils.logger import setup_logger
from apps.workflow.reservation_workflow import ReservationWorkflowFactory
from apps.workflow.reservation_nodes import set_agent_cache

logger = setup_logger(__name__)



class ReservationOrchestrator(BaseOrchestrator):
    """예약 오케스트레이터 구현체.

    핵심 구성요소:
        - 모델 (Model): LLM 인스턴스 관리
        - 도구 (Tools): LangChain 도구
        - 메모리 (Memory): 대화 기록 관리
        - 지식베이스 (Knowledge): 시스템 정보 및 FAQ
        - 워크플로우 (Workflow): LangGraph 기반 상태
    """

    def __init__(
        self,
        agents: Dict[type, BaseAgent] = None,
        memory: Optional[BaseMemory] = None,
        knowledge: Optional[BaseKnowledge] = None,
        workflow_factory: Optional[BaseWorkflowFactory] = None,
        workflow: Optional[CompiledStateGraph[Any, Any, Any, Any]] = None,
    ):
        """
        Args:
            llm: LLM 모델. 없으면 기본 생성.
            memory: 메모리. 없으면 InMemoryStore 사용.
            knowledge: 지식베이스(RAG). 없으면 StaticKnowledge 사용.
        """

        logger.info("Start initializing ReservationOrchestrator..")

        self.memory = memory or InMemoryStore()
        logger.info(f"Initialized Memory: {type(self.memory).__name__}")

        self.knowledge = knowledge or StaticKnowledge()
        logger.info(f"Initialized Knowledge: {type(self.knowledge).__name__}")

        # Agent 인스턴스 생성
        logger.info("Initializing Agent instances...")
        self.agents: Dict[type, BaseAgent] = agents or self.__load_agents()
        logger.info(f"Initialized {len(self.agents)} agents")

        # Agent 캐시를 워크플로우 노드에 설정
        set_agent_cache(self.agents)
        logger.info("Agent 캐시 설정 완료")

        # workflow_factory 생성
        self.workflowFactory = workflow_factory or ReservationWorkflowFactory()
        self.workflow = workflow or self.workflowFactory.create_workflow()
        logger.info("Initialized LangGraph workflow.")

        # 다이어그램 생성
        self.workflowFactory.draw_diagram(self.workflow,"reservation_workflow.png")

        logger.info("Complete ReservationOrchestrator initialized.")

    @staticmethod
    def __load_agents() -> dict[type[
        RouterAgent | RoomAgent | ReservationAgent | QueryAgent | GeneralAgent | ResultAgent], RouterAgent | RoomAgent | ReservationAgent | QueryAgent | GeneralAgent | ResultAgent]:
        return {
            RouterAgent: RouterAgent(),
            RoomAgent: RoomAgent(),
            ReservationAgent: ReservationAgent(),
            QueryAgent: QueryAgent(),
            GeneralAgent: GeneralAgent(),
            ResultAgent: ResultAgent(),  # Result는 LLM 필요
        }

    async def process_message(
        self,
        message: str,
        session_id: str = "default",
        user_email: str = None,
    ) -> Dict[str, Any]:
        """사용자 메시지 처리
            워크플로우를 실행 후 응답 생성

            Args:
            message: 사용자 메시지
            session_id: 세션 ID
            user_email: 사용자 이메일 (예약자 이메일)

            Returns:
                처리 결과 딕셔너리
        """
        try:
            logger.info(f"[{session_id}] 메시지 처리 시작")
            logger.debug(f"[{session_id}] 사용자 입력: {message}")

            # 1. 메모리에서 대화 기록 조회
            chat_history = await self._load_chat_history(session_id)
            logger.info(
                f"[{session_id}] 대화 기록 로드 완료 "
                f"(총 {len(chat_history)}개 메시지)"
            )
            logger.debug(f"[{session_id}] chat_history: {chat_history}")

            # 2. 메모리에서 대화 상태 복원
            conversation_state = await self.memory.get_conversation_state(session_id)
            stage = conversation_state.get("conversation_stage", "initial")
            collected_info = conversation_state.get("collected_info", {})
            intent = conversation_state.get("intent", "")

            # user_email 업데이트 (새로 제공되면 업데이트, 없으면 세션에서 복원)
            if user_email:
                conversation_state["user_email"] = user_email
            else:
                user_email = conversation_state.get("user_email")

            logger.info(
                f"[{session_id}] 대화 상태 복원: "
                f"stage={stage}, intent={intent}, user_email={user_email}, "
                f"collected_info={collected_info}"
            )

            # 3. 초기 State 구성 (대화 기록 + 상태 + 이메일 포함)
            initial_state = create_initial_state(
                message,
                chat_history=chat_history,
                conversation_state=conversation_state,
                user_email=user_email
            )
            logger.debug(f"[{session_id}] 초기 State 생성 완료 (user_email: {user_email})")

            # 4. LangGraph 워크플로우 실행
            logger.info(f"[{session_id}] 워크플로우 실행 시작")
            result = await self.workflow.ainvoke(initial_state)
            logger.info(
                f"[{session_id}] 워크플로우 실행 완료 "
                f"(intent: {result.get('intent', 'unknown')}, "
                f"stage: {result.get('conversation_stage', 'unknown')})"
            )

            # 5. 메모리에 대화 및 상태 저장
            await self._save_conversation(session_id, message, result)
            logger.info(f"[{session_id}] 대화 저장 완료")

            # 6. 최종 응답 구성
            response = format_response(result, session_id)

            logger.info(
                f"[{session_id}] 메시지 처리 완료 "
                f"(success: {response.get('success', False)})"
            )
            return response

        except Exception as e:
            logger.error(
                f"[{session_id}] 메시지 처리 중 오류 발생: {e}",
                exc_info=True
            )
            return format_error_response(
                str(e),
                session_id,
                "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다."
            )

    async def _load_chat_history(
        self, session_id: str, limit: int = 10
    ) -> list[Dict[str, Any]]:
        """메모리에서 대화 기록을 로드합니다.

        Args:
            session_id: 세션 ID
            limit: 조회할 최대 메시지 수

        Returns:
            대화 기록 목록
        """
        messages = await self.memory.get_messages(session_id, limit)
        return messages


    async def _save_conversation(
        self, session_id: str, user_message: str, result: Dict[str, Any]
    ):
        """대화 내용 및 상태를 메모리에 저장

        Args:
            session_id: 세션 ID
            user_message: 사용자 메시지
            result: 워크플로우 실행 결과
        """
        # 대화 메시지 저장
        await self.memory.add_message(
            session_id=session_id, role="user", content=user_message
        )
        logger.debug(f"[{session_id}] 사용자 메시지 저장 완료")

        assistant_response = result.get("final_response", "")
        await self.memory.add_message(
            session_id=session_id,
            role="assistant",
            content=assistant_response,
            metadata={
                "success": result.get("error") is None,
                "intent": result.get("intent"),
                "conversation_stage": result.get("conversation_stage", "initial"),
            },
        )
        logger.debug(f"[{session_id}] 시스템 응답 저장 완료")

        # 대화 상태 저장
        conversation_state = {
            "conversation_stage": result.get("conversation_stage", "initial"),
            "collected_info": result.get("collected_info", {}),
            "intent": result.get("intent", ""),
            "entities": result.get("entities", {}),
            "next_action": result.get("next_action", ""),
            "user_email": result.get("user_email"),  # 이메일도 세션에 저장
        }
        await self.memory.save_conversation_state(session_id, conversation_state)

        logger.debug(
            f"[{session_id}] 대화 상태 저장 완료: "
            f"stage={conversation_state['conversation_stage']}, "
            f"intent={conversation_state['intent']}, "
            f"next_action={conversation_state['next_action']}"
        )

    async def get_session_info(self, session_id: str) -> Dict[str, Any]:
        """세션 정보 조회"""
        return await self.memory.get_session_summary(session_id)

    async def clear_session(self, session_id: str) -> None:
        """세션 초기화"""
        await self.memory.clear_session(session_id)
        logger.info(f"Session cleared: {session_id}")

    async def get_system_info(self) -> Dict[str, Any]:
        """시스템 정보 조회"""
        return await self.knowledge.get_system_info()

    async def process_message_stream(
        self,
        message: str,
        session_id: str = "default",
        user_email: str = None,
    ):
        """사용자 메시지를 스트리밍 방식으로 처리

        Args:
            message: 사용자 메시지
            session_id: 세션 ID
            user_email: 사용자 이메일

        Yields:
            각 노드의 실행 결과 (딕셔너리)
        """
        try:
            logger.info(f"[{session_id}] 스트리밍 메시지 처리 시작")

            # 1. 메모리에서 대화 기록 및 상태 조회
            chat_history = await self._load_chat_history(session_id)
            conversation_state = await self.memory.get_conversation_state(session_id)

            # user_email 업데이트
            if user_email:
                conversation_state["user_email"] = user_email
            else:
                user_email = conversation_state.get("user_email")

            # 2. 초기 State 구성
            initial_state = create_initial_state(
                message,
                chat_history=chat_history,
                conversation_state=conversation_state,
                user_email=user_email
            )

            # 3. LangGraph 워크플로우 스트리밍 실행
            logger.info(f"[{session_id}] 워크플로우 스트리밍 시작")

            # 누적된 상태 (모든 업데이트를 병합)
            accumulated_state = initial_state.copy()

            async for chunk in self.workflow.astream(initial_state):
                # 각 노드의 실행 결과를 yield
                logger.debug(f"[{session_id}] 스트리밍 청크: {chunk}")

                # chunk는 {node_name: state_update} 형태
                for node_name, state_update in chunk.items():
                    # 상태 누적 (모든 필드를 병합)
                    accumulated_state.update(state_update)

                    # final_response가 있으면 전송
                    if "final_response" in state_update and state_update["final_response"]:
                        yield {
                            "type": "response",
                            "node": node_name,
                            "content": state_update["final_response"],
                            "intent": accumulated_state.get("intent", ""),
                        }

            # 4. 메모리에 대화 및 상태 저장 (누적된 전체 상태 사용)
            await self._save_conversation(session_id, message, accumulated_state)
            logger.info(f"[{session_id}] 스트리밍 대화 저장 완료 (user_email: {accumulated_state.get('user_email')})")

            # 5. 완료 신호
            yield {
                "type": "done",
                "session_id": session_id,
            }

        except Exception as e:
            logger.error(f"[{session_id}] 스트리밍 처리 중 오류: {e}", exc_info=True)
            yield {
                "type": "error",
                "error": str(e),
                "message": "죄송합니다. 요청을 처리하는 중 오류가 발생했습니다."
            }


