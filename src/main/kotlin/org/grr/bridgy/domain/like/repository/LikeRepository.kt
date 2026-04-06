package org.grr.bridgy.domain.like.repository

import org.grr.bridgy.domain.like.entity.Like
import org.springframework.data.jpa.repository.JpaRepository

interface LikeRepository : JpaRepository<Like, Long> {
    fun findByPetIdAndUserId(petId: Long, userId: Long): Like?
    fun existsByPetIdAndUserId(petId: Long, userId: Long): Boolean
    fun countByPetId(petId: Long): Long
    fun deleteByPetIdAndUserId(petId: Long, userId: Long)
}
