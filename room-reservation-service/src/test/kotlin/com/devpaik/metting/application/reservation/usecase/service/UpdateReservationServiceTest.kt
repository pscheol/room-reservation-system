package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.output.SaveReservationPort
import com.devpaik.metting.application.reservation.usecase.command.CancelReservationCommand
import com.devpaik.metting.application.reservation.usecase.command.UpdateReservationCommand
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import com.devpaik.metting.domain.reservation.service.ReservationUpdater
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class UpdateReservationServiceTest : BehaviorSpec({

    val loadReservationPort = mockk<LoadReservationPort>()
    val saveReservationPort = mockk<SaveReservationPort>()
    val reservationUpdater = mockk<ReservationUpdater>()

    val service = UpdateReservationService(
        loadReservationPort,
        saveReservationPort,
        reservationUpdater
    )

    Given("예약 수정") {
        val reservationId = ReservationId(1L)
        val existingReservation = createConfirmedReservation(reservationId)
        val command = UpdateReservationCommand(
            reservationId = reservationId,
            title = "수정된 회의",
            contents = "수정된 내용",
            reservationDate = existingReservation.reservationDate,
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0),
            status = ReservationStatus.CONFIRMED
        )
        val updatedReservation = existingReservation.copy(
            title = ReservationTitle("수정된 회의"),
            contents = "수정된 내용",
            period = ReservationPeriod(LocalTime.of(14, 0), LocalTime.of(15, 0))
        )

        When("유효한 수정 요청을 하면") {
            every { loadReservationPort.loadReservation(reservationId) } returns existingReservation
            every { loadReservationPort.loadConflictingReservations(any(), any(), any(), any(), any()) } returns emptyList()
            every { reservationUpdater.update(any(), any(), any()) } returns updatedReservation
            every { saveReservationPort.saveReservation(any()) } returns updatedReservation

            val result = service.updateReservation(command)

            Then("예약이 수정되고 저장된다") {
                result.title.value shouldBe "수정된 회의"
                result.contents shouldBe "수정된 내용"
                verify(exactly = 1) { loadReservationPort.loadReservation(reservationId) }
                verify(exactly = 1) { reservationUpdater.update(any(), any(), any()) }
                verify(exactly = 1) { saveReservationPort.saveReservation(any()) }
            }
        }
    }

    Given("예약 취소") {
        val reservationId = ReservationId(1L)
        val organizerEmail = "organizer@example.com"
        val existingReservation = createReservationWithOrganizer(reservationId, organizerEmail)
        val command = CancelReservationCommand(
            reservationId = reservationId,
            userEmail = UserEmail(organizerEmail)
        )
        val cancelledReservation = existingReservation.copy(
            status = ReservationStatus.CANCELLED,
            cancelledAt = LocalDateTime.now()
        )

        When("예약자가 취소 요청을 하면") {
            every { loadReservationPort.loadReservation(reservationId) } returns existingReservation
            every { saveReservationPort.saveReservation(any()) } returns cancelledReservation

            val result = service.cancelReservation(command)

            Then("예약이 취소된다") {
                result.status shouldBe ReservationStatus.CANCELLED
            }
        }

        When("예약자가 아닌 사람이 취소 요청을 하면") {
            val nonOrganizerCommand = CancelReservationCommand(
                reservationId = reservationId,
                userEmail = UserEmail("other@example.com")
            )

            every { loadReservationPort.loadReservation(reservationId) } returns existingReservation

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    service.cancelReservation(nonOrganizerCommand)
                }
                exception.message shouldBe "예약을 취소할 권한이 없습니다."
            }
        }
    }
})

private fun createConfirmedReservation(id: ReservationId): Reservation {
    return Reservation.reconstitute(
        id = id,
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

private fun createReservationWithOrganizer(id: ReservationId, organizerEmail: String): Reservation {
    val organizer = Participant(
        id = ParticipantId(1L),
        participantEmail = ParticipantEmail(organizerEmail),
        participantType = ParticipantType.ORGANIZER,
        invitedAt = LocalDateTime.now()
    )

    return Reservation.reconstitute(
        id = id,
        roomId = RoomId(1L),
        userEmail = UserEmail(organizerEmail),
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
        participants = listOf(organizer)
    )
}