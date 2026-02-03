package com.devpaik.metting.adapter.secondary.persistence.adapter

import com.devpaik.metting.adapter.secondary.persistence.mapper.ReservationMapper
import com.devpaik.metting.adapter.secondary.persistence.repository.TbReservationRepository
import com.devpaik.metting.adapter.secondary.persistence.repository.TbRoomRepository
import com.devpaik.metting.application.reservation.output.CheckReservationPort
import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.output.SaveReservationPort
import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import org.springframework.data.repository.findByIdOrNull
import org.springframework.stereotype.Component
import java.time.LocalDate
import java.time.LocalTime

@Component
class TbReservationAdapter(
    private val tbReservationRepository: TbReservationRepository,
    private val tbRoomRepository: TbRoomRepository,
    private val reservationMapper: ReservationMapper
) : LoadReservationPort, SaveReservationPort, CheckReservationPort {

    override fun loadReservation(id: ReservationId): Reservation {
        return tbReservationRepository.searchReservationById(id.value)
            .map { reservationMapper.toDomain(it) }
            .orElseThrow { throw IllegalStateException("예약을 찾을 수 없습니다: ${id.value}") }
    }

    override fun loadReservations(query: ReservationQuery): List<Reservation> {
        return tbReservationRepository.searchReservations(
            userEmail = query.userEmail,
            roomName = query.roomName,
            buildingName = query.buildingName,
            startDate = query.startDate,
            endDate = query.endDate,
            status = query.status
        ).map { reservationMapper.toDomain(it) }
    }

    override fun loadUpcomingReservations(email: String, fromDate: LocalDate): List<Reservation> {
        return tbReservationRepository.findUpcomingReservations(email, fromDate)
            .map { reservationMapper.toDomain(it) }
    }

    override fun loadConflictingReservations(
        roomId: RoomId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeReservationId: ReservationId?
    ): List<Reservation> {
        return tbReservationRepository.findConflictingReservations(
            roomId.value,
            date,
            startTime,
            endTime,
            excludeReservationId?.value
        ).map { reservationMapper.toDomain(it) }
    }

    override fun saveReservation(reservation: Reservation): Reservation {
        val entity = if (reservation.id != null) {
            val existingEntity = tbReservationRepository.findById(reservation.id.value)
                .orElseThrow { IllegalStateException("예약을 찾을 수 없습니다") }
            reservationMapper.updateEntity(existingEntity, reservation)
            existingEntity
        } else {
            val room = tbRoomRepository.findByIdOrNull(reservation.roomId.value)
                ?: throw IllegalStateException("회의실을 찾을 수 없습니다: ${reservation.roomId.value}")
            reservationMapper.toEntity(reservation, room)
        }

        val savedEntity = tbReservationRepository.save(entity)
        return reservationMapper.toDomain(savedEntity)
    }

    override fun existsConflict(
        roomId: RoomId,
        date: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime,
        excludeReservationId: ReservationId?
    ): Boolean {
        return tbReservationRepository.existsConflict(
            roomId.value,
            date,
            startTime,
            endTime,
            excludeReservationId?.value
        )
    }
}