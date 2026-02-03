package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.output.SaveReservationPort
import com.devpaik.metting.application.reservation.usecase.command.CreateReservationCommand
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import com.devpaik.metting.domain.reservation.service.ReservationFactory
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class CreateReservationServiceTest : BehaviorSpec({

    val loadRoomPort = mockk<LoadRoomPort>()
    val loadReservationPort = mockk<LoadReservationPort>()
    val saveReservationPort = mockk<SaveReservationPort>()
    val reservationFactory = mockk<ReservationFactory>()

    val service = CreateReservationService(
        loadRoomPort,
        loadReservationPort,
        saveReservationPort,
        reservationFactory
    )

    Given("예약 생성") {
        val command = CreateReservationCommand(
            roomId = RoomId(1L),
            userEmail = UserEmail("test@example.com"),
            title = "주간 회의",
            contents = "회의 내용",
            reservationDate = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            participants = emptyList()
        )
        val room = createAvailableRoom()
        val createdReservation = createReservation()
        val savedReservation = createdReservation.copy(id = ReservationId(1L))

        When("유효한 예약 요청을 하면") {
            every { loadRoomPort.loadRoom(command.roomId) } returns room
            every { loadReservationPort.loadConflictingReservations(any(), any(), any(), any(), any()) } returns emptyList()
            every { reservationFactory.create(any(), any(), any()) } returns createdReservation
            every { saveReservationPort.saveReservation(any()) } returns savedReservation

            val result = service.createReservation(command)

            Then("예약이 생성되고 저장된다") {
                result.first shouldBe savedReservation
                result.second shouldBe room
                verify(exactly = 1) { loadRoomPort.loadRoom(command.roomId) }
                verify(exactly = 1) { saveReservationPort.saveReservation(any()) }
            }
        }

        When("충돌하는 예약이 있어도") {
            val existingReservations = listOf(createExistingReservation())

            every { loadRoomPort.loadRoom(command.roomId) } returns room
            every { loadReservationPort.loadConflictingReservations(any(), any(), any(), any(), any()) } returns existingReservations
            every { reservationFactory.create(any(), any(), any()) } returns createdReservation
            every { saveReservationPort.saveReservation(any()) } returns savedReservation

            val result = service.createReservation(command)

            Then("ReservationFactory에서 충돌 검증을 수행한다") {
                result shouldNotBe null
                verify(exactly = 1) { reservationFactory.create(any(), room, existingReservations) }
            }
        }
    }
})

private fun createAvailableRoom(): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = 10,
        contents = "프로젝터 있음",
        status = RoomStatus.AVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createReservation(): Reservation {
    return Reservation(
        id = null,
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = ReservationTitle("주간 회의"),
        contents = "회의 내용",
        reservationDate = LocalDate.now().plusDays(1),
        period = ReservationPeriod(LocalTime.of(10, 0), LocalTime.of(11, 0)),
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = listOf(
            Participant(
                id = null,
                participantEmail = ParticipantEmail("test@example.com"),
                participantType = ParticipantType.ORGANIZER,
                invitedAt = LocalDateTime.now()
            )
        )
    )
}

private fun createExistingReservation(): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(99L),
        roomId = RoomId(1L),
        userEmail = UserEmail("other@example.com"),
        title = "기존 회의",
        contents = null,
        reservationDate = LocalDate.now().plusDays(1),
        startTime = LocalTime.of(10, 30),
        endTime = LocalTime.of(11, 30),
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = emptyList()
    )
}