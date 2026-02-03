package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.common.vo.ValidationResult
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.room.aggregate.Room
import java.time.LocalDate
import java.time.LocalTime

/**
 * 예약 가능 여부 및 충돌 여부를 검증하는 도메인 서비스
 */
interface ReservationAvailabilityValidator {
    fun validateAvailability(context: ValidationContext): ValidationResult
}

interface ReservationConflictValidator {
    fun validateNoConflict(
        existingReservations: List<Reservation>,
        newReservation: Reservation
    ): ValidationResult
}

/**
 * 검증에 필요한 데이터를 담는 컨텍스트 객체
 */
data class ValidationContext(
    val room: Room,
    val date: LocalDate,
    val startTime: LocalTime,
    val endTime: LocalTime,
    val requiredCapacity: Int
)

/**
 * 예약 검증 도메인 서비스
 */
class ReservationValidator(
    private val availabilityRules: List<AvailabilityValidationRule>
) : ReservationAvailabilityValidator, ReservationConflictValidator {

    override fun validateAvailability(context: ValidationContext): ValidationResult {
        availabilityRules.forEach { rule ->
            val result = rule.validate(context)
            if (result.isFailure()) {
                return result
            }
        }
        return ValidationResult.success()
    }

    override fun validateNoConflict(
        existingReservations: List<Reservation>,
        newReservation: Reservation
    ): ValidationResult {
        val conflicts = existingReservations.filter { it.isConflictWith(newReservation) }
        if (conflicts.isNotEmpty()) {
            val conflictTimes = conflicts.joinToString(", ") { "${it.period.startTime} ~ ${it.period.endTime}" }
            return ValidationResult.failure("다음 시간대와 충돌합니다: $conflictTimes")
        }
        return ValidationResult.success()
    }
}

/**
 * 예약 가능 여부 검증 규칙 인터페이스
 */
interface AvailabilityValidationRule {
    fun validate(context: ValidationContext): ValidationResult
}

/**
 * 회의실 사용 가능 상태 검증 규칙
 */
class RoomAvailabilityRule : AvailabilityValidationRule {
    override fun validate(context: ValidationContext): ValidationResult {
        return if (context.room.isAvailable()) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("회의실이 사용 불가능 상태입니다")
        }
    }
}

/**
 * 회의실 수용 인원 검증 규칙
 */
class RoomCapacityRule : AvailabilityValidationRule {
    override fun validate(context: ValidationContext): ValidationResult {
        return if (context.room.canAccommodate(context.requiredCapacity)) {
            ValidationResult.success()
        } else {
            ValidationResult.failure(
                "회의실 수용 인원(${context.room.capacity.value}명)이 부족합니다 (요청: ${context.requiredCapacity}명)"
            )
        }
    }
}

/**
 * 예약 시간 검증 규칙 (영업 시간 내)
 */
class BusinessHoursRule : AvailabilityValidationRule {
    override fun validate(context: ValidationContext): ValidationResult {
        return if (context.room.canBeReservedAt(context.startTime, context.endTime)) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("영업 시간(09:00~18:00) 내에만 예약 가능합니다")
        }
    }
}

/**
 * 예약 날짜 검증 규칙 (과거 날짜 불가)
 */
class ReservationDateRule : AvailabilityValidationRule {
    override fun validate(context: ValidationContext): ValidationResult {
        return if (!context.date.isBefore(LocalDate.now())) {
            ValidationResult.success()
        } else {
            ValidationResult.failure("과거 날짜로 예약할 수 없습니다")
        }
    }
}