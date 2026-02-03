package com.devpaik.metting.application.reservation.usecase.command

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import java.time.LocalDate
import java.time.LocalTime

data class CreateReservationCommand(
    val roomId: RoomId,
    val userEmail: UserEmail,
    val title: String,
    val contents: String?,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val participants: List<ParticipantInfo> = emptyList()
)