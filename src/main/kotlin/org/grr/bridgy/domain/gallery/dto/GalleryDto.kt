package org.grr.bridgy.domain.gallery.dto

import org.grr.bridgy.domain.gallery.entity.Gallery
import java.time.LocalDateTime

data class CreateGalleryRequest(
    val petId: Long,
    val imageUrl: String,
    val caption: String? = null
)

data class GalleryResponse(
    val id: Long,
    val petId: Long,
    val petName: String,
    val imageUrl: String,
    val caption: String?,
    val createdAt: LocalDateTime
) {
    companion object {
        fun from(gallery: Gallery) = GalleryResponse(
            id = gallery.id,
            petId = gallery.pet.id,
            petName = gallery.pet.name,
            imageUrl = gallery.imageUrl,
            caption = gallery.caption,
            createdAt = gallery.createdAt
        )
    }
}
