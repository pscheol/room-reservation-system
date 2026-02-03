package com.devpaik.metting.application.room.output

import com.devpaik.metting.domain.common.vo.RoomId

interface DeleteRoomPort {
    fun deleteRoom(id: RoomId)

    fun existsRoom(id: RoomId): Boolean
}