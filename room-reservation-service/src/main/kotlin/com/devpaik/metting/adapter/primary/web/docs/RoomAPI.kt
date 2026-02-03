package com.devpaik.metting.adapter.primary.web.docs


import com.devpaik.metting.adapter.primary.web.dto.CreateRoomRequest
import com.devpaik.metting.adapter.primary.web.dto.RoomResponse
import com.devpaik.metting.adapter.primary.web.dto.UpdateRoomRequest
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*
import java.time.LocalDate
import java.time.LocalTime

@Tag(
    name = "회의실 관리",
    description = "회의실 등록, 수정, 삭제, 조회 및 가용성 확인 API"
)
interface RoomAPI {

    @Operation(
        summary = "회의실 생성",
        description = """
            새로운 회의실을 등록합니다.
            
            ### 비즈니스 규칙
            - 수용 인원은 1~100명 사이
            - 회의실명은 필수이며 100자 이하
            
            ### 초기 상태
            - 생성 시 자동으로 AVAILABLE(사용가능) 상태로 설정
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "회의실 생성 성공",
                content = [Content(schema = Schema(implementation = RoomResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효성 검증 실패)"
            ),
            ApiResponse(
                responseCode = "404",
                description = "층(Floor)을 찾을 수 없음"
            )
        ]
    )
    @PostMapping
    fun createRoom(
        @Parameter(description = "회의실 생성 요청 정보", required = true)
        @RequestBody request: CreateRoomRequest
    ): ResponseEntity<RoomResponse>

    @Operation(
        summary = "회의실 수정",
        description = """
            기존 회의실 정보를 수정합니다.
            
            ### 수정 가능 항목
            - 회의실명
            - 수용 인원
            - 설명
            - 상태 변경 (사용가능/사용불가/정비중)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "회의실 수정 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            ),
            ApiResponse(
                responseCode = "404",
                description = "회의실을 찾을 수 없음"
            )
        ]
    )
    @PutMapping("/{id}")
    fun updateRoom(
        @Parameter(description = "회의실 ID", required = true, example = "1")
        @PathVariable id: Long,

        @Parameter(description = "회의실 수정 요청 정보", required = true)
        @RequestBody request: UpdateRoomRequest
    ): ResponseEntity<RoomResponse>

    @Operation(
        summary = "회의실 삭제",
        description = """
            회의실을 삭제합니다.
            이용중에는 회의실 삭제 불가능.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "회의실 삭제 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "회의실을 찾을 수 없음"
            )
        ]
    )
    @DeleteMapping("/{id}")
    fun deleteRoom(
        @Parameter(description = "회의실 ID", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<Void>

    @Operation(
        summary = "회의실 상세 조회",
        description = "특정 회의실의 상세 정보를 조회."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "회의실을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{id}")
    fun getRoom(
        @Parameter(description = "회의실 ID", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<RoomResponse>

    @Operation(
        summary = "회의실 검색",
        description = """
            다양한 조건으로 회의실을 검색.
            
            ### 검색 조건
            - buildingId: 특정 건물의 회의실
            - floorId: 특정 층의 회의실
            - minCapacity: 최소 수용 인원
            - status: 회의실 상태
            
            조건을 지정하지 않으면 전체 회의실을 조회.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "검색 성공"
            )
        ]
    )
    @GetMapping
    fun searchRooms(
        @Parameter(description = "건물명", example = "홍길건물")
        @RequestParam(required = false) buildingName: String?,

        @Parameter(description = "회의실 층 수", example = "5")
        @RequestParam(required = false) floor: Long?,

        @Parameter(description = "최소 수용 인원", example = "10")
        @RequestParam(required = false) minCapacity: Int?,

        @Parameter(description = "회의실 상태", schema = Schema(implementation = RoomStatus::class))
        @RequestParam(required = false) status: RoomStatus?
    ): ResponseEntity<List<RoomResponse>>

    @Operation(
        summary = "가용 회의실 조회",
        description = """
            특정 날짜와 시간대에 예약 가능한 회의실 목록을 조회.
            
            ### 조회 로직
            1. 해당 시간대에 예약이 없는 회의실 필터링
            2. AVAILABLE 상태의 회의실만 조회
            3. 최소 수용 인원 조건 적용
            4. 건물 필터 적용 (선택사항)
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 날짜/시간 형식"
            )
        ]
    )
    @GetMapping("/available")
    fun findAvailableRooms(
        @Parameter(description = "예약 날짜", required = true, example = "2026-01-27")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,

        @Parameter(description = "시작 시간", required = true, example = "14:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) startTime: LocalTime,

        @Parameter(description = "종료 시간", required = true, example = "16:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) endTime: LocalTime,

        @Parameter(description = "건물 명", example = "홍길건물")
        @RequestParam(required = false) buildingName: String?,

        @Parameter(description = "최소 수용 인원", example = "10")
        @RequestParam(required = false) minCapacity: Int?
    ): ResponseEntity<List<RoomResponse>>

    @Operation(
        summary = "회의실 예약 가능 여부 확인",
        description = """
            특정 회의실이 해당 시간대에 예약 가능한지 확인.
            
            ### 검증 항목
            1. 회의실 존재 여부
            2. 시간(06:00~23:00) 내 여부
            3. 다른 예약과의 시간 충돌 여부
            
            ### 반환값
            ```json
            {
              "available": true
            }
            ```
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "확인 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청"
            )
        ]
    )
    @GetMapping("/check-availability")
    fun checkAvailability(
        @Parameter(description = "회의실 ID", required = true, example = "1")
        @RequestParam roomId: Long,

        @Parameter(description = "예약 날짜", required = true, example = "2026-01-27")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) date: LocalDate,

        @Parameter(description = "시작 시간", required = true, example = "14:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) startTime: LocalTime,

        @Parameter(description = "종료 시간", required = true, example = "16:00")
        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) endTime: LocalTime
    ): ResponseEntity<Map<String, Boolean>>
}