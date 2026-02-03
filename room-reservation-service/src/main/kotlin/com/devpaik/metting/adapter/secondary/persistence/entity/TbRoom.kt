package com.devpaik.metting.adapter.secondary.persistence.entity

import com.devpaik.metting.domain.room.aggregate.vo.RoomStatus
import jakarta.persistence.*
import org.hibernate.annotations.CreationTimestamp
import org.hibernate.annotations.UpdateTimestamp
import java.time.LocalDateTime

@Entity
@Table(name = "tb_room")
class TbRoom(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long? = null,

    @Column(name = "building_name", nullable = false, length = 100)
    var buildingName: String,

    @Column(name = "floor", nullable = false)
    var floor: Long,

    @Column(name = "room_name", nullable = false, length = 100)
    var roomName: String,

    @Column(name = "capacity", nullable = false)
    var capacity: Int,

    @Column(name = "contents", length = 500)
    var contents: String?,

    @Enumerated(EnumType.STRING)
    @Column(name = "room_status", nullable = false, length = 20)
    var roomStatus: RoomStatus,

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime? = null,

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    var updatedAt: LocalDateTime? = null,
)