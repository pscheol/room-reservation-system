package com.devpaik.metting.application.room.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.usecase.QueryRoomUseCase
import com.devpaik.metting.application.room.usecase.query.AvailabilityQuery
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement
import com.devpaik.metting.domain.room.service.RoomMatcher
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회의실 조회 애플리케이션 서비스
 * 역할: 도메인 서비스를 활용한 회의실 검색 조율
 */
@Service
@Transactional(readOnly = true)
class QueryRoomService(
    private val loadRoomPort: LoadRoomPort,
    private val loadReservationPort: LoadReservationPort,
    private val roomMatcher: RoomMatcher
) : QueryRoomUseCase {

    override fun getRoom(id: RoomId): Room {
        return loadRoomPort.loadRoom(id)
            ?: throw IllegalArgumentException("회의실을 찾을 수 없습니다: ${id.value}")
    }

    override fun searchRooms(query: RoomQuery): List<Room> {
        return loadRoomPort.loadRooms(query)
    }

    override fun findAvailableRooms(query: AvailableRoomQuery): List<Room> {
        // 1. 가용 회의실 조회 (인프라 레이어)
        val availableRooms = loadRoomPort.loadAvailableRooms(query)

        // 2. 요구사항이 있는 경우 도메인 서비스를 통한 필터링
        return if (query.minCapacity != null) {
            filterByRequirement(availableRooms, query)
        } else {
            availableRooms
        }
    }

    override fun checkAvailability(query: AvailabilityQuery): Boolean {
        // 1. 회의실 조회
        val room = loadRoomPort.loadRoom(query.roomId) ?: return false

        // 2. 도메인 모델을 통한 기본 검증
        if (!room.canBeReservedAt(query.startTime, query.endTime)) {
            return false
        }

        // 3. 충돌하는 예약 조회
        val conflicts = loadReservationPort.loadConflictingReservations(
            roomId = query.roomId,
            date = query.date,
            startTime = query.startTime,
            endTime = query.endTime,
            excludeReservationId = query.excludeReservationId
        )

        return conflicts.isEmpty()
    }

    /**
     * 요구사항에 따른 필터링
     */
    private fun filterByRequirement(
        availableRooms: List<Room>,
        query: AvailableRoomQuery
    ): List<Room> {
        val requirement = RoomRequirement(
            requiredCapacity = query.minCapacity ?: 1,
            startTime = query.startTime,
            endTime = query.endTime
        )

        return roomMatcher.filterSuitableRooms(availableRooms, requirement)
    }
}