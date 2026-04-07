package org.grr.bridgy.domain.user.controller

import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.grr.bridgy.domain.user.dto.SignUpRequest
import org.grr.bridgy.domain.user.dto.UserResponse
import org.grr.bridgy.domain.user.service.UserService
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.*

@Tag(name = "User V0", description = "회원 공개 API")
@RestController
@RequestMapping("/api/v0/users")
class UserControllerV0(
    private val userService: UserService
) {

    @Operation(summary = "회원가입", description = "이메일, 비밀번호, 닉네임으로 회원가입")
    @PostMapping("/signup")
    fun signUp(@RequestBody request: SignUpRequest): ResponseEntity<UserResponse> {
        return ResponseEntity.status(HttpStatus.CREATED).body(userService.signUp(request))
    }
}
