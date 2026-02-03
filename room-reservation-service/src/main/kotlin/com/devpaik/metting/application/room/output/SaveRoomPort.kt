package com.devpaik.metting.application.room.output

import com.devpaik.metting.domain.room.aggregate.Room

interface SaveRoomPort {
    fun saveRoom(room: Room): Room
}