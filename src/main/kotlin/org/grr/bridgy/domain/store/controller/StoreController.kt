package org.grr.bridgy.domain.store.controller

import jakarta.validation.Valid
import org.grr.bridgy.domain.store.dto.StoreCreateRequest
import org.grr.bridgy.domain.store.dto.StoreResponse
import org.grr.bridgy.domain.store.dto.StoreUpdateRequest
import org.grr.bridgy.domain.store.service.StoreService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@RestController
@RequestMapping("/api/stores")
class StoreController(
    private val storeService: StoreService
) {
    @GetMapping("/{id}")
    fun getStore(@PathVariable id: Long): ResponseEntity<StoreResponse> {
        return ResponseEntity.ok(storeService.getStore(id))
    }

    @GetMapping
    fun getStoresByOwner(@RequestParam ownerEmail: String): ResponseEntity<List<StoreResponse>> {
        return ResponseEntity.ok(storeService.getStoresByOwner(ownerEmail))
    }

    @PostMapping
    fun createStore(@Valid @RequestBody request: StoreCreateRequest): ResponseEntity<StoreResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(storeService.createStore(request))
    }

    @PutMapping("/{id}")
    fun updateStore(
        @PathVariable id: Long,
        @RequestBody request: StoreUpdateRequest
    ): ResponseEntity<StoreResponse> {
        return ResponseEntity.ok(storeService.updateStore(id, request))
    }

    @DeleteMapping("/{id}")
    fun deleteStore(@PathVariable id: Long): ResponseEntity<Unit> {
        storeService.deleteStore(id)
        return ResponseEntity.noContent().build()
    }
}
