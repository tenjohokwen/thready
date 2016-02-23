package org.tenjoh.okwen.executors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.*;

/**
 * An MDC aware ScheduledThreadPoolExecutor implementation that ensures proper inheritance of the MDC context
 * The methods NOT overridden in this class all delegate directly or indirectly to those overridden here.
 * Make use of the shutdown hooks
 */
public class InstrumentedMDCAwareScheduledExecutor extends ScheduledThreadPoolExecutor {

    private final MetricRegistry metricRegistry;
    private final String METRICS_PREFIX;

    public InstrumentedMDCAwareScheduledExecutor(int corePoolSize, String poolPrefix, RejectedExecutionHandler handler, MetricRegistry metricRegistry) {
        super(corePoolSize, new ThreadFactoryBuilder().setNameFormat(poolPrefix + "-%d").build(), handler);
        this.metricRegistry = metricRegistry;
        this.METRICS_PREFIX = poolPrefix;
        ExecutorInstrumentor.configureMetrics(metricRegistry, METRICS_PREFIX, this);
    }

    @Override
    public ScheduledFuture<?> schedule(Runnable command, long delay, TimeUnit unit) {
        return super.schedule(instrument(command), delay, unit);
    }

    @Override
    public <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit) {
        return super.schedule(instrument(callable), delay, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(Runnable command, long initialDelay, long period, TimeUnit unit) {
        return super.scheduleAtFixedRate(instrument(command), initialDelay, period, unit);
    }

    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(Runnable command, long initialDelay, long delay, TimeUnit unit) {
        return super.scheduleWithFixedDelay(instrument(command), initialDelay, delay, unit);
    }

    private Runnable instrument(Runnable runnable) {
        return InstrumentedMDCAwareExecutables.instrument(runnable, metricRegistry, METRICS_PREFIX);
    }

    private <T> Callable<T> instrument(Callable<T> callable) {
        return InstrumentedMDCAwareExecutables.instrument(callable, metricRegistry, METRICS_PREFIX);
    }
}
