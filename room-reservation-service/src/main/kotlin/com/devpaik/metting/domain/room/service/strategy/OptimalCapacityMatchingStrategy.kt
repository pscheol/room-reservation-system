package com.devpaik.metting.domain.room.service.strategy

import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement

/**
 * 최적 수용 인원 매칭
 * 요청 인원과 가장 근접한 회의실을 선택하는
 */
class OptimalCapacityMatchingStrategy : RoomMatchingStrategy {
    override fun selectBest(rooms: List<Room>, requirement: RoomRequirement): Room? {
        return rooms
            .sortedBy { room ->
                // 요청 인원과 가장 근접한 수용 인원
                room.capacity.value - requirement.requiredCapacity
            }
            .firstOrNull()
    }
}