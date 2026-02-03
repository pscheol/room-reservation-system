package com.devpaik.metting.domain.reservation.service

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.room.aggregate.Room
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

class ReservationValidatorTest : BehaviorSpec({

    val validator = ReservationValidator(
        listOf(
            RoomAvailabilityRule(),
            RoomCapacityRule(),
            BusinessHoursRule(),
            ReservationDateRule()
        )
    )

    Given("예약 가능 여부 검증") {
        When("모든 조건을 만족하면") {
            val context = ValidationContext(
                room = createAvailableRoom(capacity = 10),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = validator.validateAvailability(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("회의실이 사용 불가 상태이면") {
            val context = ValidationContext(
                room = createUnavailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = validator.validateAvailability(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
                result.getFailureReason() shouldBe "회의실이 사용 불가능 상태입니다"
            }
        }

        When("수용 인원이 부족하면") {
            val context = ValidationContext(
                room = createAvailableRoom(capacity = 5),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 10
            )

            val result = validator.validateAvailability(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
                result.getFailureReason() shouldBe "회의실 수용 인원(5명)이 부족합니다 (요청: 10명)"
            }
        }

        When("과거 날짜로 예약하면") {
            val context = ValidationContext(
                room = createAvailableRoom(capacity = 10),
                date = LocalDate.now().minusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = validator.validateAvailability(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
                result.getFailureReason() shouldBe "과거 날짜로 예약할 수 없습니다"
            }
        }
    }

    Given("시간 충돌 검증") {
        When("충돌하는 예약이 없으면") {
            val existingReservations = listOf(
                createReservation(
                    startTime = LocalTime.of(9, 0),
                    endTime = LocalTime.of(10, 0)
                )
            )
            val newReservation = createReservation(
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            val result = validator.validateNoConflict(existingReservations, newReservation)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("충돌하는 예약이 있으면") {
            val existingReservations = listOf(
                createReservation(
                    startTime = LocalTime.of(10, 0),
                    endTime = LocalTime.of(11, 0)
                )
            )
            val newReservation = createReservation(
                startTime = LocalTime.of(10, 30),
                endTime = LocalTime.of(11, 30)
            )

            val result = validator.validateNoConflict(existingReservations, newReservation)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
                result.getFailureReason()?.contains("충돌합니다") shouldBe true
            }
        }

        When("기존 예약이 없으면") {
            val existingReservations = emptyList<Reservation>()
            val newReservation = createReservation(
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0)
            )

            val result = validator.validateNoConflict(existingReservations, newReservation)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }
    }
})

class RoomAvailabilityRuleTest : BehaviorSpec({

    val rule = RoomAvailabilityRule()

    Given("회의실 사용 가능 상태 검증") {
        When("회의실이 AVAILABLE 상태이면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("회의실이 UNAVAILABLE 상태이면") {
            val context = ValidationContext(
                room = createUnavailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
            }
        }
    }
})

class RoomCapacityRuleTest : BehaviorSpec({

    val rule = RoomCapacityRule()

    Given("회의실 수용 인원 검증") {
        When("요청 인원이 수용 가능하면") {
            val context = ValidationContext(
                room = createAvailableRoom(capacity = 10),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 10
            )

            val result = rule.validate(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("요청 인원이 초과하면") {
            val context = ValidationContext(
                room = createAvailableRoom(capacity = 5),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 10
            )

            val result = rule.validate(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
            }
        }
    }
})

class BusinessHoursRuleTest : BehaviorSpec({

    val rule = BusinessHoursRule()

    Given("영업 시간 검증") {
        When("영업 시간(06:00~23:00) 내 예약이면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(18, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("영업 시간 외 예약이면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(5, 0),
                endTime = LocalTime.of(6, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
            }
        }
    }
})

class ReservationDateRuleTest : BehaviorSpec({

    val rule = ReservationDateRule()

    Given("예약 날짜 검증") {
        When("미래 날짜로 예약하면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now().plusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("오늘 날짜로 예약하면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now(),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 성공한다") {
                result.isSuccess() shouldBe true
            }
        }

        When("과거 날짜로 예약하면") {
            val context = ValidationContext(
                room = createAvailableRoom(),
                date = LocalDate.now().minusDays(1),
                startTime = LocalTime.of(10, 0),
                endTime = LocalTime.of(11, 0),
                requiredCapacity = 5
            )

            val result = rule.validate(context)

            Then("검증이 실패한다") {
                result.isFailure() shouldBe true
            }
        }
    }
})

private fun createAvailableRoom(capacity: Int = 10): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = capacity,
        contents = "프로젝터 있음",
        status = RoomStatus.AVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createUnavailableRoom(): Room {
    return Room.reconstitute(
        id = RoomId(1L),
        buildingName = BuildingName("본관"),
        floor = Floor(3L),
        roomName = "회의실A",
        capacity = 10,
        contents = "청소 중",
        status = RoomStatus.UNAVAILABLE,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now()
    )
}

private fun createReservation(
    startTime: LocalTime,
    endTime: LocalTime,
    date: LocalDate = LocalDate.now().plusDays(1)
): Reservation {
    return Reservation.reconstitute(
        id = ReservationId(1L),
        roomId = RoomId(1L),
        userEmail = UserEmail("test@example.com"),
        title = "테스트 회의",
        contents = null,
        reservationDate = date,
        startTime = startTime,
        endTime = endTime,
        status = ReservationStatus.CONFIRMED,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = emptyList()
    )
}