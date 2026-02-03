package com.devpaik.metting.domain.room.aggregate.vo

@JvmInline
value class RoomName(val value: String) {
    init {
        require(value.isNotBlank()) { "회의실명은 필수입니다" }
        require(value.length <= 100) { "회의실명은 100자를 초과할 수 없습니다" }
    }
}