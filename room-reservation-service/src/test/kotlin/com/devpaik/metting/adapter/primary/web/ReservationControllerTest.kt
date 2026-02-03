package com.devpaik.metting.adapter.primary.web

import com.devpaik.metting.adapter.primary.web.dto.CreateReservationRequest
import com.devpaik.metting.adapter.primary.web.dto.UpdateReservationRequest
import com.devpaik.metting.application.reservation.usecase.CreateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.QueryReservationUseCase
import com.devpaik.metting.application.reservation.usecase.UpdateReservationUseCase
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReservationControllerTest : BehaviorSpec({

    val createReservationUseCase = mockk<CreateReservationUseCase>()
    val updateReservationUseCase = mockk<UpdateReservationUseCase>()
    val queryReservationUseCase = mockk<QueryReservationUseCase>()
    val loadRoomPort = mockk<LoadRoomPort>()

    val controller = ReservationController(
        createReservationUseCase,
        updateReservationUseCase,
        queryReservationUseCase,
        loadRoomPort
    )

    fun createReservation(id: Long = 1L): Reservation {
        return Reservation(
            id = ReservationId(id),
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
            participants = emptyList()
        )
    }

    fun createRoom(): Room {
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

    Given("예약 생성") {
        val reservation = createReservation()
        val room = createRoom()
        val request = CreateReservationRequest(
            roomId = 1L,
            email = "test@example.com",
            title = "주간 회의",
            contents = "회의 내용",
            reservationDate = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            participants = emptyList()
        )

        When("POST /api/reservations를 호출하면") {
            every { createReservationUseCase.createReservation(any()) } returns Pair(reservation, room)

            val response = controller.createReservation(request)

            Then("201 Created와 예약 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.CREATED
                response.body?.id shouldBe 1L
                response.body?.title shouldBe "주간 회의"
            }
        }
    }

    Given("예약 수정") {
        val reservation = createReservation()
        val room = createRoom()
        val request = UpdateReservationRequest(
            title = "수정된 회의",
            contents = "수정된 내용",
            reservationDate = LocalDate.now().plusDays(2),
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0),
            status = ReservationStatus.CONFIRMED
        )

        When("PUT /api/reservations/{id}를 호출하면") {
            val updatedReservation = reservation.copy(
                title = ReservationTitle("수정된 회의"),
                contents = "수정된 내용"
            )
            every { updateReservationUseCase.updateReservation(any()) } returns updatedReservation
            every { loadRoomPort.loadRoom(updatedReservation.roomId) } returns room

            val response = controller.updateReservation(1L, request)

            Then("200 OK와 수정된 예약 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.title shouldBe "수정된 회의"
            }
        }
    }

    Given("예약 취소") {
        val reservation = createReservation()
        val room = createRoom()
        val cancelledReservation = reservation.copy(
            status = ReservationStatus.CANCELLED,
            cancelledAt = LocalDateTime.now()
        )

        When("DELETE /api/reservations/{id}를 호출하면") {
            every { updateReservationUseCase.cancelReservation(any()) } returns cancelledReservation
            every { loadRoomPort.loadRoom(cancelledReservation.roomId) } returns room

            val response = controller.cancelReservation(1L, "test@example.com")

            Then("200 OK와 취소된 예약 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.status shouldBe ReservationStatus.CANCELLED
            }
        }
    }

    Given("예약 단일 조회") {
        val reservation = createReservation()
        val room = createRoom()

        When("GET /api/reservations/{id}를 호출하면") {
            every { queryReservationUseCase.getReservation(ReservationId(1L)) } returns reservation
            every { loadRoomPort.loadRoom(reservation.roomId) } returns room

            val response = controller.getReservation(1L)

            Then("200 OK와 예약 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.id shouldBe 1L
                response.body?.title shouldBe "주간 회의"
            }
        }
    }

    Given("예약 검색") {
        val reservations = listOf(createReservation(1L), createReservation(2L))
        val room = createRoom()

        When("GET /api/reservations를 호출하면") {
            every { queryReservationUseCase.searchReservations(any()) } returns reservations
            every { loadRoomPort.loadRoom(any()) } returns room

            val response = controller.searchReservations(
                email = "test@example.com",
                roomName = "회의실A",
                buildingName = "본관",
                startDate = LocalDate.now(),
                endDate = LocalDate.now().plusDays(7),
                status = ReservationStatus.CONFIRMED
            )

            Then("200 OK와 예약 목록을 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.size shouldBe 2
            }
        }
    }

    Given("예정된 예약 조회") {
        val reservations = listOf(createReservation(1L))
        val room = createRoom()

        When("GET /api/reservations/upcoming를 호출하면") {
            every { queryReservationUseCase.getUpcomingReservations(any()) } returns reservations
            every { loadRoomPort.loadRoom(any()) } returns room

            val response = controller.getUpcomingReservations("test@example.com")

            Then("200 OK와 예정된 예약 목록을 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.size shouldBe 1
            }
        }
    }
})