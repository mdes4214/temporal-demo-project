package org.acme.temporal.demo

import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import io.temporal.client.WorkflowClient
import io.temporal.serviceclient.WorkflowServiceStubs
import io.temporal.worker.WorkerFactory
import org.acme.temporal.demo.workflow.DemoActivityExecutorImpl
import org.acme.temporal.demo.workflow.DemoWorkflowImpl
import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes

@ApplicationScoped
class WorkflowApplicationObserver {
    private lateinit var client: WorkflowClient
    private lateinit var factory: WorkerFactory

    @ConfigProperty(name = "processing.order.task.queue")
    var taskQueue: String? = null

    fun onStart(@Observes ev: StartupEvent) {
        val service = WorkflowServiceStubs.newInstance()
        client = WorkflowClient.newInstance(service)
        factory = WorkerFactory.newInstance(client)

        val worker = factory.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(DemoWorkflowImpl::class.java)
        worker.registerActivitiesImplementations(DemoActivityExecutorImpl())

        factory.start()
    }

    fun onStop(@Observes ev: ShutdownEvent) {
        factory.shutdown()
    }

    fun getClient(): WorkflowClient = client
}