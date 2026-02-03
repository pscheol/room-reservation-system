package com.devpaik.metting.application.room.usecase.service

import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement
import com.devpaik.metting.domain.room.service.RoomMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회의실 추천 서비스
 * SRP - 추천 로직만 담당
 */
@Service
@Transactional(readOnly = true)
class RoomRecommendationService(
    private val loadRoomPort: LoadRoomPort,
    private val roomMatcher: RoomMatcher
) {
    /**
     * 회의실 추천
     */
    fun recommendBestRoom(query: AvailableRoomQuery, requiredCapacity: Int): Room? {
        val availableRooms = loadRoomPort.loadAvailableRooms(query)

        val requirement = RoomRequirement(
            requiredCapacity = requiredCapacity,
            startTime = query.startTime,
            endTime = query.endTime
        )

        // 회의실 매칭
        return roomMatcher.findBestMatch(availableRooms, requirement)
    }
}