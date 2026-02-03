package com.devpaik.metting.domain.reservation.aggregate

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

/**
 * 예약 Aggregate Root
 */
data class Reservation(
    val id: ReservationId?,
    val roomId: RoomId,
    val userEmail: UserEmail,
    val title: ReservationTitle,
    val contents: String?,
    val reservationDate: LocalDate,
    val period: ReservationPeriod,
    val status: ReservationStatus,
    val googleCalendarEventId: String?,
    val createdAt: LocalDateTime,
    val updatedAt: LocalDateTime,
    val cancelledAt: LocalDateTime?,
    val participants: List<Participant>
) {
    companion object {
        fun create(
            roomId: RoomId,
            userEmail: UserEmail,
            title: String,
            contents: String?,
            reservationDate: LocalDate,
            startTime: LocalTime,
            endTime: LocalTime,
            participants: List<Participant> = emptyList()
        ): Reservation {
            validateReservationDate(reservationDate)

            return Reservation(
                id = null,
                roomId = roomId,
                userEmail = userEmail,
                title = ReservationTitle(title),
                contents = contents,
                reservationDate = reservationDate,
                period = ReservationPeriod(startTime, endTime),
                status = ReservationStatus.CONFIRMED,
                googleCalendarEventId = null,
                createdAt = LocalDateTime.now(),
                updatedAt = LocalDateTime.now(),
                cancelledAt = null,
                participants = participants
            )
        }

        fun reconstitute(
            id: ReservationId,
            roomId: RoomId,
            userEmail: UserEmail,
            title: String,
            contents: String?,
            reservationDate: LocalDate,
            startTime: LocalTime,
            endTime: LocalTime,
            status: ReservationStatus,
            googleCalendarEventId: String?,
            createdAt: LocalDateTime,
            updatedAt: LocalDateTime,
            cancelledAt: LocalDateTime?,
            participants: List<Participant>
        ): Reservation {
            return Reservation(
                id = id,
                roomId = roomId,
                userEmail = userEmail,
                title = ReservationTitle(title),
                contents = contents,
                reservationDate = reservationDate,
                period = ReservationPeriod(startTime, endTime),
                status = status,
                googleCalendarEventId = googleCalendarEventId,
                createdAt = createdAt,
                updatedAt = updatedAt,
                cancelledAt = cancelledAt,
                participants = participants
            )
        }

        private fun validateReservationDate(date: LocalDate) {
            val nowDate = LocalDate.now()
            require(!date.isBefore(nowDate)) { "과거 날짜로 예약할 수 없습니다" }
            require(!date.isAfter(nowDate.plusMonths(3))) { "3개월 이후로는 예약할 수 없습니다" }
        }
    }

    fun cancel(requestUserEmail: UserEmail): Reservation {
        require(status != ReservationStatus.CANCELLED) { "이미 취소된 예약입니다" }
        require(status != ReservationStatus.COMPLETED) { "완료된 예약은 취소할 수 없습니다" }
        require(userEmail == requestUserEmail) { "예약자만 예약을 취소할 수 있습니다" }

        val now = LocalDateTime.now()
        val reservationStartDateTime = LocalDateTime.of(reservationDate, period.startTime)
        require(now.plusMinutes(10).isBefore(reservationStartDateTime)) {
            "예약 시작 10분 전까지만 취소할 수 있습니다"
        }

        return copy(
            status = ReservationStatus.CANCELLED,
            cancelledAt = now,
            updatedAt = now
        )
    }

    fun update(
        title: String,
        contents: String?,
        reservationDate: LocalDate,
        startTime: LocalTime,
        endTime: LocalTime
    ): Reservation {
        require(canBeModified()) { "취소되었거나 완료된 예약은 수정할 수 없습니다" }
        Companion.validateReservationDate(reservationDate)

        return copy(
            title = ReservationTitle(title),
            contents = contents,
            reservationDate = reservationDate,
            period = ReservationPeriod(startTime, endTime),
            updatedAt = LocalDateTime.now()
        )
    }

    fun complete(): Reservation {
        require(status == ReservationStatus.CONFIRMED) { "확정된 예약만 완료 처리할 수 있습니다" }

        val now = LocalDateTime.now()
        val reservationEndDateTime = LocalDateTime.of(reservationDate, period.endTime)
        require(now.isAfter(reservationEndDateTime)) { "예약 종료 시간이 지나야 완료 처리할 수 있습니다" }

        return copy(
            status = ReservationStatus.COMPLETED,
            updatedAt = now
        )
    }

    fun addParticipant(participant: Participant): Reservation {
        require(status != ReservationStatus.CANCELLED) { "취소된 예약에는 참여자를 추가할 수 없습니다" }
        require(participants.none { it.participantEmail == participant.participantEmail }) {
            "이미 등록된 참여자입니다: ${participant.participantEmail}"
        }

        return copy(
            participants = participants + participant,
            updatedAt = LocalDateTime.now()
        )
    }

    fun removeParticipant(participantEmail: String): Reservation {
        require(status != ReservationStatus.CANCELLED) { "취소된 예약의 참여자는 제거할 수 없습니다" }

        val newParticipants = participants.filter { it.participantEmail.value != participantEmail }
        require(newParticipants.size < participants.size) { "참여자를 찾을 수 없습니다: $participantEmail" }

        return copy(
            participants = newParticipants,
            updatedAt = LocalDateTime.now()
        )
    }

    fun withGoogleCalendarEventId(eventId: String): Reservation {
        return copy(
            googleCalendarEventId = eventId,
            updatedAt = LocalDateTime.now()
        )
    }

    fun isConflictWith(other: Reservation): Boolean {
        if (this.reservationDate != other.reservationDate) return false
        if (this.roomId != other.roomId) return false
        if (this.status == ReservationStatus.CANCELLED || other.status == ReservationStatus.CANCELLED) return false

        return this.period.startTime < other.period.endTime && this.period.endTime > other.period.startTime
    }

    fun minutesUntilStart(): Long {
        val now = LocalDateTime.now()
        val startDateTime = LocalDateTime.of(reservationDate, period.startTime)
        return Duration.between(now, startDateTime).toMinutes()
    }

    fun canBeModified(): Boolean {
        return status == ReservationStatus.CONFIRMED || status == ReservationStatus.PENDING
    }
}