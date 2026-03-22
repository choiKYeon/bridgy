package org.grr.bridgy.config

import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain

@Configuration
@EnableWebSecurity
class SecurityConfig {

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // 카카오 웹훅은 인증 없이 접근 허용
                    .requestMatchers("/api/kakao/**").permitAll()
                    // Swagger UI
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    // H2 Console (로컬 개발용)
                    .requestMatchers("/h2-console/**").permitAll()
                    // 나머지 API는 인증 필요 (추후 JWT 등 추가)
                    .anyRequest().permitAll() // TODO: 인증 구현 후 authenticated()로 변경
            }
            .headers { it.frameOptions { frame -> frame.sameOrigin() } } // H2 Console용

        return http.build()
    }
}
