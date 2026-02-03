package com.devpaik.metting.domain.room.service.strategy

import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement

/**
 * 회의실 매칭 인터페이스
 */
interface RoomMatchingStrategy {
    fun selectBest(rooms: List<Room>, requirement: RoomRequirement): Room?
}