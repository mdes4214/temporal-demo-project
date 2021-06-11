package org.acme.temporal.demo.utils

import io.temporal.activity.ActivityOptions
import io.temporal.common.RetryOptions
import io.temporal.workflow.Workflow
import org.acme.temporal.demo.workflow.DemoActivityExecutor
import java.time.Duration

class ActivityStubUtils {
    companion object {
        fun getActivitiesStubWithTimeout(timeoutMinutes: Long): DemoActivityExecutor = Workflow.newActivityStub(
            DemoActivityExecutor::class.java,
            ActivityOptions.newBuilder()
                .setStartToCloseTimeout(Duration.ofMinutes(timeoutMinutes))
                .build()
        )

        fun getActivitiesStubWithTimeoutAndRetries(
            timeoutMinutes: Long,
            retryIntervalSeconds: Long,
            retryTimes: Int
        ): DemoActivityExecutor = Workflow.newActivityStub(
            DemoActivityExecutor::class.java,
            ActivityOptions.newBuilder()
                .setRetryOptions(getRetryOptions(retryIntervalSeconds, retryTimes))
                .setStartToCloseTimeout(Duration.ofMinutes(timeoutMinutes))
                .build()
        )

        private fun getRetryOptions(retryIntervalSeconds: Long, retryTimes: Int): RetryOptions =
            RetryOptions.newBuilder()
                .setInitialInterval(Duration.ofSeconds(retryIntervalSeconds))
                .setMaximumAttempts(retryTimes)
                .build()
    }
}