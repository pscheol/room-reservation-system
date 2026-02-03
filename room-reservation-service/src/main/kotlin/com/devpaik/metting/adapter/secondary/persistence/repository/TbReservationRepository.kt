package com.devpaik.metting.adapter.secondary.persistence.repository

import com.devpaik.metting.adapter.secondary.persistence.entity.TbReservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import org.springframework.stereotype.Repository
import java.time.LocalDate
import java.time.LocalTime
import java.util.*

@Repository
interface TbReservationRepository : JpaRepository<TbReservation, Long> {

    @Query("""
        SELECT r FROM TbReservation r
        JOIN FETCH r.room rm
        LEFT JOIN FETCH r.participants
        WHERE r.id = :reservationId
    """)
    fun searchReservationById(
        @Param("reservationId") reservationId: Long,
    ): Optional<TbReservation>

    @Query("""
        SELECT r FROM TbReservation r
        JOIN FETCH r.room rm
        LEFT JOIN FETCH r.participants p
        WHERE (:userEmail IS NULL OR (p.participantEmail = :userEmail AND p.participantType = 'ORGANIZER'))
        AND (:roomName IS NULL OR rm.roomName = :roomName)
        AND (:buildingName IS NULL OR rm.buildingName = :buildingName)
        AND (:startDate IS NULL OR r.reservationDate >= :startDate)
        AND (:endDate IS NULL OR r.reservationDate <= :endDate)
        AND (:status IS NULL OR r.status = :status)
        ORDER BY r.reservationDate DESC, r.startTime ASC
    """)
    fun searchReservations(
        @Param("userEmail") userEmail: String?,
        @Param("roomName") roomName: String?,
        @Param("buildingName") buildingName: String?,
        @Param("startDate") startDate: LocalDate?,
        @Param("endDate") endDate: LocalDate?,
        @Param("status") status: ReservationStatus?
    ): List<TbReservation>

    @Query("""
        SELECT r FROM TbReservation r
        JOIN FETCH r.room rm
        LEFT JOIN FETCH r.participants
        WHERE r.room.id = :roomId
        AND r.reservationDate = :date
        AND r.status <> 'CANCELLED'
        AND r.startTime < :endTime
        AND r.endTime > :startTime
        AND (:excludeId IS NULL OR r.id <> :excludeId)
        ORDER BY r.startTime ASC
    """)
    fun findConflictingReservations(
        @Param("roomId") roomId: Long,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime,
        @Param("excludeId") excludeId: Long?
    ): List<TbReservation>

    @Query("""
        SELECT r FROM TbReservation r
        JOIN FETCH r.room rm
        LEFT JOIN FETCH r.participants p
        WHERE p.participantEmail = :email
        AND p.participantType = 'ORGANIZER'
        AND r.reservationDate >= :fromDate
        AND r.status IN ('CONFIRMED', 'PENDING')
        ORDER BY r.reservationDate ASC, r.startTime ASC
    """)
    fun findUpcomingReservations(
        @Param("email") email: String,
        @Param("fromDate") fromDate: LocalDate
    ): List<TbReservation>

    @Query("""
        SELECT CASE WHEN COUNT(r) > 0 THEN true ELSE false END
        FROM TbReservation r
        WHERE r.room.id = :roomId
        AND r.reservationDate = :date
        AND r.status <> 'CANCELLED'
        AND r.startTime < :endTime
        AND r.endTime > :startTime
        AND (:excludeId IS NULL OR r.id <> :excludeId)
    """)
    fun existsConflict(
        @Param("roomId") roomId: Long,
        @Param("date") date: LocalDate,
        @Param("startTime") startTime: LocalTime,
        @Param("endTime") endTime: LocalTime,
        @Param("excludeId") excludeId: Long?
    ): Boolean
}