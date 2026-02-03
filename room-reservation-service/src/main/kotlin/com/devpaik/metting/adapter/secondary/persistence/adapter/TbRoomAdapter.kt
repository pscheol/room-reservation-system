package com.devpaik.metting.adapter.secondary.persistence.adapter


import com.devpaik.metting.adapter.secondary.persistence.mapper.RoomMapper
import com.devpaik.metting.adapter.secondary.persistence.repository.TbRoomRepository
import com.devpaik.metting.application.room.output.DeleteRoomPort
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.output.SaveRoomPort
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import org.springframework.stereotype.Component

/**
 * 회의실 어댑터
 */
@Component
class TbRoomAdapter(
    private val roomJpaRepository: TbRoomRepository,
    private val mapper: RoomMapper
) : LoadRoomPort, SaveRoomPort, DeleteRoomPort {

    override fun loadRoom(id: RoomId): Room {
        return roomJpaRepository.findById(id.value)
            .map { mapper.toDomain(it) }
            .orElseThrow { throw IllegalStateException("회의실을 찾을 수 없습니다: ${id.value}") }
    }

    override fun loadRooms(query: RoomQuery): List<Room> {
        val entities = roomJpaRepository.searchRooms(
            buildingName = query.buildingName?.value ,
            floor = query.floor?.value,
            status = query.status,
            minCapacity = query.minCapacity
        )
        return mapper.toDomainList(entities)
    }

    override fun loadAvailableRooms(query: AvailableRoomQuery): List<Room> {
        // 건물 ID 필터링이 필요한 경우
        val entities = if (query.buildingName != null) {
            val buildingRooms = roomJpaRepository.findByBuildingName(query.buildingName.value)
            val availableRooms = roomJpaRepository.findAvailableRooms(
                date = query.date,
                startTime = query.startTime,
                endTime = query.endTime,
                minCapacity = query.minCapacity
            )

            availableRooms.filter { availableRoom ->
                buildingRooms.any { it.id == availableRoom.id }
            }
        } else {
            roomJpaRepository.findAvailableRooms(
                date = query.date,
                startTime = query.startTime,
                endTime = query.endTime,
                minCapacity = query.minCapacity
            )
        }

        return mapper.toDomainList(entities)
    }

    override fun saveRoom(room: Room): Room {
        val entity = if (room.id != null) {
            // 기존 엔티티 업데이트
            val existingEntity = roomJpaRepository.findById(room.id.value)
                .orElseThrow { IllegalArgumentException("회의실을 찾을 수 없습니다: ${room.id.value}") }
            mapper.updateEntity(existingEntity, room)
            existingEntity
        } else {
            // 새 엔티티 생성
            mapper.toEntity(room)
        }

        val savedEntity = roomJpaRepository.save(entity)
        return mapper.toDomain(savedEntity)
    }

    override fun deleteRoom(id: RoomId) {
        roomJpaRepository.deleteById(id.value)
    }

    override fun existsRoom(id: RoomId): Boolean {
        return roomJpaRepository.existsById(id.value)
    }
}