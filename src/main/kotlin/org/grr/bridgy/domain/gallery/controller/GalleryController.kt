package org.grr.bridgy.domain.gallery.controller

import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.service.GalleryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/gallery")
class GalleryController(
    private val galleryService: GalleryService
) {

    @PostMapping
    fun addPhoto(@RequestBody request: CreateGalleryRequest): ResponseEntity<GalleryResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryService.addPhoto(request))
    }

    @GetMapping("/pet/{petId}")
    fun getPhotosByPet(@PathVariable petId: Long): ResponseEntity<List<GalleryResponse>> {
        return ResponseEntity.ok(galleryService.getPhotosByPetId(petId))
    }

    @GetMapping("/{photoId}")
    fun getPhoto(@PathVariable photoId: Long): ResponseEntity<GalleryResponse> {
        return ResponseEntity.ok(galleryService.getPhotoById(photoId))
    }

    @DeleteMapping("/{photoId}")
    fun deletePhoto(@PathVariable photoId: Long): ResponseEntity<Void> {
        galleryService.deletePhoto(photoId)
        return ResponseEntity.noContent().build()
    }
}
