package com.devpaik.metting.application.reservation.usecase.command

import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType

data class ParticipantInfo(
    val email: String,
    val type: ParticipantType
)