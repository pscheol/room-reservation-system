package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class QueryReservationServiceTest : BehaviorSpec({

    val loadReservationPort = mockk<LoadReservationPort>()

    val service = QueryReservationService(loadReservationPort)

    Given("단일 예약 조회") {
        val reservationId = ReservationId(1L)
        val reservation = createReservation(reservationId)

        When("예약 ID로 조회하면") {
            every { loadReservationPort.loadReservation(reservationId) } returns reservation

            val result = service.getReservation(reservationId)

            Then("해당 예약을 반환한다") {
                result shouldBe reservation
                verify(exactly = 1) { loadReservationPort.loadReservation(reservationId) }
            }
        }
    }

    Given("예약 검색") {
        val query = ReservationQuery(
            userEmail = "test@example.com",
            roomName = "회의실A",
            buildingName = "본관",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7),
            status = ReservationStatus.CONFIRMED
        )
        val reservations = listOf(
            createReservation(ReservationId(1L)),
            createReservation(ReservationId(2L))
        )

        When("쿼리로 검색하면") {
            every { loadReservationPort.loadReservations(query) } returns reservations

            val result = service.searchReservations(query)

            Then("조건에 맞는 예약 목록을 반환한다") {
                result.size shouldBe 2
                verify(exactly = 1) { loadReservationPort.loadReservations(query) }
            }
        }
    }

    Given("예정된 예약 조회") {
        val email = "test@example.com"
        val upcomingReservations = listOf(
            createReservation(ReservationId(1L)),
            createReservation(ReservationId(2L))
        )

        When("이메일로 예정된 예약을 조회하면") {
            every { loadReservationPort.loadUpcomingReservations(email, any()) } returns upcomingReservations

            val result = service.getUpcomingReservations(email)

            Then("예정된 예약 목록을 반환한다") {
                result.size shouldBe 2
                verify(exactly = 1) { loadReservationPort.loadUpcomingReservations(email, any()) }
            }
        }
    }
})

private fun createReservation(id: ReservationId): Reservation {
    return Reservation(
        id = id,
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