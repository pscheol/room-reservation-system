package com.devpaik.metting.adapter.primary.web

import com.devpaik.metting.adapter.primary.web.docs.ReservationAPI
import com.devpaik.metting.adapter.primary.web.dto.CreateReservationRequest
import com.devpaik.metting.adapter.primary.web.dto.ReservationResponse
import com.devpaik.metting.adapter.primary.web.dto.UpdateReservationRequest
import com.devpaik.metting.application.reservation.usecase.CreateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.QueryReservationUseCase
import com.devpaik.metting.application.reservation.usecase.UpdateReservationUseCase
import com.devpaik.metting.application.reservation.usecase.command.CancelReservationCommand
import com.devpaik.metting.application.reservation.usecase.command.UpdateReservationCommand
import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.application.room.output.LoadRoomPort
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate

@RestController
@RequestMapping("/api/reservations")
class ReservationController(
    private val createReservationUseCase: CreateReservationUseCase,
    private val updateReservationUseCase: UpdateReservationUseCase,
    private val queryReservationUseCase: QueryReservationUseCase,
    private val loadRoomPort: LoadRoomPort
) : ReservationAPI {

    @PostMapping
    override fun createReservation(
        @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val command = request.toCommand()
        val pairReservation = createReservationUseCase.createReservation(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(ReservationResponse.from(pairReservation.first, pairReservation.second))
    }

    @PutMapping("/{id}")
    override fun updateReservation(
        @PathVariable id: Long,
        @RequestBody request: UpdateReservationRequest
    ): ResponseEntity<ReservationResponse> {
        val command = UpdateReservationCommand(
            reservationId = ReservationId(id),
            title = request.title,
            contents = request.contents,
            reservationDate = request.reservationDate,
            startTime = request.startTime,
            endTime = request.endTime,
            status = request.status
        )
        val reservation = updateReservationUseCase.updateReservation(command)
        val room = loadRoomPort.loadRoom(reservation.roomId)
        return ResponseEntity.ok(ReservationResponse.from(reservation, room))
    }

    @DeleteMapping("/{id}")
    override fun cancelReservation(
        @PathVariable id: Long,
        @RequestParam email: String
    ): ResponseEntity<ReservationResponse> {
        val command = CancelReservationCommand(
            reservationId = ReservationId(id),
            userEmail = UserEmail(email)
        )
        val reservation = updateReservationUseCase.cancelReservation(command)
        val room = loadRoomPort.loadRoom(reservation.roomId)
        return ResponseEntity.ok(ReservationResponse.from(reservation, room))
    }

    @GetMapping("/{id}")
    override fun getReservation(@PathVariable id: Long): ResponseEntity<ReservationResponse> {
        val reservation = queryReservationUseCase.getReservation(ReservationId(id))
        val room = loadRoomPort.loadRoom(reservation.roomId)

        return ResponseEntity.ok(ReservationResponse.from(reservation, room))
    }

    @GetMapping
    override fun searchReservations(
        @RequestParam(required = false) email: String?,
        @RequestParam(required = false) roomName: String?,
        @RequestParam(required = false) buildingName: String?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,
        @RequestParam(required = false) status: ReservationStatus?
    ): ResponseEntity<List<ReservationResponse>> {
        val query = ReservationQuery(
            userEmail = email,
            roomName = roomName,
            buildingName = buildingName,
            startDate = startDate,
            endDate = endDate,
            status = status
        )
        val reservations = queryReservationUseCase.searchReservations(query)

        return ResponseEntity.ok(toReservationWebResponseList(reservations))
    }

    @GetMapping("/upcoming")
    override fun getUpcomingReservations(
        @RequestParam email: String
    ): ResponseEntity<List<ReservationResponse>> {
        val reservations = queryReservationUseCase.getUpcomingReservations(email)

        return ResponseEntity.ok(toReservationWebResponseList(reservations))
    }

    private fun toReservationWebResponseList(reservations: List<Reservation>): List<ReservationResponse> {
        if (reservations.isEmpty()) {
            return emptyList()
        }

        // N+1 문제를 피하기 위해 Room 정보를 미리 조회
        val roomIds = reservations.map { it.roomId }.distinct()
        val rooms = roomIds.map { loadRoomPort.loadRoom(it) }.associateBy { it.id }

        return reservations.map { reservation ->
            val room = rooms[reservation.roomId]
                ?: throw IllegalStateException("Room not found for reservation: ${reservation.roomId.value}")
            ReservationResponse.from(reservation, room)
        }
    }
}