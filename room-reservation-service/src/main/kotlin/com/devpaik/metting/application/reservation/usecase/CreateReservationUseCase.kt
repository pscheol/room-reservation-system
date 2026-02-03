package com.devpaik.metting.application.reservation.usecase

import com.devpaik.metting.application.reservation.usecase.command.CreateReservationCommand
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.room.aggregate.Room

interface CreateReservationUseCase {
    fun createReservation(command: CreateReservationCommand): Pair<Reservation, Room>
}