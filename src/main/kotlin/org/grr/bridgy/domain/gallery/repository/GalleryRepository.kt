package org.grr.bridgy.domain.gallery.repository

import org.grr.bridgy.domain.gallery.entity.Gallery
import org.springframework.data.jpa.repository.JpaRepository

interface GalleryRepository : JpaRepository<Gallery, Long> {
    fun findByPetIdOrderByCreatedAtDesc(petId: Long): List<Gallery>
    fun countByPetId(petId: Long): Long
}
