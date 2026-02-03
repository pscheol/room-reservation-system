package com.devpaik.metting.domain.reservation.aggregate

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReservationTest : BehaviorSpec({

    Given("예약 생성") {
        When("유효한 정보로 예약을 생성하면") {
            val reservation = Reservation.create(
                roomId = RoomId(1L),
                userEmail = UserEmail("test@example.com"),
                title = "주간 회의",
                contents = "회의 내용",
                reservationDate = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                participants = emptyList()
            )

            Then("예약이 CONFIRMED 상태로 생성된다") {
                reservation.id shouldBe null
                reservation.roomId shouldBe RoomId(1L)
                reservation.userEmail shouldBe UserEmail("test@example.com")
                reservation.title shouldBe ReservationTitle("주간 회의")
                reservation.status shouldBe ReservationStatus.CONFIRMED
                reservation.createdAt shouldNotBe null
            }
        }

        When("과거 날짜로 예약을 생성하면") {
            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    Reservation.create(
                        roomId = RoomId(1L),
                        userEmail = UserEmail("test@example.com"),
                        title = "과거 회의",
                        contents = null,
                        reservationDate = LocalDate.now().minusDays(1),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(11, 0)
                    )
                }
                exception.message shouldBe "과거 날짜로 예약할 수 없습니다"
            }
        }

        When("3개월 이후로 예약을 생성하면") {
            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    Reservation.create(
                        roomId = RoomId(1L),
                        userEmail = UserEmail("test@example.com"),
                        title = "먼 미래 회의",
                        contents = null,
                        reservationDate = LocalDate.now().plusMonths(4),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(11, 0)
                    )
                }
                exception.message shouldBe "3개월 이후로는 예약할 수 없습니다"
            }
        }
    }

    Given("예약 취소") {
        When("예약자가 예약을 취소하면") {
            val reservation = createConfirmedReservation()
            val cancelledReservation = reservation.cancel(UserEmail("test@example.com"))

            Then("CANCELLED 상태가 된다") {
                cancelledReservation.status shouldBe ReservationStatus.CANCELLED
                cancelledReservation.cancelledAt shouldNotBe null
            }
        }

        When("예약자가 아닌 사람이 취소하면") {
            val reservation = createConfirmedReservation()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservation.cancel(UserEmail("other@example.com"))
                }
                exception.message shouldBe "예약자만 예약을 취소할 수 있습니다"
            }
        }

        When("이미 취소된 예약을 취소하면") {
            val reservation = createCancelledReservation()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservation.cancel(UserEmail("test@example.com"))
                }
                exception.message shouldBe "이미 취소된 예약입니다"
            }
        }

        When("완료된 예약을 취소하면") {
            val reservation = createCompletedReservation()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservation.cancel(UserEmail("test@example.com"))
                }
                exception.message shouldBe "완료된 예약은 취소할 수 없습니다"
            }
        }
    }

    Given("예약 수정") {
        When("CONFIRMED 상태인 예약을 수정하면") {
            val reservation = createConfirmedReservation()
            val updatedReservation = reservation.update(
                title = "수정된 회의",
                contents = "수정된 내용",
                reservationDate = LocalDate.now().plusDays(2),
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0)
            )

            Then("새로운 불변 객체가 반환된다") {
                updatedReservation shouldNotBe reservation
                updatedReservation.title shouldBe ReservationTitle("수정된 회의")
                updatedReservation.contents shouldBe "수정된 내용"
                updatedReservation.reservationDate shouldBe LocalDate.now().plusDays(2)
                updatedReservation.period.startTime shouldBe LocalTime.of(14, 0)
                updatedReservation.period.endTime shouldBe LocalTime.of(15, 0)
            }
        }

        When("취소된 예약을 수정하면") {
            val reservation = createCancelledReservation()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservation.update(
                        title = "수정 시도",
                        contents = null,
                        reservationDate = LocalDate.now().plusDays(1),
                        startTime = LocalTime.of(10, 0),
                        endTime = LocalTime.of(11, 0)
                    )
                }
                exception.message shouldBe "취소되었거나 완료된 예약은 수정할 수 없습니다"
            }
        }
    }

    Given("예약 완료 처리") {
        When("예약 종료 시간이 지난 CONFIRMED 예약을 완료 처리하면") {
            val reservation = createPastReservation()
            val completedReservation = reservation.complete()

            Then("COMPLETED 상태가 된다") {
                completedReservation.status shouldBe ReservationStatus.COMPLETED
            }
        }
    }

    Given("참여자 추가") {
        val reservation = createConfirmedReservation()
        val participant = createParticipant("participant@example.com")

        When("참여자를 추가하면") {
            val updatedReservation = reservation.addParticipant(participant)

            Then("참여자 목록에 추가된다") {
                updatedReservation.participants.size shouldBe 1
                updatedReservation.participants.first().participantEmail shouldBe ParticipantEmail("participant@example.com")
            }
        }

        When("이미 등록된 참여자를 추가하면") {
            val reservationWithParticipant = reservation.addParticipant(participant)

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservationWithParticipant.addParticipant(participant)
                }
                exception.message shouldBe "이미 등록된 참여자입니다: ${participant.participantEmail}"
            }
        }

        When("취소된 예약에 참여자를 추가하면") {
            val cancelledReservation = createCancelledReservation()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    cancelledReservation.addParticipant(participant)
                }
                exception.message shouldBe "취소된 예약에는 참여자를 추가할 수 없습니다"
            }
        }
    }

    Given("참여자 제거") {
        val participant = createParticipant("participant@example.com")
        val reservation = createConfirmedReservation().addParticipant(participant)

        When("참여자를 제거하면") {
            val updatedReservation = reservation.removeParticipant("participant@example.com")

            Then("참여자 목록에서 제거된다") {
                updatedReservation.participants.size shouldBe 0
            }
        }

        When("존재하지 않는 참여자를 제거하면") {
            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    reservation.removeParticipant("notexist@example.com")
                }
                exception.message shouldBe "참여자를 찾을 수 없습니다: notexist@example.com"
            }
        }
    }

    Given("Google Calendar Event ID 설정") {
        val reservation = createConfirmedReservation()

        When("Event ID를 설정하면") {
            val updatedReservation = reservation.withGoogleCalendarEventId("event123")

            Then("googleCalendarEventId가 설정된다") {
                updatedReservation.googleCalendarEventId shouldBe "event123"
            }
        }
    }

    Given("예약 시간 충돌 확인") {
        val reservation1 = createReservationWithTime(
            date = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0)
        )

        When("같은 날 시간이 겹치는 예약이 있으면") {
            val reservation2 = createReservationWithTime(
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 30),
                endTime = LocalTime.of(11, 30)
            )

            Then("충돌로 판단한다") {
                reservation1.isConflictWith(reservation2) shouldBe true
            }
        }

        When("같은 날 시간이 겹치지 않는 예약이 있으면") {
            val reservation2 = createReservationWithTime(
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(11, 0),
                endTime = LocalTime.of(12, 0)
            )

            Then("충돌이 없다고 판단한다") {
                reservation1.isConflictWith(reservation2) shouldBe false
            }
        }

        When("다른 날 예약이 있으면") {
            val reservation2 = createReservationWithTime(
                date = LocalDate.now().plusDays(2),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            Then("충돌이 없다고 판단한다") {
                reservation1.isConflictWith(reservation2) shouldBe false
            }
        }

        When("취소된 예약과 비교하면") {
            val cancelledReservation = createReservationWithTime(
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 30),
                endTime = LocalTime.of(11, 30),
                status = ReservationStatus.CANCELLED
            )

            Then("충돌이 없다고 판단한다") {
                reservation1.isConflictWith(cancelledReservation) shouldBe false
            }
        }
    }

    Given("수정 가능 여부 확인") {
        When("CONFIRMED 상태일 때") {
            val reservation = createConfirmedReservation()
            Then("수정 가능하다") {
                reservation.canBeModified() shouldBe true
            }
        }

        When("PENDING 상태일 때") {
            val reservation = createPendingReservation()
            Then("수정 가능하다") {
                reservation.canBeModified() shouldBe true
            }
        }

        When("CANCELLED 상태일 때") {
            val reservation = createCancelledReservation()
            Then("수정 불가능하다") {
                reservation.canBeModified() shouldBe false
            }
        }

        When("COMPLETED 상태일 때") {
            val reservation = createCompletedReservation()
            Then("수정 불가능하다") {
                reservation.canBeModified() shouldBe false
            }
        }
    }
})

