package org.tenjoh.okwen.executors;

import com.codahale.metrics.MetricRegistry;
import com.jayway.awaitility.Awaitility;
import org.apache.log4j.MDC;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;

public class InstrumentedMDCAwareScheduledExecutorTest extends AbstractExecutorBaseTest {

    @Before
    public void setUp() throws Exception {
        onSetUp();
    }

    @After
    public void tearDown() throws Exception {
        onCleanUp();
    }

    @Test
    public void threadMdcShouldNotBeDirty() throws Exception {
        addMemAppenderToLogger(InstrumentedMDCAwareExecutables.class);
        String threadPrefix = "CLEAN_MDC_CXT";
        String reqId = "rcmc987";
        final InstrumentedMDCAwareScheduledExecutor scheduledExecutor = createScheduledExecutor(threadPrefix);
        MDC.put("requestId", reqId);
        final AtomicInteger callCount = new AtomicInteger(0);
        scheduledExecutor.scheduleAtFixedRate(() -> callCount.incrementAndGet(), 1, 2, TimeUnit.SECONDS);
        MDC.put("requestId", "r111");
        Awaitility.waitAtMost(10, TimeUnit.SECONDS).until(() -> countByRequestIdByThreadPrefixPredicate(reqId, threadPrefix) > 2);

        //in order to avoid inconsistency during verification first shutdown executor
        scheduledExecutor.shutdownNow();
        //Assert that all logged events have initial request id and not the second one that was passed to MDC
        long expectedCount = callCount.get() * 2; //each call logs twice
        assertThat(memAppender.countEvents(t -> true)).isEqualTo(expectedCount);
    }

    private InstrumentedMDCAwareScheduledExecutor createScheduledExecutor(String poolName) {
        return new InstrumentedMDCAwareScheduledExecutor(7, poolName, new ThreadPoolExecutor.CallerRunsPolicy(), new MetricRegistry());
    }

    @Test
    public void givenScheduledCallableWithExceptionShouldLogIt() throws Exception {
        testJobWithException(exe -> exe.schedule(new CallableWithException("LOG_CALLABLE"), 2, TimeUnit.SECONDS));
    }

    @Test
    public void givenScheduleAtFixedRateExceptionJobShouldLogIt() throws Exception {
        testJobWithException(exe -> exe.scheduleAtFixedRate(new JobWithException("FIXED_RATE_LOG"), 2, 60, TimeUnit.SECONDS));
    }

    @Test
    public void givenScheduleWithFixedDelayExceptionJobShouldLogIt() throws Exception {
        testJobWithException(exe -> exe.scheduleWithFixedDelay(new JobWithException("FIXED_DELAY_LOG"), 2, 60, TimeUnit.SECONDS));
    }

    @Test
    public void givenScheduledJobWithExceptionShouldLogIt() throws InterruptedException {
        testJobWithException(exe -> exe.schedule(new JobWithException("justALog"), 2, TimeUnit.SECONDS));
    }

    private void testJobWithException(Consumer<InstrumentedMDCAwareScheduledExecutor> task) throws InterruptedException {
        addMemAppenderToLogger(ExecutorExceptionHandler.class);
        String threadPrefix = "logException";
        String reqId = "rwe123";
        MDC.put(REQUEST_ID_KEY, reqId);

        final InstrumentedMDCAwareScheduledExecutor executor = createScheduledExecutor(threadPrefix);
        task.accept(executor);

        executor.awaitTermination(6, TimeUnit.SECONDS);
        String expectedMsg = "Exception in task submitted from thread";
        final long expectedCount = 1;
        long count= filterByMsgByThreadNameByReqId(expectedMsg, threadPrefix, reqId);
        assertThat(count).isEqualTo(expectedCount);

    }

}