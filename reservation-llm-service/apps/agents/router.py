
from datetime import datetime
import json
import re
from typing import Any, Dict, List, Optional

from langchain_core.language_models.chat_models import BaseChatModel
from langchain_core.messages import BaseMessage
from langchain_core.prompts import ChatPromptTemplate, MessagesPlaceholder

from apps.core import BaseAgent
from apps.prompts.templates import ROUTER_SYSTEM_PROMPT
from apps.utils.logger import setup_logger

logger = setup_logger(__name__)


class RouterAgent(BaseAgent):
    """사용자 입력을 분석하여 적절한 전문 Agent로 라우팅하는 Agent.

    사용자의 의도를 분석하고 다음 중 하나를 분류:
    - room: 회의실 검색, 가용성 확인
    - reservation: 예약 생성, 수정, 취소
    - query: 예약 조회, 검색
    - general: 일반 대화, 인사, 도움말
    """

    def __init__(self, llm: Optional[BaseChatModel] = None):
        """Router Agent 초기화.

        Args:
            llm: LLM 모델 인스턴스
        """
        super().__init__(llm)
        self.system_prompt = ROUTER_SYSTEM_PROMPT
        self.logger.info("Complete initializing RouterAgent.")

    async def process(
        self, user_input: str, context: Optional[Dict[str, Any]] = None
    ) -> Dict[str, Any]:
        """사용자 의도를 분류하여 엔티티를 추출.

        Args:
            user_input: 사용자 입력
            context: 대화 컨텍스트 (이전 대화 기록 포함)

        Returns:
            intent와 entities를 포함한 딕셔너리
        """
        try:
            self.logger.info(f"RouterAgent 처리 시작: {user_input}")

            # ChatPromptTemplate을 사용하여 메시지 템플릿 구성
            template = ChatPromptTemplate.from_messages([
                ("system", "{system_prompt}"),
                MessagesPlaceholder(variable_name="chat_history", optional=True),
                ("human", "{user_input}")
            ])

            # 이전 대화 기록 준비 (room_id 추출을 위해 최근 5개)
            chat_history: List[BaseMessage] = []
            if context and "messages" in context:
                recent_messages = context["messages"][-5:] if len(context["messages"]) > 5 else context["messages"]
                if recent_messages:
                    self.logger.debug(f"이전 대화 기록 추가: {len(recent_messages)}개")
                    chat_history = recent_messages

            # 템플릿에 변수 대입하여 메시지 생성
            messages = template.format_messages(
                system_prompt=self.system_prompt,
                chat_history=chat_history,
                user_input=user_input,
            )

            logger.debug(f"RouterAgent Messages {messages}")

            # LLM 호출
            response = await self.llm.ainvoke(messages)
            response_text = response.content

            self.logger.debug(f"RouterAgent LLM 응답: {response_text}")

            # JSON 파싱
            result = self._parse_response(response_text)

            # 이전 대화에서 room_id 추출 (room_name이 있지만 room_id가 없을 때)
            entities = result.get("entities", {})
            if not entities.get("room_id") and not entities.get("roomId"):
                room_name = entities.get("room_name")
                if room_name and chat_history:
                    # 이전 대화에서 "(ID: X)" 패턴 찾기
                    extracted_room_id = self._extract_room_id_from_history(
                        room_name, chat_history
                    )
                    if extracted_room_id:
                        entities["room_id"] = extracted_room_id
                        self.logger.info(
                            f"대화 기록에서 room_id 추출: {room_name} → {extracted_room_id}"
                        )

            self.logger.info(f"의도 분류 완료: {result.get('intent')}, 엔티티: {entities}")

            return result

        except Exception as e:
            self.logger.error(f"RouterAgent 처리 중 오류: {e}", exc_info=True)
            # 오류 시 기본값 반환
            return {
                "intent": "general",
                "entities": {},
            }

    def _parse_response(self, response_text: str) -> Dict[str, Any]:
        """LLM 응답에서 JSON을 파싱합니다.

        Args:
            response_text: LLM 응답 텍스트

        Returns:
            intent와 entities를 포함한 딕셔너리
        """
        try:
            # JSON 블록 추출 (```json ... ``` 또는 { ... } 형식)
            json_match = re.search(r'```json\s*(\{.*?\})\s*```', response_text, re.DOTALL)
            if json_match:
                json_str = json_match.group(1)
            else:
                # 중괄호로 감싸진 JSON 찾기
                json_match = re.search(r'\{.*\}', response_text, re.DOTALL)
                if json_match:
                    json_str = json_match.group(0)
                else:
                    raise ValueError("JSON 형식을 찾을 수 없음")

            # JSON 파싱
            parsed = json.loads(json_str)

            # 필수 필드 확인 및 기본값 설정
            intent = parsed.get("intent", "general")
            entities = parsed.get("entities", {})

            # intent 유효성 검증
            valid_intents = ["room", "reservation", "query", "general"]
            if intent not in valid_intents:
                self.logger.warning(f"유효하지 않은 intent: {intent}, general로 대체")
                intent = "general"

            return {
                "intent": intent,
                "entities": entities,
            }

        except (json.JSONDecodeError, ValueError) as e:
            self.logger.warning(f"JSON 파싱 실패: {e}, 응답: {response_text}")
            # 파싱 실패 시 키워드 기반 폴백
            return self._fallback_intent_detection(response_text)

    def _extract_room_id_from_history(
        self, room_name: str, chat_history: List[BaseMessage]
    ) -> Optional[int]:
        """이전 대화 기록에서 room_name에 해당하는 room_id를 추출.

        Args:
            room_name: 회의실 이름 (예: "회의실 B")
            chat_history: 이전 대화 기록

        Returns:
            room_id (숫자) 또는 None
        """
        import re

        # 최근 대화부터 역순으로 검색
        for message in reversed(chat_history):
            content = message.content
            if not content:
                continue

            # 패턴: "회의실 B (ID: 2)" 또는 "501호 (ID: 1)"
            # room_name이 포함되고 바로 뒤에 (ID: X) 패턴이 있는지 확인
            pattern = rf"{re.escape(room_name)}\s*\(ID:\s*(\d+)\)"
            match = re.search(pattern, content, re.IGNORECASE)

            if match:
                room_id = int(match.group(1))
                self.logger.info(
                    f"대화 기록에서 찾음: '{room_name}' → room_id={room_id}"
                )
                return room_id

        self.logger.debug(f"대화 기록에서 '{room_name}'의 room_id를 찾지 못함")
        return None

    def _fallback_intent_detection(self, text: str) -> Dict[str, Any]:
        """JSON 파싱 실패 시 키워드 기반으로 의도를 추출합니다.

        Args:
            text: 사용자 입력 또는 LLM 응답

        Returns:
            intent와 entities 딕셔너리
        """
        text_lower = text.lower()

        # 키워드 기반 의도 분류
        if any(kw in text_lower for kw in ["예약", "취소", "변경", "수정"]):
            intent = "reservation"
        elif any(kw in text_lower for kw in ["조회", "확인", "목록", "내 예약"]):
            intent = "query"
        elif any(kw in text_lower for kw in ["회의실", "검색", "찾기", "층", "수용", "사용 가능"]):
            intent = "room"
        else:
            intent = "general"

        self.logger.info(f"폴백 의도 분류: {intent}")

        return {
            "intent": intent,
            "entities": {},
        }