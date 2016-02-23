package org.tenjoh.okwen.executors;

import org.slf4j.MDC;

import java.util.Map;

/**
 * Wraps MDC and provides some convenience methods/fields
 */
public class MDCWrapper {

    public static void setContext(Map<String, String> currentThreadCxt) {
        if(currentThreadCxt != null ) {
            MDC.setContextMap(currentThreadCxt);
        } else {
            MDC.clear();
        }
    }

}
