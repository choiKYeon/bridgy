package org.grr.bridgy.domain.gallery.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.service.GalleryService
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Gallery V0", description = "갤러리 공개 API (인증 불필요)")
@RestController
@RequestMapping("/api/v0/gallery")
class GalleryControllerV0(
    private val galleryService: GalleryService
) {

    @Operation(
        summary = "반려동물별 갤러리 사진 조회",
        description = "특정 반려동물의 갤러리 사진 목록을 최신순으로 조회합니다.",
        tags = ["Gallery"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "갤러리 사진 목록 조회 성공"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/pet/{petId}")
    fun getPhotosByPet(
        @Parameter(
            description = "조회할 반려동물의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable petId: Long
    ): ResponseEntity<List<GalleryResponse>> {
        return ResponseEntity.ok(galleryService.getPhotosByPetId(petId))
    }

    @Operation(
        summary = "갤러리 사진 단건 조회",
        description = "사진 ID로 갤러리 사진 상세 정보를 조회합니다.",
        tags = ["Gallery"]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "200",
                description = "사진 조회 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GalleryResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사진을 찾을 수 없음"
            )
        ]
    )
    @GetMapping("/{photoId}")
    fun getPhoto(
        @Parameter(
            description = "조회할 사진의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable photoId: Long
    ): ResponseEntity<GalleryResponse> {
        return ResponseEntity.ok(galleryService.getPhotoById(photoId))
    }
}
