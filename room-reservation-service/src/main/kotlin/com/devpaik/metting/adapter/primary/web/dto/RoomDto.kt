package com.devpaik.metting.adapter.primary.web.dto

import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.swagger.v3.oas.annotations.media.Schema
import java.time.LocalDateTime

@Schema(description = "회의실 생성 요청")
data class CreateRoomRequest(
    @Schema(description = "건물명", example = "홍길건물", required = true)
    val buildingName: String,

    @Schema(description = "층 수", example = "5", required = true)
    val floor: Long,

    @Schema(description = "회의실명", example = "대회의실 A", required = true)
    val roomName: String,

    @Schema(description = "수용 인원", example = "20", required = true)
    val capacity: Int,

    @Schema(description = "회의실 설명", example = "프로젝터, 화이트보드 구비")
    val contents: String?
)

@Schema(description = "회의실 수정 요청")
data class UpdateRoomRequest(
    @Schema(description = "회의실명", example = "중회의실 B", required = true)
    val roomName: String,

    @Schema(description = "건물명", example = "홍길건물", required = true)
    val buildingName: String,

    @Schema(description = "수용 인원", example = "15", required = true)
    val capacity: Int,

    @Schema(description = "회의실 설명", example = "화상회의 가능")
    val contents: String?,

    @Schema(description = "사용 불가 처리 여부", example = "false")
    val makeUnavailable: Boolean = false,

    @Schema(description = "사용 불가 사유", example = "냉난방 고장")
    val unavailableReason: String? = null,

    @Schema(description = "정비 시작 사유", example = "정기 점검")
    val startMaintenance: String? = null,

    @Schema(description = "사용 가능 처리 여부", example = "false")
    val makeAvailable: Boolean = false
)

@Schema(description = "회의실 응답")
data class RoomResponse(
    @Schema(description = "회의실 ID", example = "1")
    val id: Long,

    @Schema(description = "건물명", example = "홍길건물")
    val buildingName: String,

    @Schema(description = "회의실 위치 층수", example = "5")
    val floor: Long,

    @Schema(description = "회의실명", example = "대회의실 A")
    val roomName: String,

    @Schema(description = "수용 인원", example = "20")
    val capacity: Int,

    @Schema(description = "회의실 설명", example = "프로젝터, 화이트보드 구비")
    val contents: String?,

    @Schema(description = "회의실 상태", example = "AVAILABLE")
    val status: RoomStatus,

    @Schema(description = "생성 일시", example = "2026-01-20T10:00:00")
    val createdAt: LocalDateTime?,

    @Schema(description = "수정 일시", example = "2026-01-20T10:00:00")
    val updatedAt: LocalDateTime?
) {
    companion object {
        fun from(room: Room): RoomResponse {
            return RoomResponse(
                id = room.id!!.value,
                buildingName = room.buildingName.value,
                floor = room.floor.value,
                roomName = room.roomName.value,
                capacity = room.capacity.value,
                contents = room.contents,
                status = room.status,
                createdAt = room.createdAt,
                updatedAt = room.updatedAt
            )
        }
    }
}