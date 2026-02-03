package com.devpaik.metting.application.reservation.output

import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import java.time.LocalDate
import java.time.LocalTime

interface LoadReservationPort {
    fun loadReservation(id: ReservationId): Reservation

    fun loadReservations(query: ReservationQuery): List<Reservation>

    fun loadUpcomingReservations(email: String, fromDate: LocalDate): List<Reservation>

    fun loadConflictingReservations(
        roomId: RoomId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeReservationId: ReservationId? = null
    ): List<Reservation>
}
