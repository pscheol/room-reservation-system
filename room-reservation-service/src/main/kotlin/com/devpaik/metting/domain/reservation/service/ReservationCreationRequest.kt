package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import java.time.LocalDate
import java.time.LocalTime

/**
 * 예약 생성을 위한 도메인 요청 객체
 */
data class ReservationCreationRequest(
    val roomId: RoomId,
    val userEmail: UserEmail,
    val title: String,
    val contents: String?,
    val reservationDate: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val participants: List<ParticipantCreationInfo> = emptyList()
)

data class ParticipantCreationInfo(
    val email: String,
    val type: ParticipantType
)