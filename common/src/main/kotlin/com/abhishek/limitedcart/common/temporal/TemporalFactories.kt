package com.abhishek.limitedcart.common.temporal

import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.serviceclient.WorkflowServiceStubsOptions
import io.temporal.worker.Worker
import io.temporal.worker.WorkerFactory

/**
 * Temporal factory utilities (NO Spring annotations).
 * Each service creates its own @Configuration and uses these helpers.
 */
object TemporalFactories {

    fun createWorkflowServiceStubs(targetAddress: String): WorkflowServiceStubs {
        val options = WorkflowServiceStubsOptions.newBuilder()
            .setTarget(targetAddress)
            .build()
        return WorkflowServiceStubs.newServiceStubs(options)
    }

    fun createWorkflowClient(serviceStubs: WorkflowServiceStubs): WorkflowClient {
        return WorkflowClient.newInstance(serviceStubs)
    }

    fun createWorkerFactory(workflowClient: WorkflowClient): WorkerFactory {
        return WorkerFactory.newInstance(workflowClient)
    }

    fun createWorker(
        workerFactory: WorkerFactory,
        taskQueue: String
    ): Worker {
        return workerFactory.newWorker(taskQueue)
    }
}
