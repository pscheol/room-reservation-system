package com.devpaik.metting.adapter.secondary.persistence.mapper


import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import org.springframework.stereotype.Component

/**
 * 회의실 매퍼
 */
@Component
class RoomMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: TbRoom): Room {
        return Room.reconstitute(
            id = RoomId(entity.id!!),
            buildingName = BuildingName(entity.buildingName),
            floor = Floor(entity.floor),
            roomName = entity.roomName,
            capacity = entity.capacity,
            contents = entity.contents,
            status = entity.roomStatus,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: Room): TbRoom {
        return TbRoom(
            id = domain.id?.value,
            buildingName = domain.buildingName.value,
            floor = domain.floor.value,
            roomName = domain.roomName.value,
            capacity = domain.capacity.value,
            contents = domain.contents,
            roomStatus = domain.status,
        )
    }

    /**
     * 도메인 모델로 기존 JPA 엔티티 업데이트
     * 불변 도메인 모델의 값으로 가변 JPA 엔티티 업데이트
     */
    fun updateEntity(entity: TbRoom, domain: Room) {
        entity.apply {
            buildingName = domain.buildingName.value
            roomName = domain.roomName.value
            capacity = domain.capacity.value
            contents = domain.contents
            roomStatus = domain.status
        }
    }

    /**
     * 여러 엔티티를 도메인 모델 리스트로 변환
     */
    fun toDomainList(entities: List<TbRoom>): List<Room> {
        return entities.map { toDomain(it) }
    }
}