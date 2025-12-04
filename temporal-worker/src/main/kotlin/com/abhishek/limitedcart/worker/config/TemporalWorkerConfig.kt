package com.abhishek.limitedcart.worker.config

import io.temporal.client.WorkflowClient
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
    fun workflowServiceStubs(): WorkflowServiceStubs = WorkflowServiceStubs.newServiceStubs(
        io.temporal.serviceclient.WorkflowServiceStubsOptions.newBuilder()
            .setTarget(temporalTarget)
            .build()
    )

    @Bean
    fun workflowClient(serviceStubs: WorkflowServiceStubs): WorkflowClient =
        WorkflowClient.newInstance(serviceStubs)

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
