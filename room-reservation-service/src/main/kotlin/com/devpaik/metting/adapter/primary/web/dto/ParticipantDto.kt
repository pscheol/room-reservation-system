package com.devpaik.metting.adapter.primary.web.dto

import com.devpaik.metting.application.reservation.usecase.command.ParticipantInfo
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "참여자 요청")
data class ParticipantRequest(
    @Schema(description = "참여자 이메일", example = "user@example.com", required = true)
    val email: String,

    @Schema(description = "참여자 유형", example = "ATTENDEE", required = true)
    val type: ParticipantType
) {
    fun toInfo(): ParticipantInfo {
        return ParticipantInfo(
            email = email,
            type = type
        )
    }
}

@Schema(description = "참여자 응답")
data class ParticipantResponse(
    @Schema(description = "참여자 ID", example = "1")
    val id: Long,

    @Schema(description = "참여자 이메일", example = "user@example.com")
    val email: String,

    @Schema(description = "참여자 유형", example = "ATTENDEE")
    val type: ParticipantType,

    @Schema(description = "초대 일시", example = "2026-01-20T10:00:00")
    val invitedAt: LocalDateTime,
) {
    companion object {
        fun from(participant: Participant): ParticipantResponse {
            return ParticipantResponse(
                id = participant.id!!.value,
                email = participant.participantEmail.value,
                type = participant.participantType,
                invitedAt = participant.invitedAt
            )
        }
    }
}