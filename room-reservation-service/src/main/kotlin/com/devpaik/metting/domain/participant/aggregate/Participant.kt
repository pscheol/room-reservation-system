package com.devpaik.metting.domain.participant.aggregate

import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import java.time.LocalDateTime

/**
 * 참여자 Aggregate Root
 */
data class Participant(
    val id: ParticipantId?,
    val participantEmail: ParticipantEmail,
    val participantType: ParticipantType,
    val invitedAt: LocalDateTime,
) {
    companion object {
        fun create(
            email: String,
            type: ParticipantType
        ): Participant {
            return Participant(
                id = null,
                participantEmail = ParticipantEmail(email),
                participantType = type,
                invitedAt = LocalDateTime.now()
            )
        }

        fun reconstitute(
            id: ParticipantId,
            email: String,
            type: ParticipantType,
            invitedAt: LocalDateTime,
        ): Participant {
            return Participant(
                id = id,
                participantEmail = ParticipantEmail(email),
                participantType = type,
                invitedAt = invitedAt
            )
        }
    }
}