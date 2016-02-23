package org.tenjoh.okwen.executors;

import com.codahale.metrics.Gauge;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import static com.codahale.metrics.MetricRegistry.name;

class ExecutorInstrumentor {

    public static final String QUEUE_DELAY_POSTFIX = "queue-delay";
    public static final String EXCEPTION_POSTFIX = "exception-rate";
    public static final String ACTIVE_THREAD_COUNT_POSTFIX = "active-thread-count";
    public static final String COMPLETED_TASK_COUNT_POSTFIX = "completed-task-count";
    public static final String AGGREGATED_TASK_COUNT_POSTFIX = "aggregated-task-count";
    public static final String QUEUE_SIZE_POSTFIX = "queue-size";
    public static final String LARGEST_POOL_SIZE_POSTFIX = "largest-pool-size";
    public static final String CURRENT_POOL_SIZE_POSTFIX = "current-pool-size";

    public static void configureMetrics(MetricRegistry metricsRegistry, String givenName, ThreadPoolExecutor executor) {
        metricsRegistry.<Gauge<Integer>>register(name(givenName, "core-pool-size"), executor::getCorePoolSize);
        metricsRegistry.<Gauge<Integer>>register(name(givenName, "max-pool-size"), executor::getMaximumPoolSize);
        metricsRegistry.<Gauge<Integer>>register(name(givenName, CURRENT_POOL_SIZE_POSTFIX), executor::getPoolSize);
        metricsRegistry.<Gauge<Long>>register(name(givenName, "keep-alive-time-seconds"), () -> executor.getKeepAliveTime(TimeUnit.SECONDS));
        metricsRegistry.<Gauge<Integer>>register(name(givenName, ACTIVE_THREAD_COUNT_POSTFIX), executor::getActiveCount);
        metricsRegistry.<Gauge<Long>>register(name(givenName, COMPLETED_TASK_COUNT_POSTFIX), executor::getCompletedTaskCount);
        metricsRegistry.<Gauge<Long>>register(name(givenName, AGGREGATED_TASK_COUNT_POSTFIX), executor::getTaskCount);
        metricsRegistry.<Gauge<Integer>>register(name(givenName, QUEUE_SIZE_POSTFIX), () -> executor.getQueue().size());
        metricsRegistry.<Gauge<Integer>>register(name(givenName, LARGEST_POOL_SIZE_POSTFIX), executor::getLargestPoolSize);
        metricsRegistry.timer(MetricRegistry.name(givenName, QUEUE_DELAY_POSTFIX));
        metricsRegistry.register(name(givenName, EXCEPTION_POSTFIX), new Meter());
    }


}
