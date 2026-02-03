package com.devpaik.metting.adapter.secondary.persistence.mapper

import com.devpaik.metting.adapter.secondary.persistence.entity.TbParticipant
import com.devpaik.metting.adapter.secondary.persistence.entity.TbReservation
import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import org.springframework.stereotype.Component
import java.time.LocalDateTime

/**
 * 예약 매퍼
 */
@Component
class ReservationMapper {

    /**
     * JPA 엔티티 -> 도메인 모델 변환
     */
    fun toDomain(entity: TbReservation): Reservation {
        return Reservation.reconstitute(
            id = ReservationId(entity.id!!),
            roomId = RoomId(entity.room.id!!),
            userEmail = UserEmail(entity.userEmail),
            title = entity.title,
            contents = entity.contents,
            reservationDate = entity.reservationDate,
            startTime = entity.startTime,
            endTime = entity.endTime,
            status = entity.status,
            googleCalendarEventId = entity.googleCalendarEventId,
            createdAt = entity.createdAt?: LocalDateTime.now(),
            updatedAt = entity.updatedAt?: LocalDateTime.now() ,
            cancelledAt = entity.cancelledAt,
            participants = entity.participants.map { toParticipantDomain(it) }
        )
    }

    /**
     * 도메인 모델 -> JPA 엔티티 변환
     */
    fun toEntity(domain: Reservation, room: TbRoom): TbReservation {
        val entity = TbReservation(
            id = domain.id?.value,
            room = room,
            userEmail = domain.userEmail.value,
            title = domain.title.value,
            contents = domain.contents,
            reservationDate = domain.reservationDate,
            startTime = domain.period.startTime,
            endTime = domain.period.endTime,
            status = domain.status,
            googleCalendarEventId = domain.googleCalendarEventId,
            cancelledAt = domain.cancelledAt
        )

        // 참여자 추가
        domain.participants.forEach { participant ->
            val participantEntity = toParticipantEntity(participant, entity)
            entity.participants.add(participantEntity)
        }

        return entity
    }


    fun updateEntity(entity: TbReservation, domain: Reservation) {
        entity.apply {
            title = domain.title.value
            contents = domain.contents
            reservationDate = domain.reservationDate
            startTime = domain.period.startTime
            endTime = domain.period.endTime
            status = domain.status
            googleCalendarEventId = domain.googleCalendarEventId
            cancelledAt = domain.cancelledAt
        }

        // 참여자 동기화
        syncParticipants(entity, domain.participants)
    }

    /**
     * 참여자 동기화
     */
    private fun syncParticipants(entity: TbReservation, domainParticipants: List<Participant>) {
        // 기존 참여자 제거
        entity.participants.clear()

        // 새로운 참여자 추가
        domainParticipants.forEach { participant ->
            val participantEntity = toParticipantEntity(participant, entity)
            entity.participants.add(participantEntity)
        }
    }

    /**
     * 참여자 JPA 엔티티 -> 도메인 모델 변환
     */
    private fun toParticipantDomain(entity: TbParticipant): Participant {
        return Participant.reconstitute(
            id = ParticipantId(entity.id!!),
            email = entity.participantEmail,
            type = entity.participantType,
            invitedAt = entity.invitedAt
        )
    }

    /**
     * 참여자 도메인 모델 -> JPA 엔티티 변환
     */
    private fun toParticipantEntity(domain: Participant, reservation: TbReservation): TbParticipant {
        return TbParticipant(
            id = domain.id?.value,
            reservation = reservation,
            participantEmail = domain.participantEmail.value,
            participantType = domain.participantType,
            invitedAt = domain.invitedAt
        )
    }
}