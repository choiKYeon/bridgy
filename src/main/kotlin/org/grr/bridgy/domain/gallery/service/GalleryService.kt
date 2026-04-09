package org.grr.bridgy.domain.gallery.service

import org.grr.bridgy.common.config.FreeTierLimits
import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.entity.Gallery
import org.grr.bridgy.domain.gallery.repository.GalleryRepository
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.springframework.http.HttpStatus
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
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=${request.petId}", HttpStatus.NOT_FOUND) }

        val currentPhotoCount = galleryRepository.countByPetId(request.petId)
        if (currentPhotoCount >= FreeTierLimits.MAX_PHOTOS_PER_PET) {
            throw CustomException(
                "사진은 반려동물당 최대 ${FreeTierLimits.MAX_PHOTOS_PER_PET}장까지 업로드할 수 있습니다. (현재: ${currentPhotoCount}장)",
                HttpStatus.BAD_REQUEST
            )
        }

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
            .orElseThrow { CustomException("사진을 찾을 수 없습니다. id=$photoId", HttpStatus.NOT_FOUND) }
        return GalleryResponse.from(gallery)
    }

    @Transactional
    fun deletePhoto(photoId: Long) {
        if (!galleryRepository.existsById(photoId)) {
            throw CustomException("사진을 찾을 수 없습니다. id=$photoId", HttpStatus.NOT_FOUND)
        }
        galleryRepository.deleteById(photoId)
    }
}
