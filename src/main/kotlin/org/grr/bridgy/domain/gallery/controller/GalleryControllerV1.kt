package org.grr.bridgy.domain.gallery.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.Parameter
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.Schema
import io.swagger.v3.oas.annotations.responses.ApiResponse
import io.swagger.v3.oas.annotations.responses.ApiResponses
import io.swagger.v3.oas.annotations.security.SecurityRequirement
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.gallery.dto.CreateGalleryRequest
import org.grr.bridgy.domain.gallery.dto.GalleryResponse
import org.grr.bridgy.domain.gallery.service.GalleryService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Gallery V1", description = "갤러리 인증 API (JWT Bearer 토큰 필요)")
@RestController
@RequestMapping("/api/v1/gallery")
class GalleryControllerV1(
    private val galleryService: GalleryService
) {

    @Operation(
        summary = "갤러리 사진 업로드",
        description = "반려동물 갤러리에 사진을 추가합니다. 무료 사용자는 반려동물당 최대 20장까지만 업로드 가능합니다.",
        tags = ["Gallery"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "201",
                description = "사진 업로드 성공",
                content = [Content(
                    mediaType = "application/json",
                    schema = Schema(implementation = GalleryResponse::class)
                )]
            ),
            ApiResponse(
                responseCode = "400",
                description = "유효하지 않은 입력 데이터 또는 잘못된 이미지 형식"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "갤러리 사진 초과 (최대 20장) 또는 자신의 반려동물에만 업로드 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 반려동물을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @PostMapping
    fun addPhoto(@RequestBody request: CreateGalleryRequest): ResponseEntity<GalleryResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(galleryService.addPhoto(request))
    }

    @Operation(
        summary = "갤러리 사진 삭제",
        description = "갤러리에서 사진을 삭제합니다. 자신이 등록한 반려동물의 사진만 삭제 가능합니다.",
        tags = ["Gallery"],
        security = [SecurityRequirement(name = "bearerAuth")]
    )
    @ApiResponses(
        value = [
            ApiResponse(
                responseCode = "204",
                description = "사진 삭제 성공"
            ),
            ApiResponse(
                responseCode = "401",
                description = "유효하지 않거나 만료된 JWT 토큰"
            ),
            ApiResponse(
                responseCode = "403",
                description = "자신의 반려동물 사진만 삭제 가능"
            ),
            ApiResponse(
                responseCode = "404",
                description = "해당 사진을 찾을 수 없음"
            ),
            ApiResponse(
                responseCode = "429",
                description = "요청이 너무 많음 (Rate limiting)"
            )
        ]
    )
    @DeleteMapping("/{photoId}")
    fun deletePhoto(
        @Parameter(
            description = "삭제할 사진의 고유 ID",
            example = "1",
            required = true
        )
        @PathVariable photoId: Long
    ): ResponseEntity<Void> {
        galleryService.deletePhoto(photoId)
        return ResponseEntity.noContent().build()
    }
}
