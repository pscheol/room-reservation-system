package com.devpaik.metting.application.room.usecase.query

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import java.time.LocalDate
import java.time.LocalTime

data class AvailabilityQuery(
    val roomId: RoomId,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val excludeReservationId: ReservationId? = null
)