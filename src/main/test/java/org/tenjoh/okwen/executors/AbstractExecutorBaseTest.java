package org.tenjoh.okwen.executors;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import com.codahale.metrics.MetricRegistry;
import org.apache.log4j.MDC;
import org.slf4j.LoggerFactory;
import org.tenjoh.okwen.executors.util.MemoryAppender;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.function.Predicate;

import static com.google.common.collect.Lists.newArrayList;

/**
 * Created by mokwen on 13.02.16.
 */
public class AbstractExecutorBaseTest {

    protected static final String REQUEST_ID_KEY = "requestId";
    protected final MemoryAppender memAppender = new MemoryAppender();
    private final List<Class> loggerClasses = Collections.synchronizedList(newArrayList());
    protected final MetricRegistry defaultRegistry = new MetricRegistry();

    protected void onSetUp() throws Exception {
        memAppender.start();
    }

    protected void onCleanUp() throws Exception {
        memAppender.reset();
        loggerClasses.stream().forEach(clazz -> removeMemAppenderFromLogger(clazz));
        loggerClasses.clear();
    }

    protected void addMemAppenderToLogger(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.addAppender(memAppender);
        logger.setLevel(Level.DEBUG);
        loggerClasses.add(clazz);
    }

    private void removeMemAppenderFromLogger(Class clazz) {
        Logger logger = (Logger) LoggerFactory.getLogger(clazz);
        logger.detachAppender(memAppender);
    }

    protected long filterByMsgByThreadNameByReqId(String msg, String threadPrefix, String reqId) {
        return memAppender.countEvents(formattedMsgStartsWith(msg).and(threadNameStartsWith(threadPrefix).and(mdcMapContainsReqId(reqId))));
    }


    protected long countByRequestIdByThreadPrefixPredicate(String requestIdValue, String threadPrefix) {
        return memAppender.countEvents(containsRequestId(requestIdValue).and(threadNameStartsWith(threadPrefix)));
    }

    protected Predicate<ILoggingEvent> mdcMapContainsReqId(String value) {
        return t -> t.getMDCPropertyMap().get(REQUEST_ID_KEY).equals(value);
    }

    protected Predicate<ILoggingEvent> containsRequestId(String reqIdValue) {
        return t -> t.getFormattedMessage().contains(reqIdValue);
    }

    protected Predicate<ILoggingEvent> formattedMsgStartsWith(String msgPrefix) {
        return t -> t.getFormattedMessage().startsWith(msgPrefix);
    }

    protected Predicate<ILoggingEvent> threadNameStartsWith(String threadPrefix) {
        return t -> t.getThreadName().startsWith(threadPrefix);
    }


    private static void executeTask(String taskName, org.slf4j.Logger logger) {
        final String originalName = Thread.currentThread().getName();
        try {
            if(taskName != null) {
                Thread.currentThread().setName(originalName + "--" + taskName);
            }
            runTask(logger);
        } finally {
            Thread.currentThread().setName(originalName);
        }
    }

    private static void runTask(org.slf4j.Logger logger) {
        logger.info("Running with request id '{}'", MDC.get("requestId"));
        try {
            TimeUnit.SECONDS.sleep(2);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        throw new RuntimeException("Exception from with the run method");
    }


    protected static class JobWithException implements Runnable {

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(JobWithException.class);

        private final String TASK_NAME;

        public JobWithException() {
            this.TASK_NAME = null;
        }

        public JobWithException(String taskName) {
            this.TASK_NAME = taskName;
        }

        @Override
        public void run() {
            executeTask(TASK_NAME, logger);
        }
    }

    protected static class CallableWithException implements Callable<Void> {

        private static final org.slf4j.Logger logger = LoggerFactory.getLogger(CallableWithException.class);

        private final String TASK_NAME;

        public CallableWithException(String task_name) {
            TASK_NAME = task_name;
        }

        @Override
        public Void call() throws Exception {
            executeTask(TASK_NAME, logger);
            return null;
        }
    }

}
