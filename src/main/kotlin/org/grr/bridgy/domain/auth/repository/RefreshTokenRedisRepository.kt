package org.grr.bridgy.domain.auth.repository

import org.springframework.context.annotation.Profile
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.stereotype.Repository
import java.time.Duration

@Repository
@Profile("!test")
class RefreshTokenRedisRepository(private val redisTemplate: RedisTemplate<String, Any>) {

    companion object {
        private const val USER_KEY = "rt:user:"
        private const val TOKEN_KEY = "rt:token:"
    }

    fun save(userId: Long, token: String, ttlMillis: Long) {
        val ttl = Duration.ofMillis(ttlMillis)
        val oldToken = redisTemplate.opsForValue().get("$USER_KEY$userId") as? String
        oldToken?.let { redisTemplate.delete("$TOKEN_KEY$it") }
        redisTemplate.opsForValue().set("$USER_KEY$userId", token, ttl)
        redisTemplate.opsForValue().set("$TOKEN_KEY$token", userId.toString(), ttl)
    }

    fun findUserIdByToken(token: String): Long? {
        return (redisTemplate.opsForValue().get("$TOKEN_KEY$token") as? String)?.toLong()
    }

    fun existsByToken(token: String): Boolean {
        return redisTemplate.hasKey("$TOKEN_KEY$token") == true
    }

    fun deleteByUserId(userId: Long) {
        val token = redisTemplate.opsForValue().get("$USER_KEY$userId") as? String
        token?.let { redisTemplate.delete("$TOKEN_KEY$it") }
        redisTemplate.delete("$USER_KEY$userId")
    }
}