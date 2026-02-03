package com.devpaik.metting.application.room.output

import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room

interface LoadRoomPort {
    fun loadRoom(id: RoomId): Room

    fun loadRooms(query: RoomQuery): List<Room>

    fun loadAvailableRooms(query: AvailableRoomQuery): List<Room>
}