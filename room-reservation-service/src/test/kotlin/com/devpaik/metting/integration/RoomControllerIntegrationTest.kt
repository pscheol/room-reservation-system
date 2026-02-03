package com.devpaik.metting.integration

import com.devpaik.metting.adapter.primary.web.RoomController
import com.devpaik.metting.adapter.primary.web.dto.CreateRoomRequest
import com.devpaik.metting.adapter.primary.web.dto.UpdateRoomRequest
import com.devpaik.metting.application.room.usecase.CreateRoomUseCase
import com.devpaik.metting.application.room.usecase.DeleteRoomUseCase
import com.devpaik.metting.application.room.usecase.QueryRoomUseCase
import com.devpaik.metting.application.room.usecase.UpdateRoomUseCase
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
import io.mockk.justRun
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest
import org.springframework.http.MediaType
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import tools.jackson.module.kotlin.jacksonObjectMapper
import java.time.LocalDate
import java.time.LocalDateTime


@AutoConfigureMockMvc
@WebMvcTest(controllers = [RoomController::class])
class RoomControllerIntegrationTest : DescribeSpec() {


    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createRoomUseCase: CreateRoomUseCase

    @MockkBean
    private lateinit var updateRoomUseCase: UpdateRoomUseCase

    @MockkBean
    private lateinit var deleteRoomUseCase: DeleteRoomUseCase

    @MockkBean
    private lateinit var queryRoomUseCase: QueryRoomUseCase

    init {
        extensions(listOf(SpringExtension()))
        val objectMapper = jacksonObjectMapper()

        afterEach {
            clearAllMocks()
        }

        describe("POST /api/rooms - 회의실 생성") {
            val request = CreateRoomRequest(
                floor = 3L,
                buildingName = "본관",
                roomName = "회의실A",
                capacity = 10,
                contents = "프로젝터 있음"
            )

            context("유효한 요청으로 회의실을 생성하면") {
                it("201 Created를 반환한다") {
                    val room = createMockRoom(1L)
                    every { createRoomUseCase.createRoom(any()) } returns room

                    mockMvc.perform(
                        post("/api/rooms")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.id").value(1))
                        .andExpect(jsonPath("$.roomName").value("회의실A"))
                        .andExpect(jsonPath("$.buildingName").value("본관"))
                        .andExpect(jsonPath("$.capacity").value(10))
                }
            }
        }

        describe("GET /api/rooms/{id} - 회의실 조회") {
            val roomId = 1L

            context("존재하는 회의실을 조회하면") {
                it("200 OK와 회의실 정보를 반환한다") {
                    val room = createMockRoom(roomId)
                    every { queryRoomUseCase.getRoom(RoomId(roomId)) } returns room

                    mockMvc.perform(get("/api/rooms/$roomId"))
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(roomId))
                        .andExpect(jsonPath("$.roomName").value("회의실A"))
                }
            }
        }

        describe("GET /api/rooms - 회의실 검색") {
            context("건물명으로 검색하면") {
                it("200 OK와 회의실 목록을 반환한다") {
                    val rooms = listOf(createMockRoom(1L), createMockRoom(2L))
                    every { queryRoomUseCase.searchRooms(any()) } returns rooms

                    mockMvc.perform(
                        get("/api/rooms")
                            .param("buildingName", "본관")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(2))
                }
            }

            context("층수와 최소 수용 인원으로 검색하면") {
                it("200 OK와 조건에 맞는 회의실 목록을 반환한다") {
                    val rooms = listOf(createMockRoom(1L), createMockRoom(2L))
                    every { queryRoomUseCase.searchRooms(any()) } returns rooms

                    mockMvc.perform(
                        get("/api/rooms")
                            .param("floor", "3")
                            .param("minCapacity", "5")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(2))
                }
            }
        }

        describe("GET /api/rooms/available - 가용 회의실 조회") {
            context("날짜와 시간으로 가용 회의실을 조회하면") {
                it("200 OK와 가용 회의실 목록을 반환한다") {
                    val rooms = listOf(createMockRoom(1L))
                    every { queryRoomUseCase.findAvailableRooms(any()) } returns rooms

                    mockMvc.perform(
                        get("/api/rooms/available")
                            .param("date", LocalDate.now().plusDays(1).toString())
                            .param("startTime", "10:00")
                            .param("endTime", "11:00")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(1))
                }
            }

            context("건물명과 최소 수용 인원 조건을 추가하면") {
                it("200 OK와 조건에 맞는 가용 회의실 목록을 반환한다") {
                    val rooms = listOf(createMockRoom(1L))
                    every { queryRoomUseCase.findAvailableRooms(any()) } returns rooms

                    mockMvc.perform(
                        get("/api/rooms/available")
                            .param("date", LocalDate.now().plusDays(1).toString())
                            .param("startTime", "10:00")
                            .param("endTime", "11:00")
                            .param("buildingName", "본관")
                            .param("minCapacity", "5")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(1))
                }
            }
        }

        describe("GET /api/rooms/check-availability - 회의실 가용성 확인") {
            val roomId = 1L

            context("가용한 시간으로 확인하면") {
                it("200 OK와 true를 반환한다") {
                    every { queryRoomUseCase.checkAvailability(any()) } returns true

                    mockMvc.perform(
                        get("/api/rooms/check-availability")
                            .param("roomId", roomId.toString())
                            .param("date", LocalDate.now().plusDays(1).toString())
                            .param("startTime", "10:00")
                            .param("endTime", "11:00")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.available").value(true))
                }
            }

            context("가용하지 않은 시간으로 확인하면") {
                it("200 OK와 false를 반환한다") {
                    every { queryRoomUseCase.checkAvailability(any()) } returns false

                    mockMvc.perform(
                        get("/api/rooms/check-availability")
                            .param("roomId", roomId.toString())
                            .param("date", LocalDate.now().plusDays(1).toString())
                            .param("startTime", "10:00")
                            .param("endTime", "11:00")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.available").value(false))
                }
            }
        }

        describe("PUT /api/rooms/{id} - 회의실 수정") {
            val roomId = 1L
            val request = UpdateRoomRequest(
                buildingName = "별관",
                roomName = "대회의실",
                capacity = 20,
                contents = "빔 프로젝터, 화이트보드",
            )

            context("회의실 정보를 수정하면") {
                it("200 OK와 수정된 회의실 정보를 반환한다") {
                    val updatedRoom = createMockRoom(roomId)
                    every { updateRoomUseCase.updateRoom(any()) } returns updatedRoom

                    mockMvc.perform(
                        put("/api/rooms/$roomId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(roomId))
                }
            }
        }

        describe("DELETE /api/rooms/{id} - 회의실 삭제") {
            val roomId = 1L

            context("회의실을 삭제하면") {
                it("204 No Content를 반환한다") {
                    justRun { deleteRoomUseCase.deleteRoom(RoomId(roomId)) }

                    mockMvc.perform(delete("/api/rooms/$roomId"))
                        .andExpect(status().isNoContent)
                }
            }
        }
    }
    private fun createMockRoom(id: Long): Room {
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
}
