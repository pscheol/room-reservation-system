package com.devpaik.metting.application.room.usecase

import com.devpaik.metting.application.room.usecase.command.UpdateRoomCommand
import com.devpaik.metting.domain.room.aggregate.Room

/**
 * 회의실 수정 Use Case 인터페이스
 */
interface UpdateRoomUseCase {
    fun updateRoom(command: UpdateRoomCommand): Room
}