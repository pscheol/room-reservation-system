package com.devpaik.metting.application.reservation.usecase.command

import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId

data class CancelReservationCommand(
    val reservationId: ReservationId,
    val userEmail: UserEmail
)