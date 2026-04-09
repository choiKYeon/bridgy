package org.grr.bridgy.common.jwt

import io.jsonwebtoken.Claims
import io.jsonwebtoken.ExpiredJwtException
import io.jsonwebtoken.Jwts
import io.jsonwebtoken.security.Keys
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.Authentication
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.userdetails.User
import org.springframework.stereotype.Component
import java.util.*
import javax.crypto.SecretKey

@Component
class JwtProvider(
    @Value("\${jwt.secret}") private val secret: String,
    @Value("\${jwt.access-token-expiration}") private val accessTokenExpiration: Long,
    @Value("\${jwt.refresh-token-expiration}") private val refreshTokenExpiration: Long
) {

    private val key: SecretKey by lazy {
        Keys.hmacShaKeyFor(Base64.getDecoder().decode(secret))
    }

    fun createAccessToken(userId: Long, email: String, role: String = "USER"): String {
        return createToken(userId, email, role, accessTokenExpiration, "access")
    }

    fun createRefreshToken(userId: Long, email: String, role: String = "USER"): String {
        return createToken(userId, email, role, refreshTokenExpiration, "refresh")
    }

    private fun createToken(userId: Long, email: String, role: String, expiration: Long, tokenType: String): String {
        val now = Date()
        val expireDate = Date(now.time + expiration)

        return Jwts.builder()
            .subject(userId.toString())
            .claim("email", email)
            .claim("role", role)
            .claim("type", tokenType)
            .issuedAt(now)
            .expiration(expireDate)
            .signWith(key)
            .compact()
    }

    fun getAuthentication(token: String): Authentication {
        val claims = parseClaims(token)
        val userId = claims.subject
        val role = claims["role"] as? String ?: "USER"
        val authorities = listOf(SimpleGrantedAuthority("ROLE_$role"))
        val principal = User(userId, "", authorities)
        return UsernamePasswordAuthenticationToken(principal, token, authorities)
    }

    fun getUserId(token: String): Long {
        return parseClaims(token).subject.toLong()
    }

    fun getEmail(token: String): String {
        return parseClaims(token)["email"] as String
    }

    fun validateToken(token: String): Boolean {
        return try {
            parseClaims(token)
            true
        } catch (e: Exception) {
            false
        }
    }

    fun isExpired(token: String): Boolean {
        return try {
            parseClaims(token)
            false
        } catch (e: ExpiredJwtException) {
            true
        } catch (e: Exception) {
            true
        }
    }

    fun getTokenType(token: String): String {
        return parseClaims(token)["type"] as String
    }

    fun getRefreshTokenExpiration(): Long = refreshTokenExpiration

    private fun parseClaims(token: String): Claims {
        return Jwts.parser()
            .verifyWith(key)
            .build()
            .parseSignedClaims(token)
            .payload
    }
}