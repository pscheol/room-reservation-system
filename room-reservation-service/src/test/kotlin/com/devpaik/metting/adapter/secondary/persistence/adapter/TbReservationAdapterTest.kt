package com.devpaik.metting.adapter.secondary.persistence.adapter

import com.devpaik.metting.adapter.secondary.persistence.entity.TbReservation
import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.adapter.secondary.persistence.mapper.ReservationMapper
import com.devpaik.metting.adapter.secondary.persistence.repository.TbReservationRepository
import com.devpaik.metting.adapter.secondary.persistence.repository.TbRoomRepository
import com.devpaik.metting.application.reservation.usecase.query.ReservationQuery
import com.devpaik.metting.domain.common.vo.RoomId
import com.devpaik.metting.domain.common.vo.UserEmail
import com.devpaik.metting.domain.participant.aggregate.Participant
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantEmail
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantId
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.Reservation
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationId
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationPeriod
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationTitle
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.assertions.throwables.shouldThrow
import io.kotest.core.spec.style.BehaviorSpec
import io.kotest.matchers.shouldBe
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.util.*

class TbReservationAdapterTest : BehaviorSpec({

    val tbReservationRepository = mockk<TbReservationRepository>()
    val tbRoomRepository = mockk<TbRoomRepository>()
    val reservationMapper = mockk<ReservationMapper>()

    val adapter = TbReservationAdapter(
        tbReservationRepository,
        tbRoomRepository,
        reservationMapper
    )

    // 테스트 픽스처
    fun createTbRoom(id: Long = 1L) = TbRoom(
        id = id,
        buildingName = "본관",
        floor = 3L,
        roomName = "회의실A",
        capacity = 10,
        contents = "프로젝터 있음",
        roomStatus = RoomStatus.AVAILABLE
    )

    fun createTbReservation(
        id: Long = 1L,
        room: TbRoom,
        userEmail: String = "test@example.com",
        reservationDate: LocalDate = LocalDate.now().plusDays(1),
        startTime: LocalTime = LocalTime.of(10, 0),
        endTime: LocalTime = LocalTime.of(11, 0),
        status: ReservationStatus = ReservationStatus.CONFIRMED
    ): TbReservation {
        return TbReservation(
            id = id,
            room = room,
            userEmail = userEmail,
            title = "주간 회의",
            contents = "회의 내용",
            reservationDate = reservationDate,
            startTime = startTime,
            endTime = endTime,
            status = status,
            googleCalendarEventId = null,
            cancelledAt = null
        )
    }

    fun createDomainReservation(
        id: Long = 1L,
        roomId: Long = 1L,
        userEmail: String = "test@example.com",
        reservationDate: LocalDate = LocalDate.now().plusDays(1),
        startTime: LocalTime = LocalTime.of(10, 0),
        endTime: LocalTime = LocalTime.of(11, 0),
        status: ReservationStatus = ReservationStatus.CONFIRMED,
        participants: List<Participant> = emptyList()
    ) = Reservation(
        id = ReservationId(id),
        roomId = RoomId(roomId),
        userEmail = UserEmail(userEmail),
        title = ReservationTitle("주간 회의"),
        contents = "회의 내용",
        reservationDate = reservationDate,
        period = ReservationPeriod(startTime, endTime),
        status = status,
        googleCalendarEventId = null,
        createdAt = LocalDateTime.now(),
        updatedAt = LocalDateTime.now(),
        cancelledAt = null,
        participants = participants
    )

    fun createParticipant(
        id: Long = 1L,
        email: String = "participant@example.com",
        type: ParticipantType = ParticipantType.ATTENDEE
    ) = Participant(
        id = ParticipantId(id),
        participantEmail = ParticipantEmail(email),
        participantType = type,
        invitedAt = LocalDateTime.now()
    )

    Given("예약 ID가 주어졌을 때") {
        val reservationId = ReservationId(1L)
        val tbRoom = createTbRoom()
        val tbReservation = createTbReservation(room = tbRoom)
        val domainReservation = createDomainReservation()

        When("loadReservation을 호출하면") {
            every { tbReservationRepository.searchReservationById(reservationId.value) } returns Optional.of(tbReservation)
            every { reservationMapper.toDomain(tbReservation) } returns domainReservation

            val result = adapter.loadReservation(reservationId)

            Then("해당 예약 정보를 반환한다") {
                result shouldBe domainReservation
                verify(exactly = 1) { tbReservationRepository.searchReservationById(reservationId.value) }
                verify(exactly = 1) { reservationMapper.toDomain(tbReservation) }
            }
        }

        When("존재하지 않는 예약 ID로 loadReservation을 호출하면") {
            every { tbReservationRepository.searchReservationById(reservationId.value) } returns Optional.empty()

            Then("IllegalStateException이 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    adapter.loadReservation(reservationId)
                }
                exception.message shouldBe "예약을 찾을 수 없습니다: ${reservationId.value}"
            }
        }
    }

    Given("예약 검색 쿼리가 주어졌을 때") {
        val query = ReservationQuery(
            userEmail = "test@example.com",
            roomName = "회의실A",
            buildingName = "본관",
            startDate = LocalDate.now(),
            endDate = LocalDate.now().plusDays(7),
            status = ReservationStatus.CONFIRMED
        )
        val tbRoom = createTbRoom()
        val tbReservation1 = createTbReservation(id = 1L, room = tbRoom)
        val tbReservation2 = createTbReservation(
            id = 2L,
            room = tbRoom,
            reservationDate = LocalDate.now().plusDays(2),
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0)
        )
        val domainReservation1 = createDomainReservation(id = 1L)
        val domainReservation2 = createDomainReservation(
            id = 2L,
            reservationDate = LocalDate.now().plusDays(2),
            startTime = LocalTime.of(14, 0),
            endTime = LocalTime.of(15, 0)
        )

        When("loadReservations를 호출하면") {
            every {
                tbReservationRepository.searchReservations(
                    userEmail = query.userEmail,
                    roomName = query.roomName,
                    buildingName = query.buildingName,
                    startDate = query.startDate,
                    endDate = query.endDate,
                    status = query.status
                )
            } returns listOf(tbReservation1, tbReservation2)
            every { reservationMapper.toDomain(tbReservation1) } returns domainReservation1
            every { reservationMapper.toDomain(tbReservation2) } returns domainReservation2

            val result = adapter.loadReservations(query)

            Then("쿼리 조건에 맞는 예약 목록을 반환한다") {
                result.size shouldBe 2
                result shouldBe listOf(domainReservation1, domainReservation2)
                verify(exactly = 1) {
                    tbReservationRepository.searchReservations(
                        userEmail = query.userEmail,
                        roomName = query.roomName,
                        buildingName = query.buildingName,
                        startDate = query.startDate,
                        endDate = query.endDate,
                        status = query.status
                    )
                }
            }
        }
    }

    Given("사용자 이메일과 시작 날짜가 주어졌을 때") {
        val email = "test@example.com"
        val fromDate = LocalDate.now()
        val tbRoom = createTbRoom()
        val tbReservation = createTbReservation(room = tbRoom, userEmail = email)
        val domainReservation = createDomainReservation(userEmail = email)

        When("loadUpcomingReservations를 호출하면") {
            every { tbReservationRepository.findUpcomingReservations(email, fromDate) } returns listOf(tbReservation)
            every { reservationMapper.toDomain(tbReservation) } returns domainReservation

            val result = adapter.loadUpcomingReservations(email, fromDate)

            Then("예정된 예약 목록을 반환한다") {
                result.size shouldBe 1
                result.first() shouldBe domainReservation
                verify(exactly = 1) { tbReservationRepository.findUpcomingReservations(email, fromDate) }
            }
        }
    }

    Given("회의실 ID, 날짜, 시간이 주어졌을 때") {
        val roomId = RoomId(1L)
        val date = LocalDate.now().plusDays(1)
        val startTime = LocalTime.of(10, 0)
        val endTime = LocalTime.of(11, 0)
        val excludeId = ReservationId(99L)
        val tbRoom = createTbRoom()
        val tbReservation = createTbReservation(
            room = tbRoom,
            reservationDate = date,
            startTime = startTime,
            endTime = endTime
        )
        val domainReservation = createDomainReservation(
            reservationDate = date,
            startTime = startTime,
            endTime = endTime
        )

        When("loadConflictingReservations를 호출하면") {
            every {
                tbReservationRepository.findConflictingReservations(
                    roomId.value,
                    date,
                    startTime,
                    endTime,
                    excludeId.value
                )
            } returns listOf(tbReservation)
            every { reservationMapper.toDomain(tbReservation) } returns domainReservation

            val result = adapter.loadConflictingReservations(roomId, date, startTime, endTime, excludeId)

            Then("충돌하는 예약 목록을 반환한다") {
                result.size shouldBe 1
                result.first() shouldBe domainReservation
                verify(exactly = 1) {
                    tbReservationRepository.findConflictingReservations(
                        roomId.value,
                        date,
                        startTime,
                        endTime,
                        excludeId.value
                    )
                }
            }
        }

        When("excludeReservationId가 null인 경우") {
            every {
                tbReservationRepository.findConflictingReservations(
                    roomId.value,
                    date,
                    startTime,
                    endTime,
                    null
                )
            } returns listOf(tbReservation)
            every { reservationMapper.toDomain(tbReservation) } returns domainReservation

            val result = adapter.loadConflictingReservations(roomId, date, startTime, endTime, null)

            Then("모든 충돌하는 예약을 반환한다") {
                result.size shouldBe 1
                verify(exactly = 1) {
                    tbReservationRepository.findConflictingReservations(
                        roomId.value,
                        date,
                        startTime,
                        endTime,
                        null
                    )
                }
            }
        }
    }

    Given("새로운 예약을 저장할 때") {
        val tbRoom = createTbRoom()
        val participant = createParticipant(email = "organizer@example.com", type = ParticipantType.ORGANIZER)
        val newReservation = Reservation(
            id = null,
            roomId = RoomId(1L),
            userEmail = UserEmail("organizer@example.com"),
            title = ReservationTitle("새 회의"),
            contents = "새 회의 내용",
            reservationDate = LocalDate.now().plusDays(3),
            period = ReservationPeriod(LocalTime.of(9, 0), LocalTime.of(10, 0)),
            status = ReservationStatus.CONFIRMED,
            googleCalendarEventId = null,
            createdAt = LocalDateTime.now(),
            updatedAt = LocalDateTime.now(),
            cancelledAt = null,
            participants = listOf(participant)
        )
        val savedTbReservation = createTbReservation(
            id = 10L,
            room = tbRoom,
            userEmail = "organizer@example.com",
            reservationDate = LocalDate.now().plusDays(3),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0)
        )
        val savedDomainReservation = createDomainReservation(
            id = 10L,
            userEmail = "organizer@example.com",
            reservationDate = LocalDate.now().plusDays(3),
            startTime = LocalTime.of(9, 0),
            endTime = LocalTime.of(10, 0),
            participants = listOf(participant)
        )

        When("saveReservation을 호출하면") {
            every { tbRoomRepository.findById(newReservation.roomId.value) } returns Optional.of(tbRoom)
            every { reservationMapper.toEntity(newReservation, tbRoom) } returns savedTbReservation
            every { tbReservationRepository.save(savedTbReservation) } returns savedTbReservation
            every { reservationMapper.toDomain(savedTbReservation) } returns savedDomainReservation

            val result = adapter.saveReservation(newReservation)

            Then("새로운 예약이 저장되고 반환된다") {
                result shouldBe savedDomainReservation
                verify(exactly = 1) { tbRoomRepository.findById(newReservation.roomId.value) }
                verify(exactly = 1) { reservationMapper.toEntity(newReservation, tbRoom) }
                verify(exactly = 1) { tbReservationRepository.save(savedTbReservation) }
            }
        }

        When("존재하지 않는 회의실 ID로 저장을 시도하면") {
            every { tbRoomRepository.findById(newReservation.roomId.value) } returns Optional.empty()

            Then("IllegalStateException이 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    adapter.saveReservation(newReservation)
                }
                exception.message shouldBe "회의실을 찾을 수 없습니다: ${newReservation.roomId.value}"
            }
        }
    }

    Given("기존 예약을 수정할 때") {
        val tbRoom = createTbRoom()
        val existingTbReservation = createTbReservation(id = 1L, room = tbRoom)
        val existingReservation = createDomainReservation(id = 1L)
        val updatedReservation = existingReservation.copy(
            title = ReservationTitle("수정된 회의"),
            contents = "수정된 내용"
        )

        When("saveReservation을 호출하면") {
            every { tbReservationRepository.findById(updatedReservation.id!!.value) } returns Optional.of(existingTbReservation)
            every { reservationMapper.updateEntity(existingTbReservation, updatedReservation) } returns Unit
            every { tbReservationRepository.save(existingTbReservation) } returns existingTbReservation
            every { reservationMapper.toDomain(existingTbReservation) } returns updatedReservation

            val result = adapter.saveReservation(updatedReservation)

            Then("기존 예약이 수정되고 반환된다") {
                result shouldBe updatedReservation
                verify(exactly = 1) { tbReservationRepository.findById(updatedReservation.id!!.value) }
                verify(exactly = 1) { reservationMapper.updateEntity(existingTbReservation, updatedReservation) }
                verify(exactly = 1) { tbReservationRepository.save(existingTbReservation) }
            }
        }

        When("존재하지 않는 예약을 수정하려고 하면") {
            every { tbReservationRepository.findById(updatedReservation.id!!.value) } returns Optional.empty()

            Then("IllegalStateException이 발생한다") {
                val exception = shouldThrow<IllegalStateException> {
                    adapter.saveReservation(updatedReservation)
                }
                exception.message shouldBe "예약을 찾을 수 없습니다"
            }
        }
    }

    Given("예약 충돌 여부를 확인할 때") {
        val roomId = RoomId(1L)
        val date = LocalDate.now().plusDays(1)
        val startTime = LocalTime.of(10, 0)
        val endTime = LocalTime.of(11, 0)
        val excludeId = ReservationId(99L)

        When("충돌이 존재하면") {
            every {
                tbReservationRepository.existsConflict(
                    roomId.value,
                    date,
                    startTime,
                    endTime,
                    excludeId.value
                )
            } returns true

            val result = adapter.existsConflict(roomId, date, startTime, endTime, excludeId)

            Then("true를 반환한다") {
                result shouldBe true
            }
        }

        When("충돌이 존재하지 않으면") {
            every {
                tbReservationRepository.existsConflict(
                    roomId.value,
                    date,
                    startTime,
                    endTime,
                    excludeId.value
                )
            } returns false

            val result = adapter.existsConflict(roomId, date, startTime, endTime, excludeId)

            Then("false를 반환한다") {
                result shouldBe false
            }
        }

        When("excludeReservationId가 null인 경우") {
            every {
                tbReservationRepository.existsConflict(
                    roomId.value,
                    date,
                    startTime,
                    endTime,
                    null
                )
            } returns true

            val result = adapter.existsConflict(roomId, date, startTime, endTime, null)

            Then("모든 예약과 충돌 여부를 확인한다") {
                result shouldBe true
                verify(exactly = 1) {
                    tbReservationRepository.existsConflict(
                        roomId.value,
                        date,
                        startTime,
                        endTime,
                        null
                    )
                }
            }
        }
    }
})