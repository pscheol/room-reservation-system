package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.common.vo.ValidationResult
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.exception.ReservationDomainException
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReservationFactoryTest : BehaviorSpec({

    val availabilityValidator = mockk<ReservationAvailabilityValidator>()
    val conflictValidator = mockk<ReservationConflictValidator>()

    val factory = ReservationFactory(availabilityValidator, conflictValidator)

    Given("예약 생성") {
        val room = createAvailableRoom()
        val request = createReservationRequest()
        val existingReservations = emptyList<Reservation>()

        When("모든 검증이 성공하면") {
            every { availabilityValidator.validateAvailability(any()) } returns ValidationResult.success()
            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.success()

            val result = factory.create(request, room, existingReservations)

            Then("예약이 생성된다") {
                result shouldNotBe null
                result.roomId shouldBe request.roomId
                result.userEmail shouldBe request.userEmail
                result.title.value shouldBe request.title
                result.status shouldBe ReservationStatus.CONFIRMED
            }
        }

        When("참여자 정보가 있으면") {
            val requestWithParticipants = request.copy(
                participants = listOf(
                    ParticipantCreationInfo("organizer@example.com", ParticipantType.ORGANIZER),
                    ParticipantCreationInfo("attendee@example.com", ParticipantType.ATTENDEE)
                )
            )

            every { availabilityValidator.validateAvailability(any()) } returns ValidationResult.success()
            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.success()

            val result = factory.create(requestWithParticipants, room, existingReservations)

            Then("참여자가 포함된 예약이 생성된다") {
                result.participants.size shouldBe 2
            }
        }

        When("가용성 검증이 실패하면") {
            every { availabilityValidator.validateAvailability(any()) } returns ValidationResult.failure("회의실이 사용 불가능 상태입니다")

            Then("ReservationDomainException이 발생한다") {
                val exception = shouldThrow<ReservationDomainException> {
                    factory.create(request, room, existingReservations)
                }
                exception.message shouldBe "회의실이 사용 불가능 상태입니다"
            }
        }

        When("시간 충돌 검증이 실패하면") {
            every { availabilityValidator.validateAvailability(any()) } returns ValidationResult.success()
            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.failure("다음 시간대와 충돌합니다: 10:00 ~ 11:00")

            Then("ReservationDomainException이 발생한다") {
                val exception = shouldThrow<ReservationDomainException> {
                    factory.create(request, room, existingReservations)
                }
                exception.message?.contains("충돌") shouldBe true
            }
        }
    }

    Given("기존 예약과의 충돌 검증") {
        val room = createAvailableRoom()
        val request = createReservationRequest()

        When("기존 예약과 시간이 겹치면") {
            val existingReservations = listOf(
                createExistingReservation(
                    startTime = LocalTime.of(10, 30),
                    endTime = LocalTime.of(11, 30)
                )
            )

            every { availabilityValidator.validateAvailability(any()) } returns ValidationResult.success()
            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.failure("시간이 충돌합니다")

            Then("ReservationDomainException이 발생한다") {
                shouldThrow<ReservationDomainException> {
                    factory.create(request, room, existingReservations)
                }
            }
        }
    }
})

private fun createAvailableRoom(capacity: Int = 10): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = capacity,
        contents = "프로젝터 있음",
        status = RoomStatus.AVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createReservationRequest(): ReservationCreationRequest {
    return ReservationCreationRequest(
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "주간 회의",
        contents = "회의 내용",
        reservationDate = LocalDate.now().plusDays(1),
        startTime = LocalTime.of(10, 0),
        endTime = LocalTime.of(11, 0),
        participants = listOf(
            ParticipantCreationInfo("test@example.com", ParticipantType.ORGANIZER)
        )
    )
}

private fun createExistingReservation(
    startTime: LocalTime,
    endTime: LocalTime
): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(99L),
        roomId = RoomId(1L),
        userEmail = UserEmail("other@example.com"),
        title = "기존 회의",
        contents = null,
        reservationDate = LocalDate.now().plusDays(1),
        startTime = startTime,
        endTime = endTime,
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = emptyList()
    )
}