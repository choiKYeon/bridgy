package org.grr.bridgy.config

import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.databind.SerializationFeature
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import org.springframework.cache.CacheManager
import org.springframework.cache.annotation.EnableCaching
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.cache.RedisCacheConfiguration
import org.springframework.data.redis.cache.RedisCacheManager
import org.springframework.data.redis.connection.RedisConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer
import org.springframework.data.redis.serializer.RedisSerializationContext
import org.springframework.data.redis.serializer.StringRedisSerializer
import org.springframework.context.annotation.Profile
import java.time.Duration

@Configuration
@EnableCaching
@Profile("!test")
class RedisConfig {

    @Bean
    fun redisObjectMapper(): ObjectMapper {
        return jacksonObjectMapper().apply {
            registerModule(JavaTimeModule())
            disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS)
        }
    }

    @Bean
    fun redisTemplate(connectionFactory: RedisConnectionFactory): RedisTemplate<String, Any> {
        return RedisTemplate<String, Any>().apply {
            setConnectionFactory(connectionFactory)
            keySerializer = StringRedisSerializer()
            valueSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper())
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper())
        }
    }

    @Bean
    fun cacheManager(connectionFactory: RedisConnectionFactory): CacheManager {
        val jsonSerializer = GenericJackson2JsonRedisSerializer(redisObjectMapper())

        val defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
            .entryTtl(Duration.ofMinutes(10))
            .serializeKeysWith(RedisSerializationContext.SerializationPair.fromSerializer(StringRedisSerializer()))
            .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(jsonSerializer))
            .disableCachingNullValues()

        // 캐시별 TTL 설정
        val cacheConfigs = mapOf(
            "pets" to defaultConfig.entryTtl(Duration.ofMinutes(30)),           // 반려동물 프로필: 30분
            "petsByUser" to defaultConfig.entryTtl(Duration.ofMinutes(15)),     // 사용자별 반려동물 목록: 15분
            "gallery" to defaultConfig.entryTtl(Duration.ofMinutes(10)),        // 갤러리: 10분
            "healthRecords" to defaultConfig.entryTtl(Duration.ofMinutes(10)),  // 건강 기록: 10분
            "dailyRecords" to defaultConfig.entryTtl(Duration.ofMinutes(5))    // 일상 기록: 5분
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }
}
