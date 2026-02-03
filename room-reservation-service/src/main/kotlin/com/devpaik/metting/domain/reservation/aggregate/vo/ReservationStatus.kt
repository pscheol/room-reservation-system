package com.devpaik.metting.domain.reservation.aggregate.vo

enum class ReservationStatus {
    CONFIRMED,   // 확정
    PENDING,     // 승인 대기
    CANCELLED,   // 취소
    COMPLETED    // 완료
}