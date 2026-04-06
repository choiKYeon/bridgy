package org.grr.bridgy.domain.pet.controller

import org.grr.bridgy.domain.pet.dto.CreatePetRequest
import org.grr.bridgy.domain.pet.dto.PetDashboardResponse
import org.grr.bridgy.domain.pet.dto.PetResponse
import org.grr.bridgy.domain.pet.dto.UpdatePetRequest
import org.grr.bridgy.domain.pet.service.PetService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/pets")
class PetController(
    private val petService: PetService
) {

    @PostMapping
    fun createPet(@RequestBody request: CreatePetRequest): ResponseEntity<PetResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(petService.createPet(request))
    }

    @GetMapping("/{petId}")
    fun getPet(@PathVariable petId: Long): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.getPetById(petId))
    }

    @GetMapping
    fun getAllPets(): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getAllPets())
    }

    @GetMapping("/user/{userId}")
    fun getPetsByUser(@PathVariable userId: Long): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsByUserId(userId))
    }

    @GetMapping("/search")
    fun searchPets(@RequestParam name: String): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.searchPetsByName(name))
    }

    @GetMapping("/species/{species}")
    fun getPetsBySpecies(@PathVariable species: String): ResponseEntity<List<PetResponse>> {
        return ResponseEntity.ok(petService.getPetsBySpecies(species))
    }

    @PutMapping("/{petId}")
    fun updatePet(
        @PathVariable petId: Long,
        @RequestBody request: UpdatePetRequest
    ): ResponseEntity<PetResponse> {
        return ResponseEntity.ok(petService.updatePet(petId, request))
    }

    @DeleteMapping("/{petId}")
    fun deletePet(@PathVariable petId: Long): ResponseEntity<Void> {
        petService.deletePet(petId)
        return ResponseEntity.noContent().build()
    }

    // ─── 대시보드 / 피드 ───

    @GetMapping("/feed/popular")
    fun getPopularPets(
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<List<PetDashboardResponse>> {
        return ResponseEntity.ok(petService.getPopularPets(userId))
    }

    @GetMapping("/feed/recent")
    fun getRecentPets(
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<List<PetDashboardResponse>> {
        return ResponseEntity.ok(petService.getRecentPets(userId))
    }

    @GetMapping("/{petId}/detail")
    fun getPetDetail(
        @PathVariable petId: Long,
        @RequestParam(required = false) userId: Long?
    ): ResponseEntity<PetDashboardResponse> {
        return ResponseEntity.ok(petService.getPetDetail(petId, userId))
    }
}
