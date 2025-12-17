package com.abhishek.limitedcart.common.kafka

import org.apache.kafka.clients.consumer.ConsumerConfig
import org.apache.kafka.clients.producer.ProducerConfig
import org.apache.kafka.common.serialization.StringDeserializer
import org.apache.kafka.common.serialization.StringSerializer
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory
import org.springframework.kafka.core.*
import org.springframework.kafka.listener.ContainerProperties
import org.springframework.kafka.support.serializer.JsonDeserializer
import org.springframework.kafka.support.serializer.JsonSerializer

/**
 * Kafka factory utilities (NO Spring annotations).
 * Each service creates its own @Configuration class and uses these factories.
 */
object KafkaFactories {

    fun createProducerFactory(bootstrapServers: String): ProducerFactory<String, Any> {
        val configProps = mapOf(
            ProducerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG to StringSerializer::class.java,
            ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG to JsonSerializer::class.java
        )
        return DefaultKafkaProducerFactory(configProps)
    }

    fun createKafkaTemplate(producerFactory: ProducerFactory<String, Any>): KafkaTemplate<String, Any> {
        return KafkaTemplate(producerFactory)
    }

    fun createConsumerFactory(
        bootstrapServers: String,
        groupId: String,
        autoOffsetReset: String = "earliest",
        enableAutoCommit: Boolean = false
    ): ConsumerFactory<String, Any> {
        val props = mapOf(
            ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG to bootstrapServers,
            ConsumerConfig.GROUP_ID_CONFIG to groupId,
            ConsumerConfig.AUTO_OFFSET_RESET_CONFIG to autoOffsetReset,
            ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG to enableAutoCommit,
            ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG to StringDeserializer::class.java,
            ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG to JsonDeserializer::class.java,
            JsonDeserializer.TRUSTED_PACKAGES to "*",
            ConsumerConfig.ISOLATION_LEVEL_CONFIG to "read_committed"
        )
        return DefaultKafkaConsumerFactory(props)
    }

    fun createListenerContainerFactory(
        consumerFactory: ConsumerFactory<String, Any>,
        ackMode: ContainerProperties.AckMode = ContainerProperties.AckMode.MANUAL
    ): ConcurrentKafkaListenerContainerFactory<String, Any> {
        val factory = ConcurrentKafkaListenerContainerFactory<String, Any>()
        factory.consumerFactory = consumerFactory
        factory.containerProperties.ackMode = ackMode
        return factory
    }
}
