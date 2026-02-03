package com.devpaik.metting.adapter.secondary.persistence.adapter

import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.adapter.secondary.persistence.mapper.RoomMapper
import com.devpaik.metting.adapter.secondary.persistence.repository.TbRoomRepository
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
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
import io.mockk.slot
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class TbRoomAdapterTest : BehaviorSpec({

    val tbRoomRepository = mockk<TbRoomRepository>()
    val roomMapper = mockk<RoomMapper>()

    val adapter = TbRoomAdapter(tbRoomRepository, roomMapper)

    fun createTbRoom(id: Long = 1L) = TbRoom(
        id = id,
        buildingName = "본관",
        floor = 3L,
        roomName = "회의실A",
        capacity = 10,
        contents = "프로젝터 있음",
        roomStatus = RoomStatus.AVAILABLE
    )

    fun createDomainRoom(id: Long = 1L) = Room.reconstitute(
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

    Given("회의실 조회") {
        val roomId = RoomId(1L)
        val tbRoom = createTbRoom()
        val domainRoom = createDomainRoom()

        When("loadRoom을 호출하면") {
            every { tbRoomRepository.findById(roomId.value) } returns Optional.of(tbRoom)
            every { roomMapper.toDomain(tbRoom) } returns domainRoom

            val result = adapter.loadRoom(roomId)

            Then("해당 회의실을 반환한다") {
                result shouldBe domainRoom
                verify(exactly = 1) { tbRoomRepository.findById(roomId.value) }
                verify(exactly = 1) { roomMapper.toDomain(tbRoom) }
            }
        }

        When("존재하지 않는 회의실 ID로 loadRoom을 호출하면") {
            every { tbRoomRepository.findById(roomId.value) } returns Optional.empty()

            Then("IllegalStateException이 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    adapter.loadRoom(roomId)
                }
                exception.message shouldBe "회의실을 찾을 수 없습니다: ${roomId.value}"
            }
        }
    }

    Given("회의실 검색") {
        val query = RoomQuery(
            buildingName = BuildingName("본관"),
            floor = Floor(3L),
            status = RoomStatus.AVAILABLE,
            minCapacity = 5
        )
        val tbRooms = listOf(createTbRoom(1L), createTbRoom(2L))
        val domainRooms = listOf(createDomainRoom(1L), createDomainRoom(2L))

        When("loadRooms를 호출하면") {
            every {
                tbRoomRepository.searchRooms(
                    buildingName = "본관",
                    floor = 3L,
                    status = RoomStatus.AVAILABLE,
                    minCapacity = 5
                )
            } returns tbRooms
            every { roomMapper.toDomainList(tbRooms) } returns domainRooms

            val result = adapter.loadRooms(query)

            Then("조건에 맞는 회의실 목록을 반환한다") {
                result.size shouldBe 2
                verify(exactly = 1) {
                    tbRoomRepository.searchRooms(
                        buildingName = "본관",
                        floor = 3L,
                        status = RoomStatus.AVAILABLE,
                        minCapacity = 5
                    )
                }
            }
        }
    }

    Given("가용 회의실 조회") {
        val date = LocalDate.now().plusDays(1)
        val startTime = LocalTime.of(10, 0)
        val endTime = LocalTime.of(11, 0)

        When("건물명 없이 loadAvailableRooms를 호출하면") {
            val query = AvailableRoomQuery(
                date = date,
                startTime = startTime,
                endTime = endTime,
                buildingName = null,
                minCapacity = 5
            )
            val tbRooms = listOf(createTbRoom(1L))
            val domainRooms = listOf(createDomainRoom(1L))

            every {
                tbRoomRepository.findAvailableRooms(
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    minCapacity = 5
                )
            } returns tbRooms
            every { roomMapper.toDomainList(tbRooms) } returns domainRooms

            val result = adapter.loadAvailableRooms(query)

            Then("가용 회의실 목록을 반환한다") {
                result.size shouldBe 1
                verify(exactly = 1) {
                    tbRoomRepository.findAvailableRooms(
                        date = date,
                        startTime = startTime,
                        endTime = endTime,
                        minCapacity = 5
                    )
                }
            }
        }

        When("건물명으로 필터링하여 loadAvailableRooms를 호출하면") {
            val query = AvailableRoomQuery(
                date = date,
                startTime = startTime,
                endTime = endTime,
                buildingName = BuildingName("본관"),
                minCapacity = null
            )
            val buildingRooms = listOf(createTbRoom(1L), createTbRoom(2L))
            val availableRooms = listOf(createTbRoom(1L), createTbRoom(3L))
            val domainRooms = listOf(createDomainRoom(1L))

            every { tbRoomRepository.findByBuildingName("본관") } returns buildingRooms
            every {
                tbRoomRepository.findAvailableRooms(
                    date = date,
                    startTime = startTime,
                    endTime = endTime,
                    minCapacity = null
                )
            } returns availableRooms

            val listSlot = slot<List<TbRoom>>()
            every { roomMapper.toDomainList(capture(listSlot)) } answers {
                val captured = listSlot.captured
                captured.filter { it.id == 1L }.map { createDomainRoom(it.id!!) }
            }

            val result = adapter.loadAvailableRooms(query)

            Then("건물에 속한 가용 회의실만 반환한다") {
                result.size shouldBe 1
                result[0].id?.value shouldBe 1L
                verify(exactly = 1) { tbRoomRepository.findByBuildingName("본관") }
            }
        }
    }

    Given("회의실 저장") {
        When("새 회의실을 저장하면") {
            val newRoom = Room.create(
                floor = 3L,
                buildingName = "본관",
                roomName = "신규 회의실",
                capacity = 10,
                contents = "프로젝터 있음"
            )
            val newTbRoom = createTbRoom()
            val savedTbRoom = createTbRoom(10L)
            val savedDomainRoom = createDomainRoom(10L)

            every { roomMapper.toEntity(newRoom) } returns newTbRoom
            every { tbRoomRepository.save(newTbRoom) } returns savedTbRoom
            every { roomMapper.toDomain(savedTbRoom) } returns savedDomainRoom

            val result = adapter.saveRoom(newRoom)

            Then("새 회의실이 저장된다") {
                result shouldBe savedDomainRoom
                verify(exactly = 1) { roomMapper.toEntity(newRoom) }
                verify(exactly = 1) { tbRoomRepository.save(newTbRoom) }
            }
        }

        When("기존 회의실을 수정하면") {
            val existingRoom = createDomainRoom(1L)
            val existingTbRoom = createTbRoom(1L)
            val updatedRoom = existingRoom.update(
                buildingName = "별관",
                roomName = "대회의실",
                capacity = 20,
                contents = null
            )

            every { tbRoomRepository.findById(any()) } returns Optional.of(existingTbRoom)
            every { roomMapper.updateEntity(any(), any()) } returns Unit
            every { tbRoomRepository.save(any()) } returns existingTbRoom
            every { roomMapper.toDomain(any()) } returns updatedRoom

            val result = adapter.saveRoom(updatedRoom)

            Then("기존 회의실이 수정된다") {
                result shouldBe updatedRoom
            }
        }

        When("존재하지 않는 회의실을 수정하면") {
            val nonExistentRoom = createDomainRoom(999L)

            every { tbRoomRepository.findById(999L) } returns Optional.empty()

            Then("IllegalArgumentException이 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    adapter.saveRoom(nonExistentRoom)
                }
                exception.message shouldBe "회의실을 찾을 수 없습니다: 999"
            }
        }
    }

    Given("회의실 삭제") {
        val roomId = RoomId(1L)

        When("deleteRoom을 호출하면") {
            every { tbRoomRepository.deleteById(roomId.value) } returns Unit

            adapter.deleteRoom(roomId)

            Then("회의실이 삭제된다") {
                verify(exactly = 1) { tbRoomRepository.deleteById(roomId.value) }
            }
        }
    }

    Given("회의실 존재 여부 확인") {
        val roomId = RoomId(1L)

        When("존재하는 회의실 ID로 existsRoom을 호출하면") {
            every { tbRoomRepository.existsById(roomId.value) } returns true

            val result = adapter.existsRoom(roomId)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("존재하지 않는 회의실 ID로 existsRoom을 호출하면") {
            every { tbRoomRepository.existsById(roomId.value) } returns false

            val result = adapter.existsRoom(roomId)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }
    }
})