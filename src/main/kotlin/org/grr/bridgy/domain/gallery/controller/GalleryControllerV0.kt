package org.grr.bridgy.domain.gallery.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.service.GalleryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Gallery V0", description = "갤러리 공개 API")
@RestController
@RequestMapping("/api/v0/gallery")
class GalleryControllerV0(
    private val galleryService: GalleryService
) {

    @Operation(summary = "펫별 사진 목록", description = "특정 반려동물의 갤러리 사진 목록 (최신순)")
    @GetMapping("/pet/{petId}")
    fun getPhotosByPet(@PathVariable petId: Long): ResponseEntity<List<GalleryResponse>> {
        return ResponseEntity.ok(galleryService.getPhotosByPetId(petId))
    }

    @Operation(summary = "사진 단건 조회", description = "photoId로 사진 상세 조회")
    @GetMapping("/{photoId}")
    fun getPhoto(@PathVariable photoId: Long): ResponseEntity<GalleryResponse> {
        return ResponseEntity.ok(galleryService.getPhotoById(photoId))
    }
}
