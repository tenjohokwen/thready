package org.tenjoh.okwen.executors.util;


import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.Predicate;

/**
 * Appender for testing purposes.
 * Holds log messages in memory.
 */
public class MemoryAppender extends AppenderBase<ILoggingEvent> {
    private final List<ILoggingEvent> events = Collections.synchronizedList(new ArrayList<>());

    @Override
    protected void append(ILoggingEvent loggingEvent) {
        events.add(loggingEvent);
    }

    public void reset() {
        events.clear();
    }

    public boolean hasMatch(Predicate<? super ILoggingEvent> predicate) {
        return events.stream().anyMatch(predicate);
    }

    public long countEvents(Predicate<? super ILoggingEvent> predicate) {
        return events.stream().filter(predicate).count();
    }
}
