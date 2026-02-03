package com.devpaik.metting.domain.participant.aggregate

import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime

class ParticipantTest : BehaviorSpec({

    Given("참여자 생성") {
        When("ATTENDEE 타입으로 참여자를 생성하면") {
            val participant = Participant.create(
                email = "attendee@example.com",
                type = ParticipantType.ATTENDEE
            )

            Then("참여자가 생성된다") {
                participant.id shouldBe null
                participant.participantEmail shouldBe ParticipantEmail("attendee@example.com")
                participant.participantType shouldBe ParticipantType.ATTENDEE
                participant.invitedAt shouldNotBe null
            }
        }

        When("ORGANIZER 타입으로 참여자를 생성하면") {
            val participant = Participant.create(
                email = "organizer@example.com",
                type = ParticipantType.ORGANIZER
            )

            Then("참여자가 생성된다") {
                participant.participantEmail shouldBe ParticipantEmail("organizer@example.com")
                participant.participantType shouldBe ParticipantType.ORGANIZER
            }
        }
    }

    Given("참여자 reconstitute") {
        When("영속화된 데이터로 참여자를 복원하면") {
            val invitedAt = LocalDateTime.now().minusHours(1)
            val participant = Participant.reconstitute(
                id = ParticipantId(1L),
                email = "participant@example.com",
                type = ParticipantType.ATTENDEE,
                invitedAt = invitedAt
            )

            Then("모든 필드가 정확히 복원된다") {
                participant.id shouldBe ParticipantId(1L)
                participant.participantEmail shouldBe ParticipantEmail("participant@example.com")
                participant.participantType shouldBe ParticipantType.ATTENDEE
                participant.invitedAt shouldBe invitedAt
            }
        }
    }

    Given("참여자 타입 확인") {
        When("ORGANIZER 타입일 때") {
            val participant = Participant.create(
                email = "organizer@example.com",
                type = ParticipantType.ORGANIZER
            )

            Then("participantType이 ORGANIZER이다") {
                participant.participantType shouldBe ParticipantType.ORGANIZER
            }
        }

        When("ATTENDEE 타입일 때") {
            val participant = Participant.create(
                email = "attendee@example.com",
                type = ParticipantType.ATTENDEE
            )

            Then("participantType이 ATTENDEE이다") {
                participant.participantType shouldBe ParticipantType.ATTENDEE
            }
        }
    }
})