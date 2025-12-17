package com.abhishek.limitedcart.notification.config

import com.abhishek.limitedcart.common.kafka.KafkaFactories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.annotation.EnableKafka
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory

@Configuration
@EnableKafka
class KafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        return KafkaFactories.createConsumerFactory(
            bootstrapServers = bootstrapServers,
            groupId = "notification-service",
            autoOffsetReset = "earliest",
            enableAutoCommit = false
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        return KafkaFactories.createListenerContainerFactory(
            consumerFactory = consumerFactory(),
            ackMode = org.springframework.kafka.listener.ContainerProperties.AckMode.MANUAL
        )
    }
}
