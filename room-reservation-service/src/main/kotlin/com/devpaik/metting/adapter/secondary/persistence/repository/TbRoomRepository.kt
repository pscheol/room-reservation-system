package com.devpaik.metting.adapter.secondary.persistence.repository


import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime

@Repository
interface TbRoomRepository : JpaRepository<TbRoom, Long> {

    /**
     * 건물명 으로 회의실 조회
     */
    @Query("""
        SELECT r FROM TbRoom r
        WHERE r.buildingName = :buildingName
    """)
    fun findByBuildingName(@Param("buildingName") buildingName: String): List<TbRoom>

    /**
     * 특정 조건으로 회의실 검색
     */
    @Query("""
        SELECT DISTINCT r FROM TbRoom r
        WHERE (:buildingName IS NULL OR r.buildingName = :buildingName) 
        AND (:roomName IS NULL OR r.roomName = :roomName) 
        AND (:floor IS NULL OR r.floor = :floor)
        AND (:status IS NULL OR r.roomStatus = :status)
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        ORDER BY r.roomName ASC
    """)
    fun searchRooms(
        @Param(value = "buildingName") buildingName: String?,
        @Param(value = "roomName") roomName: String? = null,
        @Param("floor") floor: Long?,
        @Param("status") status: RoomStatus?,
        @Param("minCapacity") minCapacity: Int?
    ): List<TbRoom>

    /**
     * 가용 회의실 조회
     */
    @Query("""
        SELECT DISTINCT r FROM TbRoom r
        WHERE r.roomStatus = 'AVAILABLE'
        AND (:minCapacity IS NULL OR r.capacity >= :minCapacity)
        AND r.id NOT IN (
            SELECT res.room.id FROM TbReservation res
            WHERE res.reservationDate = :date
            AND res.status <> 'CANCELLED'
            AND res.startTime < :endTime
            AND res.endTime > :startTime
        )
        AND (:buildingName IS NULL OR r.buildingName = :buildingName) 
        AND (:roomName IS NULL OR r.roomName = :roomName)
        ORDER BY r.roomName ASC
    """)
    fun findAvailableRooms(
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime,
        @Param("minCapacity") minCapacity: Int?,
        @Param("buildingName") buildingName: String? = null,
        @Param("roomName") roomName: String? = null
    ): List<TbRoom>
}