package com.devpaik.metting.application.reservation.usecase

import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId

interface QueryReservationUseCase {
    fun getReservation(id: ReservationId): Reservation

    fun searchReservations(query: ReservationQuery): List<Reservation>

    fun getUpcomingReservations(email: String): List<Reservation>
}
