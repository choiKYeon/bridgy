package org.grr.bridgy.common.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.info.License
import io.swagger.v3.oas.models.security.SecurityScheme
import io.swagger.v3.oas.models.servers.Server
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class SwaggerConfig {

    @Bean
    fun openAPI(): OpenAPI {
        return OpenAPI()
            .info(
                Info()
                    .title("Bridgy API")
                    .description(
                        """
                        반려동물 소개 소셜 플랫폼 API

                        무료 사용자 제한 사항:
                        - 반려동물 등록: 최대 3마리
                        - 반려동물당 사진: 최대 20장
                        - 일일 댓글: 최대 30개
                        - 댓글 길이: 최대 300자
                        - 페이징: 페이지당 최대 20개 항목

                        인증:
                        - V0 엔드포인트: 공개 (인증 불필요)
                        - V1 엔드포인트: JWT Bearer 토큰 필수
                        """.trimIndent()
                    )
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("Bridgy Team")
                    )
                    .license(
                        License()
                            .name("MIT License")
                    )
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("로컬 개발 서버"),
                    Server().url("https://api.bridgy.app").description("프로덕션 서버")
                )
            )
            .components(
                io.swagger.v3.oas.models.Components()
                    .addSecuritySchemes(
                        "bearerAuth",
                        SecurityScheme()
                            .type(SecurityScheme.Type.HTTP)
                            .scheme("bearer")
                            .bearerFormat("JWT")
                            .description("JWT Bearer 토큰을 이용한 인증")
                    )
            )
    }
}