package org.grr.bridgy.domain.store.service

import org.grr.bridgy.domain.store.dto.StoreCreateRequest
import org.grr.bridgy.domain.store.dto.StoreResponse
import org.grr.bridgy.domain.store.dto.StoreUpdateRequest
import org.grr.bridgy.domain.store.entity.Store
import org.grr.bridgy.domain.store.repository.StoreRepository
import org.springframework.cache.annotation.CacheEvict
import org.springframework.cache.annotation.Cacheable
import org.springframework.cache.annotation.Caching
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class StoreService(
    private val storeRepository: StoreRepository
) {
    @Cacheable(value = ["store"], key = "#id")
    fun getStore(id: Long): StoreResponse {
        val store = storeRepository.findById(id)
            .orElseThrow { NoSuchElementException("매장을 찾을 수 없습니다: $id") }
        return StoreResponse.from(store)
    }

    @Cacheable(value = ["storesByOwner"], key = "#ownerEmail")
    fun getStoresByOwner(ownerEmail: String): List<StoreResponse> {
        return storeRepository.findByOwnerEmail(ownerEmail).map { StoreResponse.from(it) }
    }

    @Transactional
    @CacheEvict(value = ["storesByOwner"], allEntries = true)
    fun createStore(request: StoreCreateRequest): StoreResponse {
        val store = Store(
            name = request.name,
            category = request.category,
            address = request.address,
            phone = request.phone,
            description = request.description,
            openTime = request.openTime,
            closeTime = request.closeTime,
            closedDays = request.closedDays.toMutableList(),
            kakaoChannelId = request.kakaoChannelId,
            ownerEmail = request.ownerEmail
        )
        return StoreResponse.from(storeRepository.save(store))
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["store"], key = "#id"),
        CacheEvict(value = ["storesByOwner"], allEntries = true)
    ])
    fun updateStore(id: Long, request: StoreUpdateRequest): StoreResponse {
        val store = storeRepository.findById(id)
            .orElseThrow { NoSuchElementException("매장을 찾을 수 없습니다: $id") }

        request.name?.let { store.name = it }
        request.category?.let { store.category = it }
        request.address?.let { store.address = it }
        request.phone?.let { store.phone = it }
        request.description?.let { store.description = it }
        request.openTime?.let { store.openTime = it }
        request.closeTime?.let { store.closeTime = it }
        request.closedDays?.let { store.closedDays = it.toMutableList() }
        request.kakaoChannelId?.let { store.kakaoChannelId = it }
        store.updatedAt = LocalDateTime.now()

        return StoreResponse.from(storeRepository.save(store))
    }

    @Transactional
    @Caching(evict = [
        CacheEvict(value = ["store"], key = "#id"),
        CacheEvict(value = ["storesByOwner"], allEntries = true)
    ])
    fun deleteStore(id: Long) {
        storeRepository.deleteById(id)
    }
}
