package org.grr.bridgy.domain.comment.repository

import org.grr.bridgy.domain.comment.entity.Comment
import org.springframework.data.jpa.repository.JpaRepository

interface CommentRepository : JpaRepository<Comment, Long> {
    fun findByPetIdOrderByCreatedAtDesc(petId: Long): List<Comment>
    fun findByUserIdOrderByCreatedAtDesc(userId: Long): List<Comment>
    fun countByPetId(petId: Long): Long
}
