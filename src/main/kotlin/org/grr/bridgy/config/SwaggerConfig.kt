package org.grr.bridgy.config

import io.swagger.v3.oas.models.OpenAPI
import io.swagger.v3.oas.models.info.Contact
import io.swagger.v3.oas.models.info.Info
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
                    .description("반려동물 소개 소셜 플랫폼 API")
                    .version("v1.0.0")
                    .contact(
                        Contact()
                            .name("Bridgy Team")
                    )
            )
            .servers(
                listOf(
                    Server().url("http://localhost:8080").description("로컬 개발 서버")
                )
            )
    }
}
