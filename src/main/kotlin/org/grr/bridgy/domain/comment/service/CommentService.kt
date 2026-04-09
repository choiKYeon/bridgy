package org.grr.bridgy.domain.comment.service

import org.grr.bridgy.common.config.FreeTierLimits
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
import java.time.LocalDate
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

        // 댓글 글자수 제한
        if (request.content.length > FreeTierLimits.MAX_COMMENT_LENGTH) {
            throw CustomException(
                "댓글은 최대 ${FreeTierLimits.MAX_COMMENT_LENGTH}자까지 작성할 수 있습니다. (현재: ${request.content.length}자)",
                HttpStatus.BAD_REQUEST
            )
        }

        // 일일 댓글 수 제한
        val todayStart = LocalDate.now().atStartOfDay()
        val todayCommentCount = commentRepository.countByUserIdAndCreatedAtAfter(request.userId, todayStart)
        if (todayCommentCount >= FreeTierLimits.MAX_COMMENTS_PER_USER_PER_DAY) {
            throw CustomException(
                "하루 최대 ${FreeTierLimits.MAX_COMMENTS_PER_USER_PER_DAY}개의 댓글만 작성할 수 있습니다.",
                HttpStatus.TOO_MANY_REQUESTS
            )
        }

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

        // 수정 시에도 글자수 제한
        if (request.content.length > FreeTierLimits.MAX_COMMENT_LENGTH) {
            throw CustomException(
                "댓글은 최대 ${FreeTierLimits.MAX_COMMENT_LENGTH}자까지 작성할 수 있습니다. (현재: ${request.content.length}자)",
                HttpStatus.BAD_REQUEST
            )
        }

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
