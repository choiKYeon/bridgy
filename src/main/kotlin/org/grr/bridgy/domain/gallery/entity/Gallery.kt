package org.grr.bridgy.domain.gallery.entity

import jakarta.persistence.*
import org.grr.bridgy.domain.pet.entity.Pet
import java.time.LocalDateTime

@Entity
@Table(name = "galleries")
class Gallery(
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0,

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pet_id", nullable = false)
    val pet: Pet,

    @Column(nullable = false)
    var imageUrl: String,

    @Column(length = 500)
    var caption: String? = null,

    @Column(nullable = false, updatable = false)
    val createdAt: LocalDateTime = LocalDateTime.now()
)
