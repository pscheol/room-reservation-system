package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.exception.ReservationDomainException
import java.time.LocalDate
import java.time.LocalTime

/**
 * 예약 수정을 위한 도메인 요청 객체
 */
data class ReservationUpdateRequest(
    val title: String,
    val contents: String?,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val status: ReservationStatus
)

/**
 * 예약 수정을 전문으로하는 팩토리 도메인 서비스
 *
 * 역할: 예약 수정과 관련된 핵심 비즈니스 규칙과 절차를 캡슐화.
 */
class ReservationUpdater(
    private val reservationConflictValidator: ReservationConflictValidator
) {
    fun update(
        reservation: Reservation,
        request: ReservationUpdateRequest,
        existingReservations: List<Reservation>
    ): Reservation {
        // 1. 시간이 변경된 경우 충돌 검증
        val isTimeChanged = reservation.reservationDate != request.reservationDate ||
                reservation.period.startTime != request.startTime ||
                reservation.period.endTime != request.endTime

        if (isTimeChanged) {
            validateNoTimeConflict(reservation, request, existingReservations)
        }

        // 2. 도메인 모델의 update 메서드 호출
        val updatedReservation = reservation.update(
            title = request.title,
            contents = request.contents,
            reservationDate = request.reservationDate,
            startTime = request.startTime,
            endTime = request.endTime
        )

        // 3. 상태 변경 처리
        return if (request.status != updatedReservation.status) {
            applyStatusChange(updatedReservation, request.status)
        } else {
            updatedReservation
        }
    }

    private fun validateNoTimeConflict(
        reservation: Reservation,
        request: ReservationUpdateRequest,
        existingReservations: List<Reservation>
    ) {
        // 임시 예약 객체 생성하여 충돌 검증
        val tempReservation = Reservation.create(
            roomId = reservation.roomId,
            userEmail = reservation.userEmail,
            title = request.title,
            contents = request.contents,
            reservationDate = request.reservationDate,
            startTime = request.startTime,
            endTime = request.endTime
        )

        val conflictValidation = reservationConflictValidator.validateNoConflict(
            existingReservations = existingReservations,
            newReservation = tempReservation
        )

        if (conflictValidation.isFailure()) {
            throw ReservationDomainException(
                conflictValidation.getFailureReason() ?: "시간이 충돌합니다"
            )
        }
    }

    private fun applyStatusChange(
        reservation: Reservation,
        newStatus: ReservationStatus
    ): Reservation {
        return when (newStatus) {
            ReservationStatus.COMPLETED -> reservation.complete()
            ReservationStatus.CANCELLED ->
                throw ReservationDomainException("잘못된 요청 입니다.")
            else -> reservation
        }
    }
}