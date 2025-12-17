package com.abhishek.limitedcart.worker

import com.abhishek.limitedcart.common.constants.TaskQueues
import com.abhishek.limitedcart.worker.activities.impl.InventoryActivitiesImpl
import com.abhishek.limitedcart.worker.activities.impl.OrderActivitiesImpl
import com.abhishek.limitedcart.worker.activities.impl.PaymentActivitiesImpl
import com.abhishek.limitedcart.worker.workflow.OrderWorkflowImpl
import io.temporal.client.WorkflowClient
import io.temporal.worker.WorkerFactory
import jakarta.annotation.PostConstruct
import org.springframework.boot.autoconfigure.SpringBootApplication
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration
import org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration
import org.springframework.boot.actuate.autoconfigure.security.servlet.ManagementWebSecurityAutoConfiguration
import org.springframework.boot.runApplication


@SpringBootApplication(
    exclude = [
        DataSourceAutoConfiguration::class,
        org.springframework.boot.autoconfigure.orm.jpa.HibernateJpaAutoConfiguration::class,
        org.springframework.boot.autoconfigure.data.jpa.JpaRepositoriesAutoConfiguration::class,
        org.springframework.boot.autoconfigure.security.servlet.SecurityAutoConfiguration::class,
        ManagementWebSecurityAutoConfiguration::class
    ],
    scanBasePackages = [
        "com.abhishek.limitedcart.worker",
        "com.abhishek.limitedcart.common.workflow",
        "com.abhishek.limitedcart.common.exception"
    ]
)
class TemporalWorkerApplication(
    private val workflowClient: WorkflowClient,
    private val inventoryActivitiesImpl: InventoryActivitiesImpl,
    private val paymentActivitiesImpl: PaymentActivitiesImpl,
    private val orderActivitiesImpl: OrderActivitiesImpl
) {

    @PostConstruct
    fun startWorker() {
        val factory = WorkerFactory.newInstance(workflowClient)
        val worker = factory.newWorker(TaskQueues.ORDER_SAGA_QUEUE)
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
