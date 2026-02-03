package com.devpaik.metting.application.reservation.usecase

import com.devpaik.metting.application.reservation.usecase.command.CancelReservationCommand
import com.devpaik.metting.application.reservation.usecase.command.UpdateReservationCommand
import com.devpaik.metting.domain.reservation.aggregate.Reservation

interface UpdateReservationUseCase {
    fun updateReservation(command: UpdateReservationCommand): Reservation

    fun cancelReservation(command: CancelReservationCommand): Reservation
}