package com.abhishek.limitedcart.order.config

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class TemporalConfig(
    @Value("\${app.temporal.address}") private val temporalAddress: String
) {

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs =
        WorkflowServiceStubs.newInstance(
            WorkflowServiceStubsOptions.newBuilder()
                .setTarget(temporalAddress)
                .build()
        )

    @Bean
    fun workflowClient(serviceStubs: WorkflowServiceStubs): WorkflowClient =
        WorkflowClient.newInstance(serviceStubs)
}
