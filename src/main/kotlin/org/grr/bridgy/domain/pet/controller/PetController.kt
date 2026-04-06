package org.grr.bridgy.domain.pet.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetDashboardResponse
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.service.PetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "Pet", description = "반려동물 API")
@RestController
@RequestMapping("/api/pets")
class PetController(
    private val petService: PetService
) {

    @Operation(summary = "반려동물 등록", description = "새 반려동물 프로필 등록")
    @PostMapping
    fun createPet(@RequestBody request: CreatePetRequest): ResponseEntity<PetResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request))
    }

    @Operation(summary = "반려동물 단건 조회", description = "petId로 반려동물 정보 조회")
    @GetMapping("/{petId}")
    fun getPet(@PathVariable petId: Long): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.getPetById(petId))
    }

    @Operation(summary = "전체 반려동물 목록", description = "등록된 모든 반려동물 조회")
    @GetMapping
    fun getAllPets(): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getAllPets())
    }

    @Operation(summary = "사용자별 반려동물 목록", description = "특정 사용자가 등록한 반려동물 목록")
    @GetMapping("/user/{userId}")
    fun getPetsByUser(@PathVariable userId: Long): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsByUserId(userId))
    }

    @Operation(summary = "이름으로 검색", description = "반려동물 이름 키워드 검색")
    @GetMapping("/search")
    fun searchPets(@RequestParam name: String): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.searchPetsByName(name))
    }

    @Operation(summary = "종류별 조회", description = "강아지, 고양이 등 종류별 필터링")
    @GetMapping("/species/{species}")
    fun getPetsBySpecies(@PathVariable species: String): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsBySpecies(species))
    }

    @Operation(summary = "반려동물 수정", description = "반려동물 프로필 정보 수정")
    @PutMapping("/{petId}")
    fun updatePet(
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest
    ): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.updatePet(petId, request))
    }

    @Operation(summary = "반려동물 삭제", description = "반려동물 프로필 삭제")
    @DeleteMapping("/{petId}")
    fun deletePet(@PathVariable petId: Long): ResponseEntity<Void> {
        petService.deletePet(petId)
        return ResponseEntity.noContent().build()
    }

    // ─── 대시보드 / 피드 ───

    @Operation(summary = "인기순 피드", description = "좋아요 많은 순으로 반려동물 목록 (userId 전달 시 좋아요 여부 포함)")
    @GetMapping("/feed/popular")
    fun getPopularPets(
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<List<PetDashboardResponse>> {
        return ResponseEntity.ok(petService.getPopularPets(userId))
    }

    @Operation(summary = "최신순 피드", description = "최근 등록 순으로 반려동물 목록")
    @GetMapping("/feed/recent")
    fun getRecentPets(
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<List<PetDashboardResponse>> {
        return ResponseEntity.ok(petService.getRecentPets(userId))
    }

    @Operation(summary = "반려동물 상세", description = "좋아요 수, 댓글 수, 갤러리 수 포함 상세 조회")
    @GetMapping("/{petId}/detail")
    fun getPetDetail(
        @PathVariable petId: Long,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<PetDashboardResponse> {
        return ResponseEntity.ok(petService.getPetDetail(petId, userId))
    }
}
