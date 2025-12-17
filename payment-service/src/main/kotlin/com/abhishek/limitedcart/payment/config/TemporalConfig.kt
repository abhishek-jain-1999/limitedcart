package com.abhishek.limitedcart.payment.config

import com.abhishek.limitedcart.common.temporal.TemporalFactories
import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import org.springframework.beans.factory.annotation.Value
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration

@Configuration
class PaymentTemporalConfig(
    @Value("\${app.temporal.address:temporal:7233}") private val temporalAddress: String
) {

    @Bean
    fun workflowServiceStubs(): WorkflowServiceStubs {
        return TemporalFactories.createWorkflowServiceStubs(temporalAddress)
    }

    @Bean
    fun workflowClient(serviceStubs: WorkflowServiceStubs): WorkflowClient {
        return TemporalFactories.createWorkflowClient(serviceStubs)
    }
}
