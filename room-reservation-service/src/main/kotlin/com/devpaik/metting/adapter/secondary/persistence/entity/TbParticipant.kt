package com.devpaik.metting.adapter.secondary.persistence.entity

import com.devpaik.metting.domain.participant.aggregate.vo.ParticipantType
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import java.time.LocalDateTime

@Entity
@Table(
    name = "tb_reservation_participant",
    uniqueConstraints = [
        UniqueConstraint(name = "uq_reservation_user", columnNames = ["reservation_id", "participant_email"])
    ]
)
class TbParticipant(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reservation_id", nullable = false)
    var reservation: TbReservation,

    @Column(name = "participant_email", nullable = false, length = 100)
    var participantEmail: String,

    @Enumerated(EnumType.STRING)
    @Column(name = "participant_type", nullable = false, length = 20)
    var participantType: ParticipantType = ParticipantType.ATTENDEE,

    @Column(name = "invited_at", nullable = false)
    val invitedAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "responded_at")
    var respondedAt: LocalDateTime? = null,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null
)