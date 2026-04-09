package org.grr.bridgy.domain.pet.repository

import org.grr.bridgy.domain.pet.entity.Pet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PetRepository : JpaRepository<Pet, Long> {
    fun findByUserId(userId: Long): List<Pet>
    fun countByUserId(userId: Long): Long
    fun findBySpecies(species: String): List<Pet>

    // 이름 검색 (기존 List + 페이징)
    fun findByNameContaining(name: String): List<Pet>
    fun findByNameContaining(name: String, pageable: Pageable): Page<Pet>

    // 좋아요 많은 순으로 인기 반려동물 조회 (페이징)
    @Query("""
        SELECT p FROM Pet p
        LEFT JOIN Like l ON l.pet.id = p.id
        GROUP BY p.id
        ORDER BY COUNT(l.id) DESC
    """)
    fun findAllOrderByLikeCountDesc(pageable: Pageable): Page<Pet>

    // 최신 등록순 조회 (페이징)
    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Pet>
}
