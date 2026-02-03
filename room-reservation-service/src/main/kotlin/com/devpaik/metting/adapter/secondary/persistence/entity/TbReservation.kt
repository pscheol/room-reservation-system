package com.devpaik.metting.adapter.secondary.persistence.entity

import com.devpaik.metting.domain.reservation.aggregate.vo.ReservationStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "tb_room_reservation")
class TbReservation(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

//    @Column(name = "room_id", nullable = false)
//    var roomId: Long,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", referencedColumnName = "id")
    val room: TbRoom,

    @Column(name = "user_email", nullable = false, length = 100)
    var userEmail: String,

    @Column(name = "title", nullable = false, length = 200)
    var title: String,

    @Column(name = "contents", columnDefinition = "TEXT")
    var contents: String?,

    @Column(name = "reservation_date", nullable = false)
    var reservationDate: LocalDate,

    @Column(name = "start_time", nullable = false)
    var startTime: LocalTime,

    @Column(name = "end_time", nullable = false)
    var endTime: LocalTime,

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: ReservationStatus = ReservationStatus.CONFIRMED,

    @Column(name = "google_calendar_event_id", length = 500)
    var googleCalendarEventId: String? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,

    @Column(name = "cancelled_at")
    var cancelledAt: LocalDateTime? = null,

    @OneToMany(mappedBy = "reservation", cascade = [CascadeType.ALL], orphanRemoval = true, fetch = FetchType.LAZY)
    val participants: MutableList<TbParticipant> = mutableListOf()
)