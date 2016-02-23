package org.tenjoh.okwen.executors;

import org.apache.log4j.MDC;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

/**
* Created by mokwen on 06.02.16.
*/
class Job implements Runnable {

    private static final Logger logger = LoggerFactory.getLogger(Job.class);

    private final String name;

    Job(String name) {
        this.name = name;
    }

    @Override
    public void run() {
        log();
        try {
            TimeUnit.SECONDS.sleep(3);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        log();
    }

    private void log() {
        logger.info("'{}' is running with request id '{}'", name, MDC.get("requestId"));
    }
}
