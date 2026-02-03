package com.devpaik.metting.domain.room.aggregate.vo

import java.time.LocalTime

/**
 * 회의실 요구사항
 */
data class RoomRequirement(
    val requiredCapacity: Int,
    val startTime: LocalTime,
    val endTime: LocalTime
) {
    init {
        require(requiredCapacity > 0) { "요청 인원은 1명 이상이어야 합니다" }
        require(endTime > startTime) { "종료 시간은 시작 시간보다 이후여야 합니다" }
    }
}