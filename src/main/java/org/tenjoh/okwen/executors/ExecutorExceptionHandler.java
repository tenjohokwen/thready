package org.tenjoh.okwen.executors;

import com.codahale.metrics.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class ExecutorExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(ExecutorExceptionHandler.class);

    public static LogFlagException handleException(Exception e, ClientContext clientContext, Meter meter) {
        traceToClient(e, clientContext, meter);
        final LogFlagException logFlagException = new LogFlagException();
        log.error("Exception in task submitted from thread '{}', with help_code: '{}'", clientContext.clientThreadName, logFlagException.getHelpCode(), e);
        return logFlagException;

    }

    private static void traceToClient(Exception e, ClientContext clientContext, Meter meter) {
        modifyStackTrace(clientContext, e);
        meter.mark();
    }

    private static void modifyStackTrace(ClientContext clientContext, Exception e) {
        StackTraceElement[] stackTraceElements = mergeStacks(e.getStackTrace(), clientContext.clientStack.getStackTrace(), clientContext.clientThreadName);
        e.setStackTrace(stackTraceElements);
    }

    /**
     * combine stack traces making them appear as one.
     * This is part of the bigger effort to get full stack traces when exceptions are thrown in submitted tasks/runnables
     * @param currentStack the most recent stack
     * @param oldStack the stack got from the client
     * @param clientThreadName The name of the client thread
     * @return StackTraceElement[] merged stack trace
     */
    private static StackTraceElement[] mergeStacks(StackTraceElement[] currentStack, StackTraceElement[] oldStack, String clientThreadName) {
        StackTraceElement[] combined = new StackTraceElement[currentStack.length + oldStack.length + 1];
        System.arraycopy(currentStack, 0, combined, 0, currentStack.length);
        combined[currentStack.length] = new StackTraceElement("══════════════════════════", "<client call: Thread name:" + clientThreadName + ">", "", -1);
        System.arraycopy(oldStack, 0, combined, currentStack.length+1, oldStack.length);
        return combined;
    }

}
