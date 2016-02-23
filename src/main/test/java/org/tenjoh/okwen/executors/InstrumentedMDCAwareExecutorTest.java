package org.tenjoh.okwen.executors;

import com.codahale.metrics.Meter;
import com.jayway.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.MDC;

import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import static org.assertj.core.api.Assertions.assertThat;

public class InstrumentedMDCAwareExecutorTest extends AbstractExecutorBaseTest {

    private static final int DEFAULT_WAIT = 4;
    private static final int DEFAULT_LOG_COUNT = 2;

    @Before
    public void setUp() throws Exception {
        onSetUp();
    }

    @After
    public void cleanUp() throws Exception {
        onCleanUp();
    }

    @Test
    public void givenClientMdcContextExecuteShouldInheritMdcContext() throws Exception {
        addMemAppenderToLogger(Job.class);
        String requestIdValue = "p123";
        String threadPrefix = "meow";
        executeDefault(requestIdValue, threadPrefix);

        assertFilteredLogCountByReqIdByThreadPrefix(requestIdValue, threadPrefix, DEFAULT_LOG_COUNT, DEFAULT_WAIT);
    }

    @Test
    public void givenThreadReuseShouldInheritNewMdcCxt() {
        addMemAppenderToLogger(Job.class);
        String threadPrefix = "reuse";
        final InstrumentedMDCAwareExecutor executor = executeDefault("p111", threadPrefix);
        String requestIdValue = "aaaaaa";
        MDC.put(REQUEST_ID_KEY, requestIdValue);
        executor.execute(new Job("AAA"));
        int waitTime = DEFAULT_WAIT * 2;
        assertFilteredLogCountByReqIdByThreadPrefix(requestIdValue, threadPrefix, DEFAULT_LOG_COUNT, waitTime);
    }

    @Test
    public void givenJobWithExceptionShouldLogIt() throws InterruptedException {
        addMemAppenderToLogger(ExecutorExceptionHandler.class);
        String threadPrefix = "withException";
        String reqId = "r9876";
        final long expectedCount = 1;
        final InstrumentedMDCAwareExecutor executor = executeDefault(reqId, threadPrefix, new JobWithException());

        String reqId2 = "r2";
        MDC.put(REQUEST_ID_KEY, reqId2);

        executor.execute(new JobWithException());
        executor.awaitTermination(7, TimeUnit.SECONDS);


        String expectedMsg = "Exception in task submitted from thread";
        long count= filterByMsgByThreadNameByReqId(expectedMsg, threadPrefix, reqId);
        assertThat(count).isEqualTo(expectedCount);

        count= filterByMsgByThreadNameByReqId(expectedMsg, threadPrefix, reqId2);
        assertThat(count).isEqualTo(expectedCount);

    }

    @Test
    public void givenJobWithExceptionShouldCountIt() throws InterruptedException {
        addMemAppenderToLogger(ExecutorExceptionHandler.class);
        String reqId = "e5712";
        String threadPrefix = "exception-thread";
        final InstrumentedMDCAwareExecutor executor = executeDefault(reqId, threadPrefix, new JobWithException(), 3, 4);
        executor.execute(new JobWithException());
        executor.execute(new JobWithException());
        executor.awaitTermination(7, TimeUnit.SECONDS);
        final Meter meter = defaultRegistry.meter(threadPrefix + "." + ExecutorInstrumentor.EXCEPTION_POSTFIX);
        int expectedExceptionCount = 3;
        assertThat(meter.getCount()).isEqualTo(expectedExceptionCount);
    }


    private void assertFilteredLogCountByReqIdByThreadPrefix(String requestIdValue, String threadPrefix, int expectedLogCount, int maxWaitSecs) {
        Awaitility.waitAtMost(maxWaitSecs, TimeUnit.SECONDS).until(() -> countByRequestIdByThreadPrefixPredicate(requestIdValue, threadPrefix) == expectedLogCount);
    }

    private InstrumentedMDCAwareExecutor executeDefault(String requestIdValue, String threadPrefix) {
        return executeDefault(requestIdValue, threadPrefix, new Job("Polo"));
    }

    private InstrumentedMDCAwareExecutor executeDefault(String requestIdValue, String threadPrefix, Runnable job) {
        return executeDefault(requestIdValue, threadPrefix, job, 1, 1);
    }

    private InstrumentedMDCAwareExecutor executeDefault(String requestIdValue, String threadPrefix, Runnable job, int poolSize, int maxPoolSize) {
        MDC.put(REQUEST_ID_KEY, requestIdValue);
        final InstrumentedMDCAwareExecutor executor = new InstrumentedMDCAwareExecutor(poolSize, maxPoolSize, 20, new LinkedBlockingQueue<>(2), threadPrefix, new ThreadPoolExecutor.CallerRunsPolicy(), defaultRegistry);

        executor.execute(job);
        //change requestId on main thread while 'Job("Polo")' is still running (should not have effect on results if well implemented)
        MDC.put(REQUEST_ID_KEY, "woofwoof");
        return executor;
    }

}