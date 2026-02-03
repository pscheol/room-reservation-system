package com.devpaik.metting.application.room.usecase

import com.devpaik.metting.application.room.usecase.command.CreateRoomCommand
import com.devpaik.metting.domain.room.aggregate.Room

/**
 * 회의실 생성 Use Case 인터페이스
 */
interface CreateRoomUseCase {
    fun createRoom(command: CreateRoomCommand): Room
}