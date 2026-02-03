package com.devpaik.metting.application.room.usecase

import com.devpaik.metting.domain.common.vo.RoomId

/**
 * 회의실 삭제 Use Case 인터페이스
 */
interface DeleteRoomUseCase {
    fun deleteRoom(roomId: RoomId)
}