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
import org.springframework.web.cors.CorsConfiguration
import org.springframework.web.cors.CorsConfigurationSource
import org.springframework.web.cors.UrlBasedCorsConfigurationSource

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
            .cors { it.configurationSource(corsConfigurationSource()) }
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

    @Bean
    fun corsConfigurationSource(): CorsConfigurationSource {
        val configuration = CorsConfiguration()
        configuration.allowedOriginPatterns = listOf(
            "http://localhost:*",      // 로컬 개발 (모든 포트)
            "http://192.168.*.*:*",    // 같은 WiFi 내 접속 (사설 IP)
            "http://10.*.*.*:*",       // 사설 네트워크
            "http://172.16.*.*:*",     // 사설 네트워크
            "https://api.bridgy.app"   // 프로덕션
        )
        configuration.allowedMethods = listOf("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        configuration.allowedHeaders = listOf("*")
        configuration.allowCredentials = true
        configuration.maxAge = 3600L

        val source = UrlBasedCorsConfigurationSource()
        source.registerCorsConfiguration("/**", configuration)
        return source
    }
}