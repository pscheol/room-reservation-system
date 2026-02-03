package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.output.SaveReservationPort
import com.devpaik.metting.application.reservation.usecase.UpdateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.command.CancelReservationCommand
import com.devpaik.metting.application.reservation.usecase.command.UpdateReservationCommand
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.service.ReservationUpdateRequest
import com.devpaik.metting.domain.reservation.service.ReservationUpdater
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 예약 수정/취소 애플리케이션 서비스
 */
@Service
@Transactional
class UpdateReservationService(
    private val loadReservationPort: LoadReservationPort,
    private val saveReservationPort: SaveReservationPort,
    private val reservationUpdater: ReservationUpdater
) : UpdateReservationUseCase {

    override fun updateReservation(command: UpdateReservationCommand): Reservation {
        // 1. 인프라를 통해 필요한 데이터 조회
        val reservation = loadReservationPort.loadReservation(command.reservationId)

        val existingReservations = loadReservationPort.loadConflictingReservations(
            roomId = reservation.roomId,
            date = command.reservationDate,
            startTime = command.startTime,
            endTime = command.endTime,
            excludeReservationId = command.reservationId
        )

        // 2. 애플리케이션 Command를 도메인 Request로 변환
        val updateRequest = command.toDomainRequest()

        // 3. 도메인 서비스에 핵심 비즈니스 로직 실행 위임
        val updatedReservation = reservationUpdater.update(
            reservation = reservation,
            request = updateRequest,
            existingReservations = existingReservations
        )

        // 4. 인프라를 통해 최종 결과 저장
        return saveReservationPort.saveReservation(updatedReservation)
    }

    override fun cancelReservation(command: CancelReservationCommand): Reservation {
        // 1. 예약 조회
        val reservation = loadReservationPort.loadReservation(command.reservationId)
            ?: throw IllegalArgumentException("예약을 찾을 수 없습니다: ${command.reservationId.value}")

        // 2. 요청자 이메일이 예약자(ORGANIZER) 이메일과 일치하는지 확인
        val organizer = reservation.participants.find { it.participantType == ParticipantType.ORGANIZER }
        require(organizer != null && organizer.participantEmail.value == command.userEmail.value) {
            "예약을 취소할 권한이 없습니다."
        }

        // 3. 도메인 모델의 cancel 메서드 호출
        val cancelledReservation = reservation.cancel(reservation.userEmail)

        // 4. 저장
        return saveReservationPort.saveReservation(cancelledReservation)
    }

    private fun UpdateReservationCommand.toDomainRequest() = ReservationUpdateRequest(
        title = this.title,
        contents = this.contents,
        reservationDate = this.reservationDate,
        startTime = this.startTime,
        endTime = this.endTime,
        status = this.status
    )
}