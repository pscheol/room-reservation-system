package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.usecase.QueryReservationUseCase
import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate


@Service
@Transactional(readOnly = true)
class QueryReservationService(
    private val loadReservationPort: LoadReservationPort
) : QueryReservationUseCase {

    override fun getReservation(id: ReservationId): Reservation {
        return loadReservationPort.loadReservation(id)
    }

    override fun searchReservations(query: ReservationQuery): List<Reservation> {
        return loadReservationPort.loadReservations(query)
    }

    override fun getUpcomingReservations(email: String): List<Reservation> {
        return loadReservationPort.loadUpcomingReservations(email, LocalDate.now())
    }
}