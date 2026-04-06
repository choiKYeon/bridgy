package org.grr.bridgy.domain.gallery.service

import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.entity.Gallery
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

@Service
@Transactional(readOnly = true)
class GalleryService(
    private val galleryRepository: GalleryRepository,
    private val petRepository: PetRepository
) {

    @Transactional
    fun addPhoto(request: CreateGalleryRequest): GalleryResponse {
        val pet = petRepository.findById(request.petId)
            .orElseThrow { IllegalArgumentException("반려동물을 찾을 수 없습니다. id=${request.petId}") }

        val gallery = Gallery(
            pet = pet,
            imageUrl = request.imageUrl,
            caption = request.caption
        )
        return GalleryResponse.from(galleryRepository.save(gallery))
    }

    fun getPhotosByPetId(petId: Long): List<GalleryResponse> {
        return galleryRepository.findByPetIdOrderByCreatedAtDesc(petId)
            .map { GalleryResponse.from(it) }
    }

    fun getPhotoById(photoId: Long): GalleryResponse {
        val gallery = galleryRepository.findById(photoId)
            .orElseThrow { IllegalArgumentException("사진을 찾을 수 없습니다. id=$photoId") }
        return GalleryResponse.from(gallery)
    }

    @Transactional
    fun deletePhoto(photoId: Long) {
        require(galleryRepository.existsById(photoId)) { "사진을 찾을 수 없습니다. id=$photoId" }
        galleryRepository.deleteById(photoId)
    }
}
