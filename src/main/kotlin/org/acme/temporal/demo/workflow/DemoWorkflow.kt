package org.acme.temporal.demo.workflow

import io.temporal.workflow.QueryMethod
import io.temporal.workflow.SignalMethod
import io.temporal.workflow.WorkflowInterface
import io.temporal.workflow.WorkflowMethod
import org.acme.temporal.demo.model.Order

@WorkflowInterface
interface DemoWorkflow {

    @WorkflowMethod
    fun processOrder(order: Order): Order

    @QueryMethod
    fun getStatus(): String

    @SignalMethod
    fun approve(approver: String)

    @SignalMethod
    fun exit()
}