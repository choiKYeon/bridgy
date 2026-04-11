package org.grr.bridgy.domain.pet.repository

import org.grr.bridgy.domain.pet.entity.Pet
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query

interface PetRepository : JpaRepository<Pet, Long> {
    fun findByUserId(userId: Long): List<Pet>
    fun countByUserId(userId: Long): Long
    fun findBySpecies(species: String, pageable: Pageable): Page<Pet>
    fun findByNameContaining(name: String, pageable: Pageable): Page<Pet>

    @Query("SELECT p FROM Pet p ORDER BY (SELECT COUNT(l) FROM Like l WHERE l.pet = p) DESC")
    fun findAllOrderByLikeCountDesc(pageable: Pageable): Page<Pet>

    fun findAllByOrderByCreatedAtDesc(pageable: Pageable): Page<Pet>
}