package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.exception.ReservationDomainException
import com.devpaik.metting.domain.room.aggregate.Room

/**
 * 예약 생성 팩토리 도메인 서비스
 * 예약 생성과 관련된 핵심 비즈니스 로직
 */
class ReservationFactory(
    private val reservationAvailabilityValidator: ReservationAvailabilityValidator,
    private val reservationConflictValidator: ReservationConflictValidator
) {

    fun create(
        request: ReservationCreationRequest,
        room: Room,
        existingReservations: List<Reservation>
    ): Reservation {
        // 1. 도메인 서비스를 통한 예약 가능 여부 검증
        val requiredCapacity = requiredCapacity(request.participants.size) // 예약자 포함

        val validationContext = ValidationContext(
            room = room,
            date = request.reservationDate,
            startTime = request.startTime,
            endTime = request.endTime,
            requiredCapacity = requiredCapacity
        )

        val availabilityValidation = reservationAvailabilityValidator.validateAvailability(validationContext)

        if (availabilityValidation.isFailure()) {
            throw ReservationDomainException(
                availabilityValidation.getFailureReason() ?: "예약이 불가능합니다"
            )
        }

        // 2. 참여자 생성
        val participants = request.participants.map { participantInfo ->
            Participant.create(
                email = participantInfo.email,
                type = participantInfo.type
            )
        }


        // 3. 예약 생성
        val reservation = Reservation.create(
            roomId = request.roomId,
            userEmail = request.userEmail,
            title = request.title,
            contents = request.contents,
            reservationDate = request.reservationDate,
            startTime = request.startTime,
            endTime = request.endTime,
            participants = participants
        )

        // 4. 도메인 서비스를 통한 시간 충돌 검증
        val conflictValidation = reservationConflictValidator.validateNoConflict(
            existingReservations = existingReservations,
            newReservation = reservation
        )

        if (conflictValidation.isFailure()) {
            throw ReservationDomainException(
                conflictValidation.getFailureReason() ?: "시간이 충돌합니다"
            )
        }

        // 5. 검증이 완료된 예약 반환
        return reservation
    }


    private fun requiredCapacity(participantsSize: Int): Int = participantsSize + 1 // 예약자 포함
}