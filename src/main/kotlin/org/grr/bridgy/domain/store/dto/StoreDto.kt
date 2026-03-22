package org.grr.bridgy.domain.store.dto

import jakarta.validation.constraints.NotBlank
import org.grr.bridgy.domain.store.entity.Store
import java.time.LocalTime

data class StoreCreateRequest(
    @field:NotBlank val name: String,
    @field:NotBlank val category: String,
    @field:NotBlank val address: String,
    @field:NotBlank val phone: String,
    val description: String? = null,
    val openTime: LocalTime? = null,
    val closeTime: LocalTime? = null,
    val closedDays: List<String> = emptyList(),
    val kakaoChannelId: String? = null,
    @field:NotBlank val ownerEmail: String
)

data class StoreUpdateRequest(
    val name: String? = null,
    val category: String? = null,
    val address: String? = null,
    val phone: String? = null,
    val description: String? = null,
    val openTime: LocalTime? = null,
    val closeTime: LocalTime? = null,
    val closedDays: List<String>? = null,
    val kakaoChannelId: String? = null
)

data class StoreResponse(
    val id: Long,
    val name: String,
    val category: String,
    val address: String,
    val phone: String,
    val description: String?,
    val openTime: LocalTime?,
    val closeTime: LocalTime?,
    val closedDays: List<String>,
    val kakaoChannelId: String?
) {
    companion object {
        fun from(store: Store) = StoreResponse(
            id = store.id,
            name = store.name,
            category = store.category,
            address = store.address,
            phone = store.phone,
            description = store.description,
            openTime = store.openTime,
            closeTime = store.closeTime,
            closedDays = store.closedDays,
            kakaoChannelId = store.kakaoChannelId
        )
    }
}
