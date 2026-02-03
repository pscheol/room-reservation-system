package com.devpaik.metting.domain.reservation.aggregate.vo

@JvmInline
value class ReservationTitle(val value: String) {
    init {
        require(value.isNotBlank()) { "예약 제목은 필수입니다" }
        require(value.length <= 200) { "예약 제목은 200자를 초과할 수 없습니다" }
    }
}