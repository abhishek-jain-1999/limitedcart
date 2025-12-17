package com.abhishek.limitedcart.inventory.config

import com.abhishek.limitedcart.common.kafka.KafkaFactories
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.ConsumerFactory
import org.springframework.kafka.core.KafkaTemplate
import org.springframework.kafka.core.ProducerFactory

@Configuration
class InventoryKafkaConfig {

    @Value("\${spring.kafka.bootstrap-servers}")
    private lateinit var bootstrapServers: String

    @Bean
    fun producerFactory(): ProducerFactory<String, Any> {
        return KafkaFactories.createProducerFactory(bootstrapServers)
    }

    @Bean
    fun kafkaTemplate(): KafkaTemplate<String, Any> {
        return KafkaFactories.createKafkaTemplate(producerFactory())
    }

    @Bean
    fun consumerFactory(): ConsumerFactory<String, Any> {
        return KafkaFactories.createConsumerFactory(
            bootstrapServers = bootstrapServers,
            groupId = "inventory-service"
        )
    }

    @Bean
    fun kafkaListenerContainerFactory(): ConcurrentKafkaListenerContainerFactory<String, Any> {
        return KafkaFactories.createListenerContainerFactory(consumerFactory())
    }
}
