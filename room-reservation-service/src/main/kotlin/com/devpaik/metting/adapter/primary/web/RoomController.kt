package com.devpaik.metting.adapter.primary.web


import com.devpaik.metting.adapter.primary.web.docs.RoomAPI
import com.devpaik.metting.adapter.primary.web.dto.CreateRoomRequest
import com.devpaik.metting.adapter.primary.web.dto.RoomResponse
import com.devpaik.metting.adapter.primary.web.dto.UpdateRoomRequest
import com.devpaik.metting.application.room.usecase.CreateRoomUseCase
import com.devpaik.metting.application.room.usecase.DeleteRoomUseCase
import com.devpaik.metting.application.room.usecase.QueryRoomUseCase
import com.devpaik.metting.application.room.usecase.UpdateRoomUseCase
import com.devpaik.metting.application.room.usecase.command.CreateRoomCommand
import com.devpaik.metting.application.room.usecase.command.UpdateRoomCommand
import com.devpaik.metting.application.room.usecase.query.AvailabilityQuery
import com.devpaik.metting.application.room.usecase.query.AvailableRoomQuery
import com.devpaik.metting.application.room.usecase.query.RoomQuery
import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalTime

@RestController
@RequestMapping("/api/rooms")
class RoomController(
    private val createRoomUseCase: CreateRoomUseCase,
    private val updateRoomUseCase: UpdateRoomUseCase,
    private val deleteRoomUseCase: DeleteRoomUseCase,
    private val queryRoomUseCase: QueryRoomUseCase
) : RoomAPI {

    @GetMapping("/{roomId}")
    override fun getRoom(@PathVariable roomId: Long): ResponseEntity<RoomResponse> {
        val room = queryRoomUseCase.getRoom(RoomId(roomId))
        return ResponseEntity.ok(RoomResponse.from(room))
    }

    @GetMapping
    override fun searchRooms(
        @RequestParam(required = false) buildingName: String?,
        @RequestParam(required = false) floor: Long?,
        @RequestParam(required = false) minCapacity: Int?,
        @RequestParam(required = false) status: RoomStatus?
    ): ResponseEntity<List<RoomResponse>> {
        val query = RoomQuery(
            buildingName = buildingName?.let { BuildingName(it) },
            floor = floor?.let { Floor(it) },
            minCapacity = minCapacity,
            status = status
        )

        val rooms = queryRoomUseCase.searchRooms(query)
        return ResponseEntity.ok(rooms.map { RoomResponse.from(it) })
    }


    @GetMapping("/available")
    override fun findAvailableRooms(
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) startTime: LocalTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) endTime: LocalTime,
        @RequestParam(required = false) buildingName: String?,
        @RequestParam(required = false) minCapacity: Int?
    ): ResponseEntity<List<RoomResponse>> {
        val query = AvailableRoomQuery(
            date = date,
            startTime = startTime,
            endTime = endTime,
            buildingName = buildingName?.let { BuildingName(it) },
            minCapacity = minCapacity
        )

        val rooms = queryRoomUseCase.findAvailableRooms(query)
        return ResponseEntity.ok(rooms.map { RoomResponse.from(it) })
    }

    @GetMapping("/check-availability")
    override fun checkAvailability(
        @RequestParam roomId: Long,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) startTime: LocalTime,
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) endTime: LocalTime
    ): ResponseEntity<Map<String, Boolean>> {
        val query = AvailabilityQuery(
            roomId = RoomId(roomId),
            date = date,
            startTime = startTime,
            endTime = endTime
        )

        val isAvailable = queryRoomUseCase.checkAvailability(query)
        return ResponseEntity.ok(mapOf("available" to isAvailable))
    }


    @PostMapping
    override fun createRoom(
        @RequestBody request: CreateRoomRequest
    ): ResponseEntity<RoomResponse> {
        val command = CreateRoomCommand(
            floor = request.floor,
            buildingName = request.buildingName,
            roomName = request.roomName,
            capacity = request.capacity,
            contents = request.contents,
        )

        val room = createRoomUseCase.createRoom(command)
        return ResponseEntity.status(HttpStatus.CREATED)
            .body(RoomResponse.from(room))
    }

    @PutMapping("/{id}")
    override fun updateRoom(
        @PathVariable id: Long,
        @RequestBody request: UpdateRoomRequest
    ): ResponseEntity<RoomResponse> {
        val command = UpdateRoomCommand(
            roomId = RoomId(id),
            buildingName = request.buildingName,
            roomName = request.roomName,
            capacity = request.capacity,
            contents = request.contents,
            makeUnavailable = request.makeUnavailable,
            unavailableReason = request.unavailableReason,
            startMaintenance = request.startMaintenance,
            makeAvailable = request.makeAvailable
        )

        val room = updateRoomUseCase.updateRoom(command)
        return ResponseEntity.ok(RoomResponse.from(room))
    }

    @DeleteMapping("/{id}")
    override fun deleteRoom(@PathVariable id: Long): ResponseEntity<Void> {
        deleteRoomUseCase.deleteRoom(RoomId(id))
        return ResponseEntity.noContent().build()
    }




}