package com.devpaik.metting.domain.room.service

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomRequirement
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import com.devpaik.metting.domain.room.service.strategy.RoomMatchingStrategy
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.mockk.every
import io.mockk.mockk
import java.time.LocalDateTime
import java.time.LocalTime

class RoomMatcherTest : BehaviorSpec({

    val matchingStrategy = mockk<RoomMatchingStrategy>()
    val roomMatcher = RoomMatcherService(matchingStrategy)

    Given("최적 회의실 찾기") {
        val requirement = RoomRequirement(
            requiredCapacity = 5,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0)
        )

        When("적합한 회의실이 있으면") {
            val availableRooms = listOf(
                createAvailableRoom(id = 1L, capacity = 10),
                createAvailableRoom(id = 2L, capacity = 8),
                createAvailableRoom(id = 3L, capacity = 5)
            )
            val bestRoom = availableRooms[2]

            every { matchingStrategy.selectBest(any(), any()) } returns bestRoom

            val result = roomMatcher.findBestMatch(availableRooms, requirement)

            Then("최적의 회의실을 반환한다") {
                result shouldNotBe null
                result?.capacity?.value shouldBe 5
            }
        }

        When("적합한 회의실이 없으면") {
            val availableRooms = listOf(
                createAvailableRoom(id = 1L, capacity = 3),
                createAvailableRoom(id = 2L, capacity = 4)
            )

            val result = roomMatcher.findBestMatch(availableRooms, requirement)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }

        When("사용 불가능한 회의실만 있으면") {
            val availableRooms = listOf(
                createUnavailableRoom(id = 1L, capacity = 10),
                createMaintenanceRoom(id = 2L, capacity = 10)
            )

            val result = roomMatcher.findBestMatch(availableRooms, requirement)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }

        When("빈 목록이 주어지면") {
            val availableRooms = emptyList<Room>()

            val result = roomMatcher.findBestMatch(availableRooms, requirement)

            Then("null을 반환한다") {
                result shouldBe null
            }
        }
    }

    Given("적합한 회의실 필터링") {
        val requirement = RoomRequirement(
            requiredCapacity = 5,
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0)
        )

        When("여러 회의실이 있으면") {
            val rooms = listOf(
                createAvailableRoom(id = 1L, capacity = 10),  // 적합
                createAvailableRoom(id = 2L, capacity = 3),   // 수용 인원 부족
                createUnavailableRoom(id = 3L, capacity = 10), // 사용 불가
                createAvailableRoom(id = 4L, capacity = 5)    // 적합
            )

            val result = roomMatcher.filterSuitableRooms(rooms, requirement)

            Then("적합한 회의실만 반환한다") {
                result.size shouldBe 2
                result.map { it.id?.value } shouldBe listOf(1L, 4L)
            }
        }

        When("모든 회의실이 적합하지 않으면") {
            val rooms = listOf(
                createAvailableRoom(id = 1L, capacity = 3),
                createUnavailableRoom(id = 2L, capacity = 10)
            )

            val result = roomMatcher.filterSuitableRooms(rooms, requirement)

            Then("빈 목록을 반환한다") {
                result shouldBe emptyList()
            }
        }
    }
})

private fun createAvailableRoom(id: Long, capacity: Int): Room {
    return Room.reconstitute(
        id = RoomId(id),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실$id",
        capacity = capacity,
        contents = "프로젝터 있음",
        status = RoomStatus.AVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createUnavailableRoom(id: Long, capacity: Int): Room {
    return Room.reconstitute(
        id = RoomId(id),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실$id",
        capacity = capacity,
        contents = "청소 중",
        status = RoomStatus.UNAVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createMaintenanceRoom(id: Long, capacity: Int): Room {
    return Room.reconstitute(
        id = RoomId(id),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실$id",
        capacity = capacity,
        contents = "수리 중",
        status = RoomStatus.MAINTENANCE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}