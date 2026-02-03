package com.devpaik.metting.application.reservation.usecase.command

import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import java.time.LocalDate
import java.time.LocalTime

data class UpdateReservationCommand(
    val reservationId: ReservationId,
    val title: String,
    val contents: String?,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: ReservationStatus
)