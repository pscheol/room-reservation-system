package com.devpaik.metting.application.room.usecase.service

import com.devpaik.metting.application.room.output.DeleteRoomPort
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.output.SaveRoomPort
import com.devpaik.metting.application.room.usecase.CreateRoomUseCase
import com.devpaik.metting.application.room.usecase.DeleteRoomUseCase
import com.devpaik.metting.application.room.usecase.UpdateRoomUseCase
import com.devpaik.metting.application.room.usecase.command.CreateRoomCommand
import com.devpaik.metting.application.room.usecase.command.UpdateRoomCommand
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/**
 * 회의실 생성 애플리케이션 서비스
 */
@Service
@Transactional
class CreateRoomService(
    private val saveRoomPort: SaveRoomPort
) : CreateRoomUseCase {

    override fun createRoom(command: CreateRoomCommand): Room {
        // 도메인 팩토리 메서드를 통한 회의실 생성
        val room = Room.create(
            floor = command.floor,
            buildingName = command.buildingName,
            roomName = command.roomName,
            capacity = command.capacity,
            contents = command.contents
        )

        // 저장
        return saveRoomPort.saveRoom(room)
    }
}

/**
 * 회의실 수정 애플리케이션 서비스
 */
@Service
@Transactional
class UpdateRoomService(
    private val loadRoomPort: LoadRoomPort,
    private val saveRoomPort: SaveRoomPort
) : UpdateRoomUseCase {

    override fun updateRoom(command: UpdateRoomCommand): Room {
        // 회의실 조회
        val room = loadRoomPort.loadRoom(command.roomId)
            ?: throw IllegalArgumentException("회의실을 찾을 수 없습니다: ${command.roomId.value}")

        // 도메인 모델의 update 메서드 호출 (비즈니스 규칙 자동 적용)
        val updatedRoom = room.update(
            buildingName = command.buildingName,
            roomName = command.roomName,
            capacity = command.capacity,
            contents = command.contents
        )

        // 상태 변경이 필요한 경우
        val finalRoom = when {
            command.makeUnavailable -> updatedRoom.makeUnavailable(command.unavailableReason)
            command.startMaintenance != null -> updatedRoom.startMaintenance(command.startMaintenance)
            command.makeAvailable -> updatedRoom.makeAvailable()
            else -> updatedRoom
        }

        // 저장
        return saveRoomPort.saveRoom(finalRoom)
    }
}

/**
 * 회의실 삭제 애플리케이션 서비스
 */
@Service
@Transactional
class DeleteRoomService(
    private val deleteRoomPort: DeleteRoomPort
) : DeleteRoomUseCase {

    override fun deleteRoom(roomId: RoomId) {
        if (!deleteRoomPort.existsRoom(roomId)) {
            throw IllegalArgumentException("회의실을 찾을 수 없습니다: ${roomId.value}")
        }

        deleteRoomPort.deleteRoom(roomId)
    }
}