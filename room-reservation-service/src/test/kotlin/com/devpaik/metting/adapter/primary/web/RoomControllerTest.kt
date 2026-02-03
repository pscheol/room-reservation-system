package com.devpaik.metting.adapter.primary.web

import com.devpaik.metting.application.room.usecase.CreateRoomUseCase
import com.devpaik.metting.application.room.usecase.DeleteRoomUseCase
import com.devpaik.metting.application.room.usecase.QueryRoomUseCase
import com.devpaik.metting.application.room.usecase.UpdateRoomUseCase
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.springframework.http.HttpStatus
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class RoomControllerTest : BehaviorSpec({

    val createRoomUseCase = mockk<CreateRoomUseCase>()
    val updateRoomUseCase = mockk<UpdateRoomUseCase>()
    val deleteRoomUseCase = mockk<DeleteRoomUseCase>()
    val queryRoomUseCase = mockk<QueryRoomUseCase>()

    val controller = RoomController(
        createRoomUseCase,
        updateRoomUseCase,
        deleteRoomUseCase,
        queryRoomUseCase
    )

    fun createRoom(id: Long = 1L): Room {
        return Room.reconstitute(
            id = RoomId(id),
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

    Given("회의실 단일 조회") {
        val roomId = 1L
        val room = createRoom(roomId)

        When("GET /api/rooms/{roomId}를 호출하면") {
            every { queryRoomUseCase.getRoom(RoomId(roomId)) } returns room

            val response = controller.getRoom(roomId)

            Then("200 OK와 회의실 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.id shouldBe roomId
                response.body?.buildingName shouldBe "본관"
                response.body?.roomName shouldBe "회의실A"
            }
        }
    }

    Given("회의실 검색") {
        val rooms = listOf(createRoom(1L), createRoom(2L))

        When("GET /api/rooms를 호출하면") {
            every { queryRoomUseCase.searchRooms(any()) } returns rooms

            val response = controller.searchRooms(
                buildingName = "본관",
                floor = 3L,
                minCapacity = 5,
                status = RoomStatus.AVAILABLE
            )

            Then("200 OK와 회의실 목록을 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.size shouldBe 2
            }
        }
    }

    Given("가용 회의실 조회") {
        val rooms = listOf(createRoom(1L))

        When("GET /api/rooms/available를 호출하면") {
            every { queryRoomUseCase.findAvailableRooms(any()) } returns rooms

            val response = controller.findAvailableRooms(
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                buildingName = "본관",
                minCapacity = 5
            )

            Then("200 OK와 가용 회의실 목록을 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.size shouldBe 1
            }
        }
    }

    Given("회의실 가용성 확인") {
        When("GET /api/rooms/check-availability를 호출하고 가용하면") {
            every { queryRoomUseCase.checkAvailability(any()) } returns true

            val response = controller.checkAvailability(
                roomId = 1L,
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            Then("200 OK와 available: true를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.get("available") shouldBe true
            }
        }

        When("GET /api/rooms/check-availability를 호출하고 불가용하면") {
            every { queryRoomUseCase.checkAvailability(any()) } returns false

            val response = controller.checkAvailability(
                roomId = 1L,
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            Then("200 OK와 available: false를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.get("available") shouldBe false
            }
        }
    }

    Given("회의실 생성") {
        val room = createRoom(1L)
        val request = com.devpaik.metting.adapter.primary.web.dto.CreateRoomRequest(
            floor = 3L,
            buildingName = "본관",
            roomName = "회의실A",
            capacity = 10,
            contents = "프로젝터 있음"
        )

        When("POST /api/rooms를 호출하면") {
            every { createRoomUseCase.createRoom(any()) } returns room

            val response = controller.createRoom(request)

            Then("201 Created와 생성된 회의실 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.CREATED
                response.body?.id shouldBe 1L
                response.body?.buildingName shouldBe "본관"
            }
        }
    }

    Given("회의실 수정") {
        val room = createRoom(1L)
        val request = com.devpaik.metting.adapter.primary.web.dto.UpdateRoomRequest(
            buildingName = "별관",
            roomName = "대회의실",
            capacity = 20,
            contents = null
        )

        When("PUT /api/rooms/{id}를 호출하면") {
            every { updateRoomUseCase.updateRoom(any()) } returns room.update(
                buildingName = "별관",
                roomName = "대회의실",
                capacity = 20,
                contents = null
            )

            val response = controller.updateRoom(1L, request)

            Then("200 OK와 수정된 회의실 정보를 반환한다") {
                response.statusCode shouldBe HttpStatus.OK
                response.body?.buildingName shouldBe "별관"
                response.body?.roomName shouldBe "대회의실"
            }
        }
    }

    Given("회의실 삭제") {
        When("DELETE /api/rooms/{id}를 호출하면") {
            every { deleteRoomUseCase.deleteRoom(any()) } returns Unit

            val response = controller.deleteRoom(1L)

            Then("204 No Content를 반환한다") {
                response.statusCode shouldBe HttpStatus.NO_CONTENT
                verify(exactly = 1) { deleteRoomUseCase.deleteRoom(RoomId(1L)) }
            }
        }
    }
})