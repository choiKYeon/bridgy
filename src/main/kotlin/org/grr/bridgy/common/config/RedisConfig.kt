package org.grr.bridgy.common.config

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
            "pets"          to defaultConfig.entryTtl(Duration.ofMinutes(30)),
            "petsByUser"    to defaultConfig.entryTtl(Duration.ofMinutes(15)),
            "gallery"       to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "likeCounts"    to defaultConfig.entryTtl(Duration.ofMinutes(1)),
            "commentCounts" to defaultConfig.entryTtl(Duration.ofMinutes(1)),
            "healthRecords" to defaultConfig.entryTtl(Duration.ofMinutes(10)),
            "dailyRecords"  to defaultConfig.entryTtl(Duration.ofMinutes(5))
        )

        return RedisCacheManager.builder(connectionFactory)
            .cacheDefaults(defaultConfig)
            .withInitialCacheConfigurations(cacheConfigs)
            .build()
    }
}