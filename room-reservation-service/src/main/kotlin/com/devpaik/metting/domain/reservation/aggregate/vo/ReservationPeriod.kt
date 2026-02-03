package com.devpaik.metting.domain.reservation.aggregate.vo

import java.time.Duration
import java.time.LocalTime

data class ReservationPeriod(val startTime: LocalTime, val endTime: LocalTime) {
    init {
        require(endTime > startTime) { "종료 시간은 시작 시간보다 이후여야 합니다" }
        val duration = Duration.between(startTime, endTime)
        require(duration.toMinutes() >= 30) { "예약 시간은 최소 30분 이상이어야 합니다" }
        require(duration.toHours() <= 8) { "예약 시간은 최대 8시간을 초과할 수 없습니다" }
    }
}