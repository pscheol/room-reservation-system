package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.common.vo.ValidationResult
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.exception.ReservationDomainException
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.clearMocks
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReservationUpdaterTest : BehaviorSpec({

    Given("예약 수정") {
        val conflictValidator = mockk<ReservationConflictValidator>()
        val updater = ReservationUpdater(conflictValidator)
        val reservation = createConfirmedReservation()
        val existingReservations = emptyList<Reservation>()

        When("시간이 변경되지 않으면") {
            val request = ReservationUpdateRequest(
                title = "수정된 회의",
                contents = "수정된 내용",
                reservationDate = reservation.reservationDate,
                startTime = reservation.period.startTime,
                endTime = reservation.period.endTime,
                status = ReservationStatus.CONFIRMED
            )

            val result = updater.update(reservation, request, existingReservations)

            Then("충돌 검증 없이 수정된다") {
                result.title.value shouldBe "수정된 회의"
                result.contents shouldBe "수정된 내용"
            }
        }

        When("시간이 변경되면") {
            clearMocks(conflictValidator)
            val request = ReservationUpdateRequest(
                title = "수정된 회의",
                contents = "수정된 내용",
                reservationDate = reservation.reservationDate,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                status = ReservationStatus.CONFIRMED
            )

            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.success()

            val result = updater.update(reservation, request, existingReservations)

            Then("충돌 검증 후 수정된다") {
                result.period.startTime shouldBe LocalTime.of(14, 0)
                result.period.endTime shouldBe LocalTime.of(15, 0)
            }
        }

        When("날짜가 변경되면") {
            clearMocks(conflictValidator)
            val request = ReservationUpdateRequest(
                title = "수정된 회의",
                contents = null,
                reservationDate = reservation.reservationDate.plusDays(1),
                startTime = reservation.period.startTime,
                endTime = reservation.period.endTime,
                status = ReservationStatus.CONFIRMED
            )

            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.success()

            val result = updater.update(reservation, request, existingReservations)

            Then("충돌 검증 후 수정된다") {
                result.reservationDate shouldBe reservation.reservationDate.plusDays(1)
            }
        }

        When("시간 변경 시 충돌이 발생하면") {
            clearMocks(conflictValidator)
            val request = ReservationUpdateRequest(
                title = "수정된 회의",
                contents = null,
                reservationDate = reservation.reservationDate,
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                status = ReservationStatus.CONFIRMED
            )

            every { conflictValidator.validateNoConflict(any(), any()) } returns ValidationResult.failure("시간이 충돌합니다")

            Then("ReservationDomainException이 발생한다") {
                val exception = shouldThrow<ReservationDomainException> {
                    updater.update(reservation, request, existingReservations)
                }
                exception.message shouldBe "시간이 충돌합니다"
            }
        }
    }

    Given("상태 변경") {
        val conflictValidator = mockk<ReservationConflictValidator>()
        val updater = ReservationUpdater(conflictValidator)
        val existingReservations = emptyList<Reservation>()

        When("COMPLETED로 상태를 변경하면") {
            val reservation = createCompletableReservation()
            val request = ReservationUpdateRequest(
                title = reservation.title.value,
                contents = reservation.contents,
                reservationDate = reservation.reservationDate,
                startTime = reservation.period.startTime,
                endTime = reservation.period.endTime,
                status = ReservationStatus.COMPLETED
            )

            val result = updater.update(reservation, request, existingReservations)

            Then("예약이 완료 처리된다") {
                result.status shouldBe ReservationStatus.COMPLETED
            }
        }

        When("CANCELLED로 상태를 변경하면") {
            val confirmedReservation = createConfirmedReservation()
            val request = ReservationUpdateRequest(
                title = confirmedReservation.title.value,
                contents = confirmedReservation.contents,
                reservationDate = confirmedReservation.reservationDate,
                startTime = confirmedReservation.period.startTime,
                endTime = confirmedReservation.period.endTime,
                status = ReservationStatus.CANCELLED
            )

            Then("ReservationDomainException이 발생한다") {
                shouldThrow<ReservationDomainException> {
                    updater.update(confirmedReservation, request, existingReservations)
                }
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

private fun createCompletableReservation(): Reservation {
    // 완료 가능한 예약: 예약 날짜는 오늘, 종료 시간은 현재 시간 이전
    val now = LocalDateTime.now()
    return Reservation.reconstitute(
        id = ReservationId(1L),
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "완료 가능 회의",
        contents = null,
        reservationDate = now.toLocalDate(),
        startTime = now.minusHours(2).toLocalTime(),
        endTime = now.minusHours(1).toLocalTime(),
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = now.minusDays(1),
        updatedAt = now.minusDays(1),
        cancelledAt = null,
        participants = emptyList()
    )
}