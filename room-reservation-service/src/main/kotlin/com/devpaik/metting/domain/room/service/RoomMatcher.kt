package com.devpaik.metting.domain.room.service

import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement
import com.devpaik.metting.domain.room.service.strategy.RoomMatchingStrategy

/**
 * 회의실 매칭 도메인 서비스 인터페이스
 */
interface RoomMatcher {
    fun findBestMatch(
        availableRooms: List<Room>,
        requirement: RoomRequirement
    ): Room?

    fun filterSuitableRooms(
        rooms: List<Room>,
        requirement: RoomRequirement
    ): List<Room>
}

/**
 * 회의실 매칭 도메인 서비스
 */
class RoomMatcherService(
    private val matchingStrategy: RoomMatchingStrategy
) : RoomMatcher {

    override fun findBestMatch(
        availableRooms: List<Room>,
        requirement: RoomRequirement
    ): Room? {
        val suitableRooms = filterSuitableRooms(availableRooms, requirement)
        if (suitableRooms.isEmpty()) return null

        return matchingStrategy.selectBest(suitableRooms, requirement)
    }

    override fun filterSuitableRooms(
        rooms: List<Room>,
        requirement: RoomRequirement
    ): List<Room> {
        return rooms.filter { room ->
            RoomSuitabilityChecker.isSuitable(room, requirement)
        }
    }
}

/**
 * 회의실 적합성 체크 헬퍼 객체
 */
private object RoomSuitabilityChecker {
    fun isSuitable(room: Room, requirement: RoomRequirement): Boolean {
        return room.isAvailable() &&
                room.canAccommodate(requirement.requiredCapacity) &&
                room.canBeReservedAt(requirement.startTime, requirement.endTime)
    }
}