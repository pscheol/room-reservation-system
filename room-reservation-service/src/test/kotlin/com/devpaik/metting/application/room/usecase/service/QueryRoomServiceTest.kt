package com.devpaik.metting.application.room.usecase.service

import com.devpaik.metting.application.reservation.output.LoadReservationPort
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.application.room.usecase.query.AvailabilityQuery
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import com.devpaik.metting.domain.room.service.RoomMatcher
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class QueryRoomServiceTest : BehaviorSpec({

    val loadRoomPort = mockk<LoadRoomPort>()
    val loadReservationPort = mockk<LoadReservationPort>()
    val roomMatcher = mockk<RoomMatcher>()

    val service = QueryRoomService(loadRoomPort, loadReservationPort, roomMatcher)

    Given("단일 회의실 조회") {
        val roomId = RoomId(1L)
        val room = createAvailableRoom(roomId)

        When("존재하는 회의실 ID로 조회하면") {
            every { loadRoomPort.loadRoom(roomId) } returns room

            val result = service.getRoom(roomId)

            Then("해당 회의실을 반환한다") {
                result shouldBe room
                verify(exactly = 1) { loadRoomPort.loadRoom(roomId) }
            }
        }

        When("존재하지 않는 회의실 ID로 조회하면") {
            every { loadRoomPort.loadRoom(roomId) } throws IllegalStateException("회의실을 찾을 수 없습니다: ${roomId.value}")

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    service.getRoom(roomId)
                }
                exception.message shouldBe "회의실을 찾을 수 없습니다: ${roomId.value}"
            }
        }
    }

    Given("회의실 검색") {
        val query = RoomQuery(
            buildingName = BuildingName("본관"),
            floor = Floor(3L),
            status = RoomStatus.AVAILABLE
        )
        val rooms = listOf(
            createAvailableRoom(RoomId(1L)),
            createAvailableRoom(RoomId(2L))
        )

        When("쿼리로 검색하면") {
            every { loadRoomPort.loadRooms(query) } returns rooms

            val result = service.searchRooms(query)

            Then("조건에 맞는 회의실 목록을 반환한다") {
                result.size shouldBe 2
                verify(exactly = 1) { loadRoomPort.loadRooms(query) }
            }
        }
    }

    Given("가용 회의실 조회") {
        val query = AvailableRoomQuery(
            date = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            buildingName = BuildingName("본관"),
            minCapacity = null
        )
        val availableRooms = listOf(
            createAvailableRoom(RoomId(1L)),
            createAvailableRoom(RoomId(2L))
        )

        When("최소 수용 인원 조건 없이 조회하면") {
            every { loadRoomPort.loadAvailableRooms(query) } returns availableRooms

            val result = service.findAvailableRooms(query)

            Then("가용 회의실 목록을 반환한다") {
                result.size shouldBe 2
                verify(exactly = 1) { loadRoomPort.loadAvailableRooms(query) }
                verify(exactly = 0) { roomMatcher.filterSuitableRooms(any(), any()) }
            }
        }

        When("최소 수용 인원 조건이 있으면") {
            val queryWithCapacity = query.copy(minCapacity = 5)
            val filteredRooms = listOf(createAvailableRoom(RoomId(1L)))

            every { loadRoomPort.loadAvailableRooms(queryWithCapacity) } returns availableRooms
            every { roomMatcher.filterSuitableRooms(any(), any()) } returns filteredRooms

            val result = service.findAvailableRooms(queryWithCapacity)

            Then("RoomMatcher를 통해 필터링된 회의실 목록을 반환한다") {
                result.size shouldBe 1
                verify(exactly = 1) { roomMatcher.filterSuitableRooms(any(), any()) }
            }
        }
    }

    Given("회의실 가용성 확인") {
        val roomId = RoomId(1L)
        val room = createAvailableRoom(roomId)
        val query = AvailabilityQuery(
            roomId = roomId,
            date = LocalDate.now().plusDays(1),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            excludeReservationId = null
        )

        When("회의실이 가용하고 충돌이 없으면") {
            every { loadRoomPort.loadRoom(roomId) } returns room
            every { loadReservationPort.loadConflictingReservations(any(), any(), any(), any(), any()) } returns emptyList()

            val result = service.checkAvailability(query)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("회의실이 존재하지 않으면") {
            every { loadRoomPort.loadRoom(roomId) } throws IllegalStateException("회의실을 찾을 수 없습니다: ${roomId.value}")

            Then("예외가 발생한다") {
                shouldThrow<IllegalStateException> {
                    service.checkAvailability(query)
                }
            }
        }

        When("충돌하는 예약이 있으면") {
            every { loadRoomPort.loadRoom(roomId) } returns room
            every { loadReservationPort.loadConflictingReservations(any(), any(), any(), any(), any()) } returns listOf(mockk())

            val result = service.checkAvailability(query)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }

        When("영업 시간 외 예약이면") {
            val earlyQuery = query.copy(startTime = LocalTime.of(5, 0), endTime = LocalTime.of(6, 0))

            every { loadRoomPort.loadRoom(roomId) } returns room

            val result = service.checkAvailability(earlyQuery)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }
})

private fun createAvailableRoom(id: RoomId): Room {
    return Room.reconstitute(
        id = id,
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