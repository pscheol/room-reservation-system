package com.devpaik.metting.application.room.usecase.command

import com.devpaik.metting.domain.common.vo.RoomId

/**
 * 회의실 생성 커맨드
 */
data class CreateRoomCommand(
    val floor: Long,
    val buildingName: String,
    val roomName: String,
    val capacity: Int,
    val contents: String?
)

/**
 * 회의실 수정 커맨드
 */
data class UpdateRoomCommand(
    val roomId: RoomId,
    val buildingName: String,
    val roomName: String,
    val capacity: Int,
    val contents: String?,
    val makeUnavailable: Boolean = false,
    val unavailableReason: String? = null,
    val startMaintenance: String? = null,
    val makeAvailable: Boolean = false
)