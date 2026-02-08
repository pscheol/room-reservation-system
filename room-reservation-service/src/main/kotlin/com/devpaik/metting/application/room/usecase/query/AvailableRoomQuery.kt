package com.devpaik.metting.application.room.usecase.query

import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomName
import java.time.LocalDate
import java.time.LocalTime

data class AvailableRoomQuery(
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val buildingName: BuildingName? = null,
    val roomName: RoomName? = null,
    val minCapacity: Int? = null
)