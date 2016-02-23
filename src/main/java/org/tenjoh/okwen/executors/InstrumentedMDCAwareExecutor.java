package org.tenjoh.okwen.executors;

import com.codahale.metrics.MetricRegistry;
import com.google.common.util.concurrent.ThreadFactoryBuilder;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 *  Ensures that all methods of the ThreadPoolExecutor are able to inherit the MDC context from calling threads.
 *  Just the {@link InstrumentedMDCAwareExecutor#execute(Runnable) execute} method is overridden because every other method uses it.
 *  Always make use of the shutdown hook for graceful shutdown
 *
 **/
public class InstrumentedMDCAwareExecutor extends ThreadPoolExecutor {

    private final MetricRegistry metricRegistry;
    private final String METRICS_PREFIX;

    public InstrumentedMDCAwareExecutor(int corePoolSize,
                                        int maximumPoolSize,
                                        long keepAliveTimeSecs,
                                        BlockingQueue<Runnable> workQueue,
                                        String poolPrefix,
                                        RejectedExecutionHandler handler,
                                        MetricRegistry metricRegistry) {
        super(corePoolSize, maximumPoolSize, keepAliveTimeSecs, TimeUnit.SECONDS, workQueue, new ThreadFactoryBuilder().setNameFormat(poolPrefix + "-%d").build(), handler);
        this.metricRegistry = metricRegistry;
        this.METRICS_PREFIX = poolPrefix;
        ExecutorInstrumentor.configureMetrics(metricRegistry, METRICS_PREFIX, this);
    }


    @Override
    public void execute(Runnable command) {
        super.execute(InstrumentedMDCAwareExecutables.instrument(command, metricRegistry, METRICS_PREFIX));
    }

}
