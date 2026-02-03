package com.devpaik.metting.adapter.primary.web.dto

import com.devpaik.metting.application.reservation.usecase.command.CreateReservationCommand
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.room.aggregate.Room
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Schema(description = "예약 생성 요청")
data class CreateReservationRequest(
    @Schema(description = "회의실 ID", example = "1", required = true)
    val roomId: Long,

    @Schema(description = "예약자 ID", example = "hello@gmail.com", required = true)
    val email: String,

    @Schema(description = "예약 제목", example = "프로젝트 킥오프 회의", required = true)
    val title: String,

    @Schema(description = "예약 내용", example = "2026년 신규 프로젝트 킥오프 미팅")
    val contents: String?,

    @Schema(description = "예약 날짜", example = "2026-01-27", required = true)
    val reservationDate: LocalDate,

    @Schema(description = "시작 시간", example = "14:00", required = true)
    val startTime: LocalTime,

    @Schema(description = "종료 시간", example = "16:00", required = true)
    val endTime: LocalTime,

    @Schema(description = "참여자 목록")
    val participants: List<ParticipantRequest> = emptyList()
) {
    fun toCommand(): CreateReservationCommand {
        return CreateReservationCommand(
            roomId = RoomId(roomId),
            userEmail = UserEmail(email),
            title = title,
            contents = contents,
            reservationDate = reservationDate,
            startTime = startTime,
            endTime = endTime,
            participants = participants.map { it.toInfo() }
        )
    }
}

@Schema(description = "예약 수정 요청")
data class UpdateReservationRequest(
    @Schema(description = "예약 제목", example = "프로젝트 리뷰 회의", required = true)
    val title: String,

    @Schema(description = "예약 내용", example = "1차 스프린트 리뷰")
    val contents: String?,

    @Schema(description = "예약 날짜", example = "2026-01-31", required = true)
    val reservationDate: LocalDate,

    @Schema(description = "시작 시간", example = "15:00", required = true)
    val startTime: LocalTime,

    @Schema(description = "종료 시간", example = "17:00", required = true)
    val endTime: LocalTime,

    @Schema(description = "예약 상태", example = "CONFIRMED", required = true)
    val status: ReservationStatus
)

@Schema(description = "회의실 정보 응답")
data class RoomVO(
    @Schema(description = "회의실 ID", example = "1")
    val id: Long,
    @Schema(description = "건물명", example = "본관")
    val buildingName: String,
    @Schema(description = "층 수", example = "3")
    val floor: Long,
    @Schema(description = "회의실명", example = "대회의실")
    val roomName: String
) {
    companion object {
        fun from(room: Room): RoomVO {
            return RoomVO(
                id = room.id!!.value,
                buildingName = room.buildingName.value,
                floor = room.floor.value,
                roomName = room.roomName.value
            )
        }
    }
}

@Schema(description = "예약 응답")
data class ReservationResponse(
    @Schema(description = "예약 ID", example = "1")
    val id: Long,

    @Schema(description = "회의실 정보")
    val room: RoomVO,

    @Schema(description = "예약자 이메일", example = "hello@gmail.com")
    val userEmail: String,

    @Schema(description = "예약 제목", example = "프로젝트 킥오프 회의")
    val title: String,

    @Schema(description = "예약 내용", example = "2026년 신규 프로젝트 킥오프 미팅")
    val contents: String?,

    @Schema(description = "예약 날짜", example = "2026-01-31")
    val reservationDate: LocalDate,

    @Schema(description = "시작 시간", example = "14:00")
    val startTime: LocalTime,

    @Schema(description = "종료 시간", example = "16:00")
    val endTime: LocalTime,

    @Schema(description = "예약 상태", example = "CONFIRMED")
    val status: ReservationStatus,

    @Schema(description = "구글 캘린더 이벤트 ID")
    val googleCalendarEventId: String?,

    @Schema(description = "참여자 목록")
    val participants: List<ParticipantResponse>,

    @Schema(description = "생성 일시", example = "2026-01-31T10:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정 일시", example = "2026-01-31T10:00:00")
    val updatedAt: LocalDateTime?,

    @Schema(description = "취소 일시", example = "2026-01-31T15:00:00")
    val cancelledAt: LocalDateTime?
) {
    companion object {
        fun from(reservation: Reservation, room: Room): ReservationResponse {
            val organizerEmail = reservation.participants
                .find { it.participantType == ParticipantType.ORGANIZER }
                ?.participantEmail?.value ?: "unknown"

            return ReservationResponse(
                id = reservation.id!!.value,
                room = RoomVO.from(room),
                userEmail = organizerEmail,
                title = reservation.title.value,
                contents = reservation.contents,
                reservationDate = reservation.reservationDate,
                startTime = reservation.period.startTime,
                endTime = reservation.period.endTime,
                status = reservation.status,
                googleCalendarEventId = reservation.googleCalendarEventId,
                participants = reservation.participants.map { ParticipantResponse.from(it) },
                createdAt = reservation.createdAt,
                updatedAt = reservation.updatedAt,
                cancelledAt = reservation.cancelledAt
            )
        }
    }
}