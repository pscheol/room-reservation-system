package com.devpaik.metting.application.reservation.usecase.query

import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import java.time.LocalDate

data class ReservationQuery(
    val userEmail: String? = null,
    val roomName: String? = null,
    val buildingName: String? = null,
    val startDate: LocalDate? = null,
    val endDate: LocalDate? = null,
    val status: ReservationStatus? = null
)
