package com.devpaik.metting.integration

import com.devpaik.metting.adapter.primary.web.ReservationController
import com.devpaik.metting.adapter.primary.web.dto.CreateReservationRequest
import com.devpaik.metting.adapter.primary.web.dto.UpdateReservationRequest
import com.devpaik.metting.application.reservation.usecase.CreateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.QueryReservationUseCase
import com.devpaik.metting.application.reservation.usecase.UpdateReservationUseCase
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import com.ninjasquad.springmockk.MockkBean
import io.kotest.core.spec.style.DescribeSpec
import io.kotest.extensions.spring.SpringExtension
import io.mockk.clearAllMocks
import io.mockk.every
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
import java.time.LocalTime


@AutoConfigureMockMvc
@WebMvcTest(controllers = [ReservationController::class])
class ReservationControllerIntegrationTest : DescribeSpec() {


    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockkBean
    private lateinit var createReservationUseCase: CreateReservationUseCase

    @MockkBean
    private lateinit var updateReservationUseCase: UpdateReservationUseCase

    @MockkBean
    private lateinit var queryReservationUseCase: QueryReservationUseCase

    @MockkBean
    private lateinit var loadRoomPort: LoadRoomPort

    init {
        extensions(listOf(SpringExtension()))
        val objectMapper = jacksonObjectMapper()

        afterEach {
            clearAllMocks()
        }

        describe("POST /api/reservations - 예약 생성") {
            val request = CreateReservationRequest(
                roomId = 1L,
                email = "test@example.com",
                title = "주간 회의",
                contents = "회의 내용",
                reservationDate = LocalDate.now().plusDays(7),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            context("유효한 요청으로 예약을 생성하면") {
                it("201 Created를 반환한다") {
                    val reservation = createMockReservation(1L)
                    val room = createMockRoom(1L)
                    every { createReservationUseCase.createReservation(any()) } returns Pair(reservation, room)

                    mockMvc.perform(
                        post("/api/reservations")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                    )
                        .andExpect(status().isCreated)
                        .andExpect(jsonPath("$.id").value(1))
                        .andExpect(jsonPath("$.title").value("주간 회의"))
                        .andExpect(jsonPath("$.room.roomName").value("회의실A"))
                }
            }
        }

        describe("GET /api/reservations/{id} - 예약 조회") {
            val reservationId = 1L
            
            context("존재하는 예약을 조회하면") {
                it("200 OK와 예약 정보를 반환한다") {
                    val reservation = createMockReservation(reservationId)
                    val room = createMockRoom(1L)
                    every { queryReservationUseCase.getReservation(ReservationId(reservationId)) } returns reservation
                    every { loadRoomPort.loadRoom(RoomId(1L)) } returns room

                    mockMvc.perform(get("/api/reservations/$reservationId"))
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(reservationId))
                        .andExpect(jsonPath("$.title").value("주간 회의"))
                }
            }
        }

        describe("PUT /api/reservations/{id} - 예약 수정") {
            val reservationId = 1L
            val request = UpdateReservationRequest(
                title = "수정된 회의",
                contents = "수정된 내용",
                reservationDate = LocalDate.now().plusDays(7),
                startTime = LocalTime.of(14, 0),
                endTime = LocalTime.of(15, 0),
                status = ReservationStatus.CONFIRMED
            )
            
            context("예약을 수정하면") {
                it("200 OK와 수정된 예약 정보를 반환한다") {
                    val updatedReservation = createMockReservation(reservationId)
                    val room = createMockRoom(1L)
                    every { updateReservationUseCase.updateReservation(any()) } returns updatedReservation
                    every { loadRoomPort.loadRoom(RoomId(1L)) } returns room

                    mockMvc.perform(
                        put("/api/reservations/$reservationId")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request))
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.title").value("주간 회의"))
                }
            }
        }

        describe("DELETE /api/reservations/{id} - 예약 취소") {
            val reservationId = 1L
            val email = "test@example.com"
            
            context("예약을 취소하면") {
                it("200 OK와 취소된 예약 정보를 반환한다") {
                    val cancelledReservation = createMockReservation(reservationId)
                    val room = createMockRoom(1L)
                    every { updateReservationUseCase.cancelReservation(any()) } returns cancelledReservation
                    every { loadRoomPort.loadRoom(RoomId(1L)) } returns room

                    mockMvc.perform(
                        delete("/api/reservations/$reservationId")
                            .param("email", email)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.id").value(reservationId))
                }
            }
        }

        describe("GET /api/reservations - 예약 검색") {
            context("상태로 검색하면") {
                it("200 OK와 예약 목록을 반환한다") {
                    val reservations = listOf(
                        createMockReservation(1L),
                        createMockReservation(2L)
                    )
                    val room = createMockRoom(1L)
                    every { queryReservationUseCase.searchReservations(any()) } returns reservations
                    every { loadRoomPort.loadRoom(RoomId(1L)) } returns room

                    mockMvc.perform(
                        get("/api/reservations")
                            .param("status", "CONFIRMED")
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(2))
                }
            }
        }

        describe("GET /api/reservations/upcoming - 다가오는 예약 조회") {
            val email = "test@example.com"
            
            context("사용자의 다가오는 예약을 조회하면") {
                it("200 OK와 예약 목록을 반환한다") {
                    val reservations = listOf(createMockReservation(1L))
                    val room = createMockRoom(1L)
                    every { queryReservationUseCase.getUpcomingReservations(email) } returns reservations
                    every { loadRoomPort.loadRoom(RoomId(1L)) } returns room

                    mockMvc.perform(
                        get("/api/reservations/upcoming")
                            .param("email", email)
                    )
                        .andExpect(status().isOk)
                        .andExpect(jsonPath("$.length()").value(1))
                }
            }
        }
    }

    private fun createMockReservation(id: Long): Reservation {
        return Reservation.reconstitute(
            id = ReservationId(id),
            roomId = RoomId(1L),
            userEmail = UserEmail("test@example.com"),
            title = "주간 회의",
            contents = "회의 내용",
            reservationDate = LocalDate.now().plusDays(7),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0),
            status = ReservationStatus.CONFIRMED,
            googleCalendarEventId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            cancelledAt = null,
            participants = emptyList()
        )
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