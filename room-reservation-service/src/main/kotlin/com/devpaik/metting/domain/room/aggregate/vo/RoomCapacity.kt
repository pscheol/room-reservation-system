package com.devpaik.metting.domain.room.aggregate.vo

@JvmInline
value class RoomCapacity(val value: Int) {
    companion object {
        private const val MIN_CAPACITY = 1
        private const val MAX_CAPACITY = 100
    }

    init {
        require(value >= MIN_CAPACITY) { "수용 인원은 최소 ${MIN_CAPACITY}명 이상이어야 합니다" }
        require(value <= MAX_CAPACITY) { "수용 인원은 최대 ${MAX_CAPACITY}명을 초과할 수 없습니다" }
    }
}