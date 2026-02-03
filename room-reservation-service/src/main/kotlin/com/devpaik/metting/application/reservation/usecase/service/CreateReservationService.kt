package com.devpaik.metting.application.reservation.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.reservation.output.SaveReservationPort
import com.devpaik.metting.application.reservation.usecase.CreateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.command.CreateReservationCommand
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.service.ParticipantCreationInfo
import com.devpaik.metting.domain.reservation.service.ReservationCreationRequest
import com.devpaik.metting.domain.reservation.service.ReservationFactory
import com.devpaik.metting.domain.room.aggregate.Room
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 예약 생성 애플리케이션 서비스
 */
@Service
@Transactional
class CreateReservationService(
    private val loadRoomPort: LoadRoomPort,
    private val loadReservationPort: LoadReservationPort,
    private val saveReservationPort: SaveReservationPort,
    private val reservationFactory: ReservationFactory
) : CreateReservationUseCase {

    override fun createReservation(command: CreateReservationCommand): Pair<Reservation, Room> {
        // 1. 인프라를 통해 필요한 데이터 조회
        val room = loadRoomPort.loadRoom(command.roomId)

        val existingReservations = loadReservationPort.loadConflictingReservations(
            roomId = command.roomId,
            date = command.reservationDate,
            startTime = command.startTime,
            endTime = command.endTime
        )

        // 2. 애플리케이션 Command를 도메인 Request로 변환
        val creationRequest = command.toDomainRequest()

        // 3. 도메인 서비스에 핵심 비즈니스 로직 실행 위임
        val reservation = reservationFactory.create(
            request = creationRequest,
            room = room,
            existingReservations = existingReservations
        )

        // 4. 인프라를 통해 최종 결과 저장
        return Pair(saveReservationPort.saveReservation(reservation), room)
    }

    private fun CreateReservationCommand.toDomainRequest(): ReservationCreationRequest {
        val organizerParticipant = ParticipantCreationInfo(
            email = this.userEmail.value,
            type = ParticipantType.ORGANIZER
        )

        val allParticipants = mutableListOf(organizerParticipant)
        this.participants.forEach { participantInfo ->
            // 중복 방지를 위해, 이미 organizer로 추가된 이메일은 다시 추가하지 않음
            if (participantInfo.email != organizerParticipant.email) {
                allParticipants.add(
                    ParticipantCreationInfo(
                        email = participantInfo.email,
                        type = participantInfo.type
                    )
                )
            }
        }

        return ReservationCreationRequest(
            roomId = this.roomId,
            userEmail = this.userEmail,
            title = this.title,
            contents = this.contents,
            reservationDate = this.reservationDate,
            startTime = this.startTime,
            endTime = this.endTime,
            participants = allParticipants
        )
    }
}