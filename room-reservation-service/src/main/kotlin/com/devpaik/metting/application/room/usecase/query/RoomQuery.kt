package com.devpaik.metting.application.room.usecase.query

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus

data class RoomQuery(
    val buildingName: BuildingName? = null,
    val roomName: RoomName? = null,
    val floor: Floor? = null,
    val minCapacity: Int? = null,
    val status: RoomStatus? = null
)

