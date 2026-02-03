package com.devpaik.metting.application.room.usecase.service

import com.devpaik.metting.application.room.output.DeleteRoomPort
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.output.SaveRoomPort
import com.devpaik.metting.application.room.usecase.command.CreateRoomCommand
import com.devpaik.metting.application.room.usecase.command.UpdateRoomCommand
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDateTime

class CreateRoomServiceTest : BehaviorSpec({

    val saveRoomPort = mockk<SaveRoomPort>()

    val service = CreateRoomService(saveRoomPort)

    Given("회의실 생성") {
        val command = CreateRoomCommand(
            floor = 3L,
            buildingName = "본관",
            roomName = "회의실A",
            capacity = 10,
            contents = "프로젝터 있음"
        )

        When("유효한 정보로 회의실을 생성하면") {
            val savedRoom = createAvailableRoom()
            every { saveRoomPort.saveRoom(any()) } returns savedRoom

            val result = service.createRoom(command)

            Then("회의실이 생성되고 저장된다") {
                result shouldBe savedRoom
                verify(exactly = 1) { saveRoomPort.saveRoom(any()) }
            }
        }
    }
})

class UpdateRoomServiceTest : BehaviorSpec({

    val loadRoomPort = mockk<LoadRoomPort>()
    val saveRoomPort = mockk<SaveRoomPort>()

    val service = UpdateRoomService(loadRoomPort, saveRoomPort)

    Given("회의실 정보 수정") {
        val roomId = RoomId(1L)
        val existingRoom = createAvailableRoom()
        val command = UpdateRoomCommand(
            roomId = roomId,
            buildingName = "별관",
            roomName = "대회의실",
            capacity = 20,
            contents = "빔프로젝터, 화이트보드"
        )

        When("유효한 정보로 회의실을 수정하면") {
            val updatedRoom = existingRoom.update(
                buildingName = command.buildingName,
                roomName = command.roomName,
                capacity = command.capacity,
                contents = command.contents
            )

            every { loadRoomPort.loadRoom(roomId) } returns existingRoom
            every { saveRoomPort.saveRoom(any()) } returns updatedRoom

            val result = service.updateRoom(command)

            Then("회의실이 수정되고 저장된다") {
                result.buildingName shouldBe BuildingName("별관")
                result.roomName.value shouldBe "대회의실"
                verify(exactly = 1) { loadRoomPort.loadRoom(roomId) }
                verify(exactly = 1) { saveRoomPort.saveRoom(any()) }
            }
        }

        When("존재하지 않는 회의실을 수정하면") {
            val exceptionMessage = "회의실을 찾을 수 없습니다: ${roomId.value}"
            every { loadRoomPort.loadRoom(roomId) } throws IllegalStateException(exceptionMessage)

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    service.updateRoom(command)
                }
                exception.message shouldBe exceptionMessage
            }
        }
    }

    Given("회의실 상태 변경") {
        val roomId = RoomId(1L)
        val existingRoom = createAvailableRoom()

        When("사용 불가로 변경하면") {
            val command = UpdateRoomCommand(
                roomId = roomId,
                buildingName = existingRoom.buildingName.value,
                roomName = existingRoom.roomName.value,
                capacity = existingRoom.capacity.value,
                contents = existingRoom.contents,
                makeUnavailable = true,
                unavailableReason = "청소 중"
            )
            val unavailableRoom = existingRoom.makeUnavailable("청소 중")

            every { loadRoomPort.loadRoom(roomId) } returns existingRoom
            every { saveRoomPort.saveRoom(any()) } returns unavailableRoom

            val result = service.updateRoom(command)

            Then("UNAVAILABLE 상태가 된다") {
                result.status shouldBe RoomStatus.UNAVAILABLE
            }
        }

        When("정비 시작으로 변경하면") {
            val command = UpdateRoomCommand(
                roomId = roomId,
                buildingName = existingRoom.buildingName.value,
                roomName = existingRoom.roomName.value,
                capacity = existingRoom.capacity.value,
                contents = existingRoom.contents,
                startMaintenance = "에어컨 수리"
            )
            val maintenanceRoom = existingRoom.startMaintenance("에어컨 수리")

            every { loadRoomPort.loadRoom(roomId) } returns existingRoom
            every { saveRoomPort.saveRoom(any()) } returns maintenanceRoom

            val result = service.updateRoom(command)

            Then("MAINTENANCE 상태가 된다") {
                result.status shouldBe RoomStatus.MAINTENANCE
            }
        }

        When("사용 가능으로 변경하면") {
            val unavailableRoom = createUnavailableRoom()
            val command = UpdateRoomCommand(
                roomId = roomId,
                buildingName = unavailableRoom.buildingName.value,
                roomName = unavailableRoom.roomName.value,
                capacity = unavailableRoom.capacity.value,
                contents = unavailableRoom.contents,
                makeAvailable = true
            )
            val availableRoom = unavailableRoom.makeAvailable()

            every { loadRoomPort.loadRoom(roomId) } returns unavailableRoom
            every { saveRoomPort.saveRoom(any()) } returns availableRoom

            val result = service.updateRoom(command)

            Then("AVAILABLE 상태가 된다") {
                result.status shouldBe RoomStatus.AVAILABLE
            }
        }
    }
})

class DeleteRoomServiceTest : BehaviorSpec({

    val deleteRoomPort = mockk<DeleteRoomPort>()

    val service = DeleteRoomService(deleteRoomPort)

    Given("회의실 삭제") {
        val roomId = RoomId(1L)

        When("존재하는 회의실을 삭제하면") {
            every { deleteRoomPort.existsRoom(roomId) } returns true
            every { deleteRoomPort.deleteRoom(roomId) } returns Unit

            service.deleteRoom(roomId)

            Then("회의실이 삭제된다") {
                verify(exactly = 1) { deleteRoomPort.existsRoom(roomId) }
                verify(exactly = 1) { deleteRoomPort.deleteRoom(roomId) }
            }
        }

        When("존재하지 않는 회의실을 삭제하면") {
            every { deleteRoomPort.existsRoom(roomId) } returns false

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    service.deleteRoom(roomId)
                }
                exception.message shouldBe "회의실을 찾을 수 없습니다: ${roomId.value}"
            }
        }
    }
})

private fun createAvailableRoom(): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = 10,
        contents = "프로젝터 있음",
        status = RoomStatus.AVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createUnavailableRoom(): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = 10,
        contents = "청소 중",
        status = RoomStatus.UNAVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}