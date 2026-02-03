package com.devpaik.metting.adapter.secondary.persistence.repository

import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalTime


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("회의실 테이블 단위 테스트")
@ExtendWith(SpringExtension::class)
@DataJpaTest
class TbRoomRepositoryTest {

    @Autowired
    private lateinit var roomRepository: TbRoomRepository

    private lateinit var room1: TbRoom
    private lateinit var room2: TbRoom
    private lateinit var room3: TbRoom

    @BeforeEach
    fun setUp() {
        room1 = TbRoom(buildingName = "건물 A", floor = 2, roomName = "대회의실 1", capacity = 10, contents = "회의실1", roomStatus = RoomStatus.AVAILABLE)
        room2 = TbRoom(buildingName = "건물 A", floor = 2, roomName = "중회의실 1", capacity = 8, contents = "회의실2", roomStatus = RoomStatus.AVAILABLE)
        room3 = TbRoom(buildingName = "건물 B", floor = 3, roomName = "소회의실 1", capacity = 4, contents = "회의실1", roomStatus = RoomStatus.UNAVAILABLE)

        roomRepository.save(room1)
        roomRepository.save(room2)
        roomRepository.save(room3)

    }

    @Test
    fun `findByBuildingId - 특정 건물명의 회의실 목록을 조회한다`() {
        // when
        val foundRooms = roomRepository.findByBuildingName("건물 A")

        // then
        foundRooms.size shouldBe 2
        foundRooms shouldContainExactlyInAnyOrder listOf(room1, room2)
    }

    @Test
    fun `searchRooms - 모든 조건이 null일 때 모든 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms(null, null, null, null)

        // then
        foundRooms.size shouldBe 3
        foundRooms shouldContainExactlyInAnyOrder listOf(room1, room2, room3)
    }

    @Test
    fun `searchRooms - 건물명으로 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms("건물 A", null, null, null)

        // then
        foundRooms.size shouldBe 2
        foundRooms shouldContainExactlyInAnyOrder listOf(room1, room2)
    }

    @Test
    fun `searchRooms - 층으로 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms(null, 2, null, null)

        // then
        foundRooms.size shouldBe 2
        foundRooms shouldContainExactlyInAnyOrder listOf(room1, room2)
    }

    @Test
    fun `searchRooms - 상태로 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms(null, null, RoomStatus.UNAVAILABLE, null)

        // then
        foundRooms.size shouldBe 1
        foundRooms shouldContainExactlyInAnyOrder listOf(room3)
    }

    @Test
    fun `searchRooms - 최소 수용 인원으로 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms(null, null, null, 9)

        // then
        foundRooms.size shouldBe 1
        foundRooms shouldContainExactlyInAnyOrder listOf(room1)
    }

    @Test
    fun `searchRooms - 여러 조건으로 회의실을 조회한다`() {
        // when
        val foundRooms = roomRepository.searchRooms("건물 A", 2, RoomStatus.AVAILABLE, 5)

        // then
        foundRooms.size shouldBe 2
        foundRooms shouldContainExactlyInAnyOrder listOf(room1, room2)
    }

    @Test
    fun `findAvailableRooms - 예약이 없을 때 가용 회의실을 조회한다`() {
        // given
        val date = LocalDate.now()
        val startTime = LocalTime.of(10, 0)
        val endTime = LocalTime.of(11, 0)

        // when
        val availableRooms = roomRepository.findAvailableRooms(date, startTime, endTime, null)

        // then
        availableRooms.size shouldBe 2
        availableRooms shouldContainExactlyInAnyOrder listOf(room1, room2)
    }
}