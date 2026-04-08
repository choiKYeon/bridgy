package org.grr.bridgy.domain.comment.service

import org.grr.bridgy.common.exception.CustomException
import org.grr.bridgy.domain.comment.dto.CommentResponse
import org.grr.bridgy.domain.comment.dto.CreateCommentRequest
import org.grr.bridgy.domain.comment.dto.UpdateCommentRequest
import org.grr.bridgy.domain.comment.entity.Comment
import org.grr.bridgy.domain.comment.repository.CommentRepository
import org.grr.bridgy.domain.pet.repository.PetRepository
import org.grr.bridgy.domain.user.repository.UserRepository
import org.springframework.http.HttpStatus
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional(readOnly = true)
class CommentService(
    private val commentRepository: CommentRepository,
    private val petRepository: PetRepository,
    private val userRepository: UserRepository
) {

    @Transactional
    fun createComment(request: CreateCommentRequest): CommentResponse {
        val pet = petRepository.findById(request.petId)
            .orElseThrow { CustomException("반려동물을 찾을 수 없습니다. id=${request.petId}", HttpStatus.NOT_FOUND) }
        val user = userRepository.findById(request.userId)
            .orElseThrow { CustomException("사용자를 찾을 수 없습니다. id=${request.userId}", HttpStatus.NOT_FOUND) }

        val comment = Comment(
            pet = pet,
            user = user,
            content = request.content
        )
        return CommentResponse.from(commentRepository.save(comment))
    }

    fun getCommentsByPetId(petId: Long): List<CommentResponse> {
        return commentRepository.findByPetIdOrderByCreatedAtDesc(petId)
            .map { CommentResponse.from(it) }
    }

    fun getCommentCount(petId: Long): Long {
        return commentRepository.countByPetId(petId)
    }

    @Transactional
    fun updateComment(commentId: Long, request: UpdateCommentRequest): CommentResponse {
        val comment = commentRepository.findById(commentId)
            .orElseThrow { CustomException("댓글을 찾을 수 없습니다. id=$commentId", HttpStatus.NOT_FOUND) }

        comment.content = request.content
        comment.updatedAt = LocalDateTime.now()

        return CommentResponse.from(commentRepository.save(comment))
    }

    @Transactional
    fun deleteComment(commentId: Long) {
        if (!commentRepository.existsById(commentId)) {
            throw CustomException("댓글을 찾을 수 없습니다. id=$commentId", HttpStatus.NOT_FOUND)
        }
        commentRepository.deleteById(commentId)
    }
}
