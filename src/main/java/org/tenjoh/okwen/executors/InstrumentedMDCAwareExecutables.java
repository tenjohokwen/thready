package org.tenjoh.okwen.executors;

import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.util.Map;
import java.util.concurrent.Callable;

import static com.codahale.metrics.MetricRegistry.name;


class InstrumentedMDCAwareExecutables {

    private static final Logger logger = LoggerFactory.getLogger(InstrumentedMDCAwareExecutables.class);

    static Runnable instrument(Runnable task, MetricRegistry registry, String metricPrefix) {
        return new MDCAwareRunner(task, registry, metricPrefix);
    }


    static class MDCAwareRunner implements Runnable {

        private final Runnable runnable;

        /* This is the context from the thread that submits the runnable */
        private final ClientContext clientContext = ClientContext.instance();

        /* measure queue delay*/
        private final Timer.Context context;

        private final MetricRegistry registry;

        private final String metricPrefix;

        private MDCAwareRunner(Runnable runnable, MetricRegistry registry, String metricPrefix) {
            this.runnable = runnable;
            this.registry = registry;
            this.context = registry.timer(MetricRegistry.name(metricPrefix, ExecutorInstrumentor.QUEUE_DELAY_POSTFIX)).time();
            this.metricPrefix = metricPrefix;
        }

        @Override
        public void run() {
            /* pre-existing context from worker thread*/
            Map<String, String> contextFromPooledThread = MDC.getCopyOfContextMap();
            try {
                //inherit the context from the client
                clientContext.setClientMDCContext();
                logger.debug("About to run runnable with id '{}'" , MDC.get("requestId"));
                context.stop();
                this.runnable.run();
            } catch (Exception e) {
                throw ExecutorExceptionHandler.handleException(e, clientContext, registry.meter(name(metricPrefix, ExecutorInstrumentor.EXCEPTION_POSTFIX)));
            } finally {
                logger.debug("Cleaning runnable with id '{}'" , MDC.get("requestId"));
                MDCWrapper.setContext(contextFromPooledThread);
            }
        }
    }


    static <T> Callable<T> instrument(Callable<T> callable, MetricRegistry registry, String metricPrefix) {
        return new MDCAwareCallable<>(callable, registry, metricPrefix);
    }

    static class MDCAwareCallable<V> implements Callable {

        private final Callable<V> callable;

        /* This is the context from the thread that submits the runnable */
        private final ClientContext clientContext = ClientContext.instance();

        /* measure queue delay*/
        private final Timer.Context context;

        private final MetricRegistry registry;

        private final String metricPrefix;

        MDCAwareCallable(Callable<V> callable, MetricRegistry registry, String metricPrefix) {
            this.callable = callable;
            this.registry = registry;
            this.context = registry.timer(MetricRegistry.name(metricPrefix, ExecutorInstrumentor.QUEUE_DELAY_POSTFIX)).time();
            this.metricPrefix = metricPrefix;
        }

        @Override
        public V call() throws Exception {
            /* pre-existing context from worker thread*/
            Map<String, String> contextFromPooledThread = MDC.getCopyOfContextMap();
            try {
                //inherit the context from the client
                clientContext.setClientMDCContext();
                logger.info("About to run callable with id '{}'" , MDC.get("requestId"));
                context.stop();
                return this.callable.call();
            } catch (Exception e) {
                throw ExecutorExceptionHandler.handleException(e, clientContext, registry.meter(name(metricPrefix, ExecutorInstrumentor.EXCEPTION_POSTFIX)));
            } finally {
                logger.info("Cleaning callable with id '{}'" , MDC.get("requestId"));
                MDCWrapper.setContext(contextFromPooledThread);
            }
        }
    }
}
