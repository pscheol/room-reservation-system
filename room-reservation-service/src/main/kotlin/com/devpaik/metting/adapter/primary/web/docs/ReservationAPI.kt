package com.devpaik.metting.adapter.primary.web.docs

import com.devpaik.metting.adapter.primary.web.dto.CreateReservationRequest
import com.devpaik.metting.adapter.primary.web.dto.ReservationResponse
import com.devpaik.metting.adapter.primary.web.dto.UpdateReservationRequest
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
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

@Tag(
    name = "예약 관리",
    description = "회의실 예약 생성, 수정, 취소, 조회 API"
)
interface ReservationAPI {

    @Operation(
        summary = "예약 생성",
        description = """
            새로운 회의실 예약을 생성.
            
            ### 규칙
            - 예약 시간은 30분 이상, 최대 12시간 이하만 가능
            - 동일 시간대에 중복 예약은 불가능 하다.
            - 3개월 이후로는 예약할 수 없다.
            
            ### 검증 항목
            1. 회의실 존재 및 사용 가능 여부
            2. 수용 인원 충족 여부
            3. 시간대 충돌 여부
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "예약 생성 성공",
                content = [Content(schema = Schema(implementation = ReservationResponse::class))]
            ),
            ApiResponse(
                responseCode = "400",
                description = "잘못된 요청 (유효성 검증 실패)"
            ),
            ApiResponse(
                responseCode = "404",
                description = "회의실 또는 사용자를 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "409",
                description = "예약 시간 충돌"
            )
        ]
    )
    @PostMapping
    fun createReservation(
        @Parameter(description = "예약 생성 요청 정보", required = true)
        @RequestBody request: CreateReservationRequest
    ): ResponseEntity<ReservationResponse>

    @Operation(
        summary = "예약 수정",
        description = """
            기존 예약 정보를 수정.
            
            ### 제약사항
            - 취소된 예약은 수정할 수 없다.
            - 완료된 예약은 수정할 수 없다.
            - 시간 변경 시 충돌 여부를 다시 검증
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 수정 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "예약을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "409",
                description = "수정된 시간에 다른 예약 존재"
            )
        ]
    )
    @PutMapping("/{id}")
    fun updateReservation(
        @Parameter(description = "예약 ID", required = true, example = "1")
        @PathVariable id: Long,

        @Parameter(description = "예약 수정 요청 정보", required = true)
        @RequestBody request: UpdateReservationRequest
    ): ResponseEntity<ReservationResponse>

    @Operation(
        summary = "예약 취소",
        description = """
            예약을 취소.
            
            ### 규칙
            - 예약자만 취소 가능
            - 예약 시작 10분 전까지만 취소 가능
            - 이미 취소된 예약은 다시 취소할 수 없다.
            - 완료된 예약은 취소할 수 없다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "예약 취소 성공"
            ),
            ApiResponse(
                responseCode = "400",
                description = "취소 불가능한 상태"
            ),
            ApiResponse(
                responseCode = "403",
                description = "취소 권한 없음"
            ),
            ApiResponse(
                responseCode = "404",
                description = "예약을 찾을 수 없음"
            )
        ]
    )
    @DeleteMapping("/{id}")
    fun cancelReservation(
        @Parameter(description = "예약 ID", required = true, example = "1")
        @PathVariable id: Long,

        @Parameter(description = "취소 요청 사용자 메일", required = true, example = "hello@gmail.com")
        @RequestParam email: String
    ): ResponseEntity<ReservationResponse>

    @Operation(
        summary = "예약 상세 조회",
        description = "특정 예약의 상세 정보를 조회."
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "예약을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{id}")
    fun getReservation(
        @Parameter(description = "예약 ID", required = true, example = "1")
        @PathVariable id: Long
    ): ResponseEntity<ReservationResponse>

    @Operation(
        summary = "예약 검색",
        description = """
            다양한 조건으로 예약을 검색.
            
            ### 검색 조건
            - email: 특정 사용자의 예약 조회
            - roomName: 특정 회의실 예약 조회
            - buildingName: 특정 건물 예약 조회
            - startDate ~ endDate: 기간별 조회
            - status: 예약 상태별 조회
            
            조건을 지정하지 않으면 전체 예약을 조회.
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
    fun searchReservations(
        @Parameter(description = "사용자 email", example = "hello@gmail.com")
        @RequestParam(required = false) email: String?,

        @Parameter(description = "회의실 명", example = "대회의실 A")
        @RequestParam(required = false) roomName: String?,

        @Parameter(description = "건물 명", example = "홍길건물")
        @RequestParam(required = false) buildingName: String?,

        @Parameter(description = "시작 날짜", example = "2026-01-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) startDate: LocalDate?,

        @Parameter(description = "종료 날짜", example = "2026-01-31")
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) endDate: LocalDate?,

        @Parameter(description = "예약 상태", schema = Schema(implementation = ReservationStatus::class))
        @RequestParam(required = false) status: ReservationStatus?
    ): ResponseEntity<List<ReservationResponse>>

    @Operation(
        summary = "다가오는 예약 조회",
        description = """
            특정 사용자의 다가오는 예약 목록을 조회합니다.
            
            오늘 날짜 이후의 확정(CONFIRMED) 또는 대기(PENDING) 상태의 예약만 조회됩니다.
        """
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "조회 성공"
            )
        ]
    )
    @GetMapping("/upcoming")
    fun getUpcomingReservations(
        @Parameter(description = "사용자 email", required = true, example = "hello@gmail.com")
        @RequestParam email: String
    ): ResponseEntity<List<ReservationResponse>>
}
