package com.devpaik.metting.application.reservation.output

import com.devpaik.metting.domain.reservation.aggregate.Reservation

interface SaveReservationPort {
    fun saveReservation(reservation: Reservation): Reservation
}