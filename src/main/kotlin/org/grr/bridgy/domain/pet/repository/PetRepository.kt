package org.grr.bridgy.domain.pet.repository

import org.grr.bridgy.domain.pet.entity.Pet
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PetRepository : JpaRepository<Pet, Long> {
    fun findByUserId(userId: Long): List<Pet>
    fun findBySpecies(species: String): List<Pet>
    fun findByNameContaining(name: String): List<Pet>

    // 좋아요 많은 순으로 인기 반려동물 조회
    @Query("""
        SELECT p FROM Pet p
        LEFT JOIN Like l ON l.pet.id = p.id
        GROUP BY p.id
        ORDER BY COUNT(l.id) DESC
    """)
    fun findAllOrderByLikeCountDesc(): List<Pet>

    // 최신 등록순 조회
    fun findAllByOrderByCreatedAtDesc(): List<Pet>
}
