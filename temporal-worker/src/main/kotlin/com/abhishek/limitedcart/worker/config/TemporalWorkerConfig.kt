package com.abhishek.limitedcart.worker.config

import com.abhishek.limitedcart.common.temporal.TemporalFactories
import com.fasterxml.jackson.module.kotlin.jacksonObjectMapper
import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowClientOptions
import io.temporal.common.converter.DataConverter
import io.temporal.common.converter.DefaultDataConverter
import io.temporal.common.converter.JacksonJsonPayloadConverter
import io.temporal.serviceclient.WorkflowServiceStubs
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.client.JdkClientHttpRequestFactory
import org.springframework.web.client.RestClient
import java.time.Duration

@Configuration
class TemporalWorkerConfig(
    @Value("\${worker.temporal.target}") private val temporalTarget: String,
    @Value("\${services.inventory}") private val inventoryServiceUrl: String,
    @Value("\${services.payment}") private val paymentServiceUrl: String,
    @Value("\${services.orders}") private val orderServiceUrl: String
) {

    @Bean
    fun temporalDataConverter(): DataConverter {
        val mapper = jacksonObjectMapper()
        val jacksonConverter = JacksonJsonPayloadConverter(mapper)
        return DefaultDataConverter.newDefaultInstance()
            .withPayloadConverterOverrides(jacksonConverter)
    }

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs {
        return TemporalFactories.createWorkflowServiceStubs(temporalTarget)
    }

    @Bean
    fun workflowClient(
        serviceStubs: WorkflowServiceStubs,
        dataConverter: DataConverter
    ): WorkflowClient {
        val options = WorkflowClientOptions.newBuilder()
            .setDataConverter(dataConverter)
            .build()
        return WorkflowClient.newInstance(serviceStubs, options)
    }

    @Bean
    fun inventoryRestClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(10))
        }
        return RestClient.builder()
            .baseUrl(inventoryServiceUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun paymentRestClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(10))
        }
        return RestClient.builder()
            .baseUrl(paymentServiceUrl)
            .requestFactory(requestFactory)
            .build()
    }

    @Bean
    fun orderRestClient(): RestClient {
        val requestFactory = JdkClientHttpRequestFactory().apply {
            setReadTimeout(Duration.ofSeconds(10))
        }
        return RestClient.builder()
            .baseUrl(orderServiceUrl)
            .requestFactory(requestFactory)
            .build()
    }
}
