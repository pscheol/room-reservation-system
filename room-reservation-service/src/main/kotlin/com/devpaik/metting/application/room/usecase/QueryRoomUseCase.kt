package com.devpaik.metting.application.room.usecase

import com.devpaik.metting.application.room.usecase.query.AvailabilityQuery
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room

interface QueryRoomUseCase {
    fun getRoom(id: RoomId): Room

    fun searchRooms(query: RoomQuery): List<Room>

    fun findAvailableRooms(query: AvailableRoomQuery): List<Room>

    fun checkAvailability(query: AvailabilityQuery): Boolean
}