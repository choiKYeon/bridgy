package org.grr.bridgy.domain.store.repository

import org.grr.bridgy.domain.store.entity.Store
import org.springframework.data.jpa.repository.JpaRepository

interface StoreRepository : JpaRepository<Store, Long> {
    fun findByOwnerEmail(ownerEmail: String): List<Store>
    fun findByKakaoChannelId(kakaoChannelId: String): Store?
    fun findByCategoryContaining(category: String): List<Store>
}
