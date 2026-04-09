package org.grr.bridgy.common.config

import org.grr.bridgy.common.jwt.JwtFilter
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter

@Configuration
@EnableWebSecurity
class SecurityConfig(
    private val jwtFilter: JwtFilter
) {

    @Bean
    fun passwordEncoder(): PasswordEncoder = BCryptPasswordEncoder()

    @Bean
    fun filterChain(http: HttpSecurity): SecurityFilterChain {
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .authorizeHttpRequests { auth ->
                auth
                    // v0 API는 인증 없이 접근 허용
                    .requestMatchers("/api/v0/**").permitAll()
                    // Swagger UI
                    .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                    // H2 Console (로컬 개발용)
                    .requestMatchers("/h2-console/**").permitAll()
                    // Admin API는 ADMIN 또는 SUPER_ADMIN 역할 필요
                    .requestMatchers("/api/admin/**").hasAnyRole("ADMIN", "SUPER_ADMIN")
                    // 나머지 전부 인증 필요
                    .anyRequest().authenticated()
            }
            .addFilterBefore(jwtFilter, UsernamePasswordAuthenticationFilter::class.java)
            .headers { it.frameOptions { frame -> frame.sameOrigin() } }

        return http.build()
    }
}