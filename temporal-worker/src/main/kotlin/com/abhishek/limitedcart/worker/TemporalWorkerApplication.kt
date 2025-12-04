package com.abhishek.limitedcart.worker

import com.abhishek.limitedcart.worker.activities.impl.InventoryActivitiesImpl
import com.abhishek.limitedcart.worker.activities.impl.OrderActivitiesImpl
import com.abhishek.limitedcart.worker.activities.impl.PaymentActivitiesImpl
import com.abhishek.limitedcart.worker.workflow.OrderWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.worker.WorkerFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.runApplication

@SpringBootApplication(exclude = [DataSourceAutoConfiguration::class])
class TemporalWorkerApplication(
    private val workflowClient: WorkflowClient,
    private val inventoryActivitiesImpl: InventoryActivitiesImpl,
    private val paymentActivitiesImpl: PaymentActivitiesImpl,
    private val orderActivitiesImpl: OrderActivitiesImpl,
    @Value("\${worker.temporal.task-queue}") private val taskQueue: String
) {

    @PostConstruct
    fun startWorker() {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(OrderWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(
            inventoryActivitiesImpl,
            paymentActivitiesImpl,
            orderActivitiesImpl
        )
        factory.start()
    }

    companion object {
        @JvmStatic
        fun main(args: Array<String>) {
            runApplication<TemporalWorkerApplication>(*args)
        }
    }
}
