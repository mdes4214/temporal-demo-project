package org.acme.temporal.demo

import io.temporal.client.WorkflowClient
import io.temporal.client.WorkflowOptions
import io.temporal.testing.TestWorkflowEnvironment
import io.temporal.testing.WorkflowReplayer
import io.temporal.worker.Worker
import org.acme.temporal.demo.model.Goods
import org.acme.temporal.demo.model.Order
import org.acme.temporal.demo.model.OrderStatus
import org.acme.temporal.demo.workflow.DemoActivityExecutor
import org.acme.temporal.demo.workflow.DemoWorkflow
import org.acme.temporal.demo.workflow.DemoWorkflowImpl
import org.junit.jupiter.api.*
import java.lang.Exception
import org.mockito.Matchers.anyString
import org.mockito.Mockito.*
import java.util.*


@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class DemoWorkflowTest {
    private lateinit var testEnv: TestWorkflowEnvironment
    private lateinit var worker: Worker
    private lateinit var client: WorkflowClient
    private val taskQueue = "TestOrderProcessingTaskQueue"

    @BeforeAll
    fun setUp() {
        testEnv = TestWorkflowEnvironment.newInstance()
        worker = testEnv.newWorker(taskQueue)
        worker.registerWorkflowImplementationTypes(DemoWorkflowImpl::class.java)
        client = testEnv.getWorkflowClient()
    }

    @AfterAll
    fun tearDown() {
        testEnv.close()
    }

    @Test
    fun testMockedOrderProcessing() {
        // mock our workflow activities
        val activities: DemoActivityExecutor = mock(DemoActivityExecutor::class.java)
        val mockGoods = Goods(
            goodsName = "Toy No.23",
            goodsPrice = 30,
            goodsId = UUID.randomUUID().toString()
        )
        val currentDate = Date()
        val mockOrder = Order(
            orderId = UUID.randomUUID().toString(),
            status = OrderStatus.New,
            goodsId = mockGoods.goodsId,
            orderDate = Date(),
            isCheck = false
        )

        // mock activity methods
        `when`(activities.pickGoods(anyString())).thenReturn(mockGoods)
        `when`(activities.shipOrder(any(), anyBoolean())).thenReturn(currentDate)

        worker!!.registerActivitiesImplementations(activities)
        testEnv!!.start()

        val workflow: DemoWorkflow = client!!.newWorkflowStub(
            DemoWorkflow::class.java, WorkflowOptions.newBuilder()
                .setWorkflowId(mockOrder.orderId)
                .setTaskQueue(taskQueue).build()
        )

        // Execute a workflow waiting for it to complete.
        val resultOrder: Order = workflow.processOrder(mockOrder)

        // checks
        Assertions.assertNotNull(resultOrder)
        Assertions.assertEquals(true, resultOrder.isCheck)
        Assertions.assertEquals("Jack Lo", resultOrder.checkEmpl)
        Assertions.assertEquals(OrderStatus.Shipped, resultOrder.status)
        Assertions.assertEquals(currentDate, resultOrder.shipDate)
    }

//    @Test
    @Throws(Exception::class)
    fun testOderProcessingReplay() {
        WorkflowReplayer.replayWorkflowExecutionFromResource(
            "order_processing_hist.json", DemoWorkflowImpl::class.java
        )
    }
}