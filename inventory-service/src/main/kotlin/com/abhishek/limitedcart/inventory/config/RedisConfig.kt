package com.abhishek.limitedcart.inventory.config

import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.StringRedisSerializer

@Configuration
class RedisConfig {

    @Bean
    fun redisConnectionFactory(
        @Value("\${spring.redis.host}") host: String,
        @Value("\${spring.redis.port}") port: Int,
        @Value("\${spring.redis.password:}") password: String?
    ): LettuceConnectionFactory {
        val config = RedisStandaloneConfiguration(host, port)
        if (!password.isNullOrBlank()) {
            config.setPassword(password)
        }
        return LettuceConnectionFactory(config)
    }

    @Bean
    fun redisTemplate(connectionFactory: LettuceConnectionFactory): RedisTemplate<String, String> {
        return RedisTemplate<String, String>().apply {
            this.connectionFactory = connectionFactory
            keySerializer = StringRedisSerializer ()
            valueSerializer = StringRedisSerializer()
            hashKeySerializer = StringRedisSerializer()
            hashValueSerializer = StringRedisSerializer()
            afterPropertiesSet()
        }
    }
}
