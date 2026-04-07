package org.grr.bridgy.domain.gallery.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.service.GalleryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Gallery V1", description = "갤러리 인증 API")
@RestController
@RequestMapping("/api/v1/gallery")
class GalleryControllerV1(
    private val galleryService: GalleryService
) {

    @Operation(summary = "사진 업로드", description = "반려동물 갤러리에 사진 추가")
    @PostMapping
    fun addPhoto(@RequestBody request: CreateGalleryRequest): ResponseEntity<GalleryResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryService.addPhoto(request))
    }

    @Operation(summary = "사진 삭제", description = "갤러리 사진 삭제")
    @DeleteMapping("/{photoId}")
    fun deletePhoto(@PathVariable photoId: Long): ResponseEntity<Void> {
        galleryService.deletePhoto(photoId)
        return ResponseEntity.noContent().build()
    }
}