private fun createConfirmedReservation(): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(1L),
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "주간 회의",
        contents = "회의 내용",
        reservationDate = LocalDate.now().plusDays(7),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = emptyList()
    )
}

private fun createCancelledReservation(): Reservation {
    return createConfirmedReservation().copy(
        status = ReservationStatus.CANCELLED,
        cancelledAt = LocalDateTime.now()
    )
}

private fun createCompletedReservation(): Reservation {
    return createConfirmedReservation().copy(status = ReservationStatus.COMPLETED)
}

private fun createPendingReservation(): Reservation {
    return createConfirmedReservation().copy(status = ReservationStatus.PENDING)
}

private fun createPastReservation(): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(1L),
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "지난 회의",
        contents = null,
        reservationDate = LocalDate.now().minusDays(1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now().minusDays(2),
        updatedAt = LocalDateTime.now().minusDays(2),
        cancelledAt = null,
        participants = emptyList()
    )
}

private fun createReservationWithTime(
    date: LocalDate,
    startTime: LocalTime,
    endTime: LocalTime,
    status: ReservationStatus = ReservationStatus.CONFIRMED
): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(1L),
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "테스트 회의",
        contents = null,
        reservationDate = date,
        startTime = startTime,
        endTime = endTime,
        status = status,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = emptyList()
    )
}

private fun createParticipant(email: String): Participant {
    return Participant(
        id = ParticipantId(1L),
        participantEmail = ParticipantEmail(email),
        participantType = ParticipantType.ATTENDEE,
        invitedAt = LocalDateTime.now()
    )
}