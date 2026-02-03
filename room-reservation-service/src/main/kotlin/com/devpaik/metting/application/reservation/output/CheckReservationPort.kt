package com.devpaik.metting.application.reservation.output

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import java.time.LocalDate
import java.time.LocalTime

interface CheckReservationPort {
    fun existsConflict(
        roomId: RoomId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeReservationId: ReservationId? = null
    ): Boolean
}