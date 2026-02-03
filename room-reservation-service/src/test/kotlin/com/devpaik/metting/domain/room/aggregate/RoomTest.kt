package com.devpaik.metting.domain.room.aggregate

import com.devpaik.metting.domain.common.vo.Floor
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.room.aggregate.vo.BuildingName
import com.devpaik.metting.domain.room.aggregate.vo.RoomCapacity
import com.devpaik.metting.domain.room.aggregate.vo.RoomName
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import java.time.LocalDateTime
import java.time.LocalTime

class RoomTest : BehaviorSpec({

    Given("회의실 생성") {
        When("유효한 정보로 회의실을 생성하면") {
            val room = Room.create(
                floor = 3L,
                buildingName = "본관",
                roomName = "회의실A",
                capacity = 10,
                contents = "프로젝터 있음"
            )

            Then("회의실이 생성되고 AVAILABLE 상태가 된다") {
                room.id shouldBe null
                room.floor shouldBe Floor(3L)
                room.buildingName shouldBe BuildingName("본관")
                room.roomName shouldBe RoomName("회의실A")
                room.capacity shouldBe RoomCapacity(10)
                room.contents shouldBe "프로젝터 있음"
                room.status shouldBe RoomStatus.AVAILABLE
                room.createdAt shouldNotBe null
            }
        }
    }

    Given("회의실 reconstitute") {
        When("영속화된 데이터로 회의실을 복원하면") {
            val now = LocalDateTime.now()
            val room = Room.reconstitute(
                id = RoomId(1L),
                buildingName = BuildingName("본관"),
                floor = Floor(3L),
                roomName = "회의실A",
                capacity = 10,
                contents = "프로젝터 있음",
                status = RoomStatus.AVAILABLE,
                createdAt = now,
                updatedAt = now
            )

            Then("모든 필드가 정확히 복원된다") {
                room.id shouldBe RoomId(1L)
                room.buildingName shouldBe BuildingName("본관")
                room.floor shouldBe Floor(3L)
                room.roomName shouldBe RoomName("회의실A")
                room.capacity shouldBe RoomCapacity(10)
                room.status shouldBe RoomStatus.AVAILABLE
            }
        }
    }

    Given("회의실 정보 수정") {
        val room = createAvailableRoom()

        When("회의실 정보를 수정하면") {
            val updatedRoom = room.update(
                buildingName = "별관",
                roomName = "대회의실",
                capacity = 20,
                contents = "빔프로젝터, 화이트보드"
            )

            Then("새로운 불변 객체가 반환된다") {
                updatedRoom shouldNotBe room
                updatedRoom.buildingName shouldBe BuildingName("별관")
                updatedRoom.roomName shouldBe RoomName("대회의실")
                updatedRoom.capacity shouldBe RoomCapacity(20)
                updatedRoom.contents shouldBe "빔프로젝터, 화이트보드"
            }
        }
    }

    Given("회의실 사용 가능 여부 확인") {
        When("AVAILABLE 상태일 때") {
            val room = createAvailableRoom()

            Then("isAvailable()이 true를 반환한다") {
                room.isAvailable() shouldBe true
            }
        }

        When("UNAVAILABLE 상태일 때") {
            val room = createAvailableRoom().makeUnavailable("청소 중")

            Then("isAvailable()이 false를 반환한다") {
                room.isAvailable() shouldBe false
            }
        }

        When("MAINTENANCE 상태일 때") {
            val room = createAvailableRoom().startMaintenance("에어컨 수리")

            Then("isAvailable()이 false를 반환한다") {
                room.isAvailable() shouldBe false
            }
        }
    }

    Given("회의실 수용 인원 확인") {
        val room = createAvailableRoom(capacity = 10)

        When("요청 인원이 수용 인원 이하일 때") {
            Then("canAccommodate()가 true를 반환한다") {
                room.canAccommodate(10) shouldBe true
                room.canAccommodate(5) shouldBe true
            }
        }

        When("요청 인원이 수용 인원 초과일 때") {
            Then("canAccommodate()가 false를 반환한다") {
                room.canAccommodate(11) shouldBe false
                room.canAccommodate(20) shouldBe false
            }
        }
    }

    Given("회의실 사용 불가 처리") {
        val room = createAvailableRoom()

        When("사용 불가 처리를 하면") {
            val unavailableRoom = room.makeUnavailable("청소 중")

            Then("UNAVAILABLE 상태가 된다") {
                unavailableRoom.status shouldBe RoomStatus.UNAVAILABLE
                unavailableRoom.contents shouldBe "청소 중"
            }
        }

        When("이미 MAINTENANCE 상태인 회의실을 사용 불가 처리하면") {
            val maintenanceRoom = room.startMaintenance("수리 중")

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    maintenanceRoom.makeUnavailable("청소")
                }
                exception.message shouldBe "이미 정비 중인 회의실입니다"
            }
        }
    }

    Given("회의실 정비 시작") {
        val room = createAvailableRoom()

        When("정비를 시작하면") {
            val maintenanceRoom = room.startMaintenance("에어컨 수리")

            Then("MAINTENANCE 상태가 된다") {
                maintenanceRoom.status shouldBe RoomStatus.MAINTENANCE
                maintenanceRoom.contents shouldBe "에어컨 수리"
            }
        }

        When("빈 사유로 정비를 시작하면") {
            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    room.startMaintenance("")
                }
                exception.message shouldBe "정비 사유는 필수입니다"
            }
        }
    }

    Given("회의실 사용 가능 처리") {
        val unavailableRoom = createAvailableRoom().makeUnavailable("청소 중")

        When("사용 가능 처리를 하면") {
            val availableRoom = unavailableRoom.makeAvailable()

            Then("AVAILABLE 상태가 된다") {
                availableRoom.status shouldBe RoomStatus.AVAILABLE
            }
        }

        When("이미 AVAILABLE 상태인 회의실을 사용 가능 처리하면") {
            val room = createAvailableRoom()

            Then("예외가 발생한다") {
                val exception = shouldThrow<IllegalArgumentException> {
                    room.makeAvailable()
                }
                exception.message shouldBe "이미 사용 가능한 회의실입니다"
            }
        }
    }

    Given("특정 시간대 예약 가능 여부") {
        val room = createAvailableRoom()

        When("영업 시간(06:00~23:00) 내에 예약하면") {
            Then("canBeReservedAt()이 true를 반환한다") {
                room.canBeReservedAt(LocalTime.of(9, 0), LocalTime.of(10, 0)) shouldBe true
                room.canBeReservedAt(LocalTime.of(6, 0), LocalTime.of(23, 0)) shouldBe true
            }
        }

        When("영업 시간 외에 예약하면") {
            Then("canBeReservedAt()이 false를 반환한다") {
                room.canBeReservedAt(LocalTime.of(5, 0), LocalTime.of(6, 0)) shouldBe false
                room.canBeReservedAt(LocalTime.of(23, 0), LocalTime.of(23, 30)) shouldBe false
            }
        }

        When("UNAVAILABLE 상태에서 예약하려 하면") {
            val unavailableRoom = room.makeUnavailable("청소 중")

            Then("canBeReservedAt()이 false를 반환한다") {
                unavailableRoom.canBeReservedAt(LocalTime.of(9, 0), LocalTime.of(10, 0)) shouldBe false
            }
        }
    }

    Given("회의실 적합성 검증") {
        val room = createAvailableRoom(capacity = 10)

        When("요청 인원이 수용 가능하면") {
            Then("isSuitableFor()가 true를 반환한다") {
                room.isSuitableFor(5) shouldBe true
                room.isSuitableFor(10) shouldBe true
            }
        }

        When("요청 인원이 수용 불가능하면") {
            Then("isSuitableFor()가 false를 반환한다") {
                room.isSuitableFor(11) shouldBe false
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