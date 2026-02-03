package com.devpaik.metting.domain.room.aggregate

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomCapacity
import com.devpaik.metting.domain.room.aggregate.vo.RoomName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 회의실 Aggregate Root
 */
data class Room(
    val id: RoomId?,
    val buildingName: BuildingName,
    val floor: Floor,
    val roomName: RoomName,
    val capacity: RoomCapacity,
    val contents: String?,
    val status: RoomStatus,
    val createdAt: LocalDateTime?,
    val updatedAt: LocalDateTime?
) {
    companion object {
        /**
         * 회의실 생성
         */
        fun create(
            floor: Long,
            buildingName: String,
            roomName: String,
            capacity: Int,
            contents: String?
        ): Room {
            return Room(
                id = null,
                floor = Floor(floor),
                buildingName = BuildingName(buildingName),
                roomName = RoomName(roomName),
                capacity = RoomCapacity(capacity),
                contents = contents,
                status = RoomStatus.AVAILABLE,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now()
            )
        }

        /**
         * 재구성 - 영속화된 데이터로부터 복원
         */
        fun reconstitute(
            id: RoomId,
            buildingName: BuildingName,
            floor: Floor,
            roomName: String,
            capacity: Int,
            contents: String?,
            status: RoomStatus,
            createdAt: LocalDateTime?,
            updatedAt: LocalDateTime?
        ): Room {
            return Room(
                id = id,
                buildingName= buildingName,
                floor = floor,
                roomName = RoomName(roomName),
                capacity = RoomCapacity(capacity),
                contents = contents,
                status = status,
                createdAt = createdAt,
                updatedAt = updatedAt
            )
        }
    }

    /**
     * 회의실 정보 수정 - 새로운 불변 객체 반환
     */
    fun update(buildingName: String, roomName: String, capacity: Int, contents: String?): Room {
        return copy(
            buildingName = BuildingName(buildingName),
            roomName = RoomName(roomName),
            capacity = RoomCapacity(capacity),
            contents = contents,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 회의실 사용 가능 여부
     */
    fun isAvailable(): Boolean = status == RoomStatus.AVAILABLE

    /**
     * 요청된 인원 수용 가능 여부
     */
    fun canAccommodate(requiredCapacity: Int): Boolean {
        return capacity.value >= requiredCapacity
    }

    /**
     * 회의실 사용 불가 처리 - 새로운 불변 객체 반환
     */
    fun makeUnavailable(reason: String?): Room {
        require(status != RoomStatus.MAINTENANCE) { "이미 정비 중인 회의실입니다" }

        return copy(
            status = RoomStatus.UNAVAILABLE,
            contents = reason ?: contents,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 회의실 정비 시작 - 새로운 불변 객체 반환
     */
    fun startMaintenance(reason: String): Room {
        require(reason.isNotBlank()) { "정비 사유는 필수입니다" }

        return copy(
            status = RoomStatus.MAINTENANCE,
            contents = reason,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 회의실 사용 가능 처리 - 새로운 불변 객체 반환
     */
    fun makeAvailable(): Room {
        require(status != RoomStatus.AVAILABLE) { "이미 사용 가능한 회의실입니다" }

        return copy(
            status = RoomStatus.AVAILABLE,
            updatedAt = LocalDateTime.now()
        )
    }

    /**
     * 특정 시간대에 예약 가능 여부
     */
    fun canBeReservedAt(startTime: LocalTime, endTime: LocalTime): Boolean {
        if (!isAvailable()) return false

        val businessStartTime = LocalTime.of(6, 0)
        val businessEndTime = LocalTime.of(23, 0)

        return !startTime.isBefore(businessStartTime) && !endTime.isAfter(businessEndTime)
    }

    /**
     * 예약에 적합한지 검증 (인원)
     */
    fun isSuitableFor(requiredCapacity: Int): Boolean {
        return canAccommodate(requiredCapacity)
    }
}