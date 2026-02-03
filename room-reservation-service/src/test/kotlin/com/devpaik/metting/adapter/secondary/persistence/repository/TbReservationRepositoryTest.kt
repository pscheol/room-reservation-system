package com.devpaik.metting.adapter.secondary.persistence.repository

import com.devpaik.metting.adapter.secondary.persistence.entity.TbParticipant
import com.devpaik.metting.adapter.secondary.persistence.entity.TbReservation
import com.devpaik.metting.adapter.secondary.persistence.entity.TbRoom
import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import io.kotest.matchers.shouldBe
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.extension.ExtendWith
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest
import org.springframework.test.context.junit.jupiter.SpringExtension
import java.time.LocalDate
import java.time.LocalTime

@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@DisplayName("회의실 테이블 단위 테스트")
@ExtendWith(SpringExtension::class)
@DataJpaTest
class TbReservationRepositoryTest {

    @Autowired
    private lateinit var tbRoomRepository: TbRoomRepository

    @Autowired
    private lateinit var reservationRepository: TbReservationRepository

    private lateinit var room: TbRoom
    private lateinit var reservation1: TbReservation
    private lateinit var reservation2: TbReservation

    @BeforeEach
    fun setUp() {
        room = TbRoom(buildingName = "건물 A", floor = 2, roomName = "대회의실 1", capacity = 10, contents = "회의실1", roomStatus = RoomStatus.AVAILABLE)
        tbRoomRepository.save(room)

        reservation1 = TbReservation(
            room = room,
            userEmail = "test1@test.com",
            title = "세미나 미팅",
            contents = "세미나 미팅",
            status = ReservationStatus.CONFIRMED,
            reservationDate = LocalDate.now(),
            startTime = LocalTime.of(10, 0),
            endTime = LocalTime.of(11, 0)
        )

        val participant = TbParticipant(
            reservation = reservation1,
            participantEmail = "test1@test.com",
            participantType = ParticipantType.ORGANIZER
        )

        val participant2 = TbParticipant(
            reservation = reservation1,
            participantEmail = "test2@test.com",
            participantType = ParticipantType.ATTENDEE
        )

        reservation1.participants.add(participant)
        reservation1.participants.add(participant2)
        reservationRepository.save(reservation1)


        reservation2 = TbReservation(
            room = room,
            userEmail = "test3@test.com",
            title = "중간결산 미팅",
            contents = "중간겨산 미팅",
            status = ReservationStatus.CONFIRMED,
            reservationDate = LocalDate.now(),
            startTime = LocalTime.of(12, 0),
            endTime = LocalTime.of(13, 0)
        )

        val participant3 = TbParticipant(
            reservation = reservation2,
            participantEmail = "test2@test.com",
            participantType = ParticipantType.ORGANIZER
        )
        val participant4 = TbParticipant(
            reservation = reservation2,
            participantEmail = "test3@test.com",
            participantType = ParticipantType.ATTENDEE
        )

        reservation2.participants.add(participant3)
        reservation2.participants.add(participant4)

        reservationRepository.save(reservation2)
    }

    @Test
    fun `searchReservationById - ID로 예약을 조회한다`() {
        // when
        val foundReservation = reservationRepository.searchReservationById(reservation1.id!!)

        // then
        foundReservation.isPresent shouldBe true
        foundReservation.get() shouldBe reservation1
    }

    @Test
    fun `searchReservations - 여러 조건으로 예약을 검색한다`() {
        // when
        val foundReservations = reservationRepository.searchReservations(
            userEmail = "test1@test.com",
            roomName = "대회의실 1",
            buildingName = "건물 A",
            startDate = LocalDate.now(),
            endDate = LocalDate.now(),
            status = ReservationStatus.CONFIRMED
        )

        // then
        foundReservations.size shouldBe 1
        foundReservations[0] shouldBe reservation1
    }

    @Test
    fun `findConflictingReservations - 충돌하는 예약을 조회한다`() {
        // when
        val conflictingReservations = reservationRepository.findConflictingReservations(
            roomId = room.id!!,
            date = LocalDate.now(),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30),
            excludeId = null
        )

        // then
        conflictingReservations.size shouldBe 1
        conflictingReservations[0] shouldBe reservation1
    }

    @Test
    fun `findUpcomingReservations - 다가오는 예약을 조회한다`() {
        // when
        val upcomingReservations = reservationRepository.findUpcomingReservations(
            email = "test2@test.com",
            fromDate = LocalDate.now()
        )

        // then
        upcomingReservations.size shouldBe 1
        upcomingReservations[0] shouldBe reservation2
    }



    @Test
    fun `existsConflict - 충돌이 존재할 때 true를 반환한다`() {
        // when
        val exists = reservationRepository.existsConflict(
            roomId = room.id!!,
            date = LocalDate.now(),
            startTime = LocalTime.of(10, 30),
            endTime = LocalTime.of(11, 30),
            excludeId = null
        )

        // then
        exists shouldBe true
    }

    @Test
    fun `existsConflict - 충돌이 존재하지 않을 때 false를 반환한다`() {
        // when
        val exists = reservationRepository.existsConflict(
            roomId = room.id!!,
            date = LocalDate.now(),
            startTime = LocalTime.of(13, 0),
            endTime = LocalTime.of(14, 0),
            excludeId = null
        )

        // then
        exists shouldBe false
    }
}