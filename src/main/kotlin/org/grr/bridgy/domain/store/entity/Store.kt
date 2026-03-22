package org.grr.bridgy.domain.store.entity

import jakarta.persistence.*
import java.time.LocalDateTime
import java.time.LocalTime

@Entity
@Table(name = "stores")
class Store(

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @Column(nullable = false, length = 100)
    var name: String,

    @Column(nullable = false, length = 50)
    var category: String,

    @Column(nullable = false, length = 500)
    var address: String,

    @Column(nullable = false, length = 20)
    var phone: String,

    @Column(length = 1000)
    var description: String? = null,

    @Column(name = "open_time")
    var openTime: LocalTime? = null,

    @Column(name = "close_time")
    var closeTime: LocalTime? = null,

    @ElementCollection
    @CollectionTable(name = "store_closed_days", joinColumns = [JoinColumn(name = "store_id")])
    @Column(name = "day_of_week")
    var closedDays: MutableList<String> = mutableListOf(),

    @Column(name = "kakao_channel_id", length = 100)
    var kakaoChannelId: String? = null,

    @Column(name = "owner_email", nullable = false, length = 100)
    var ownerEmail: String,

    @Column(name = "created_at", nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now(),

    @Column(name = "updated_at")
    var updatedAt: LocalDateTime = LocalDateTime.now()
)
