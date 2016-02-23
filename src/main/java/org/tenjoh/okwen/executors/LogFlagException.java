package org.tenjoh.okwen.executors;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

public class LogFlagException extends RuntimeException {

    /*
    Intended to be used to avoid logging this exception multiple times
     */
    private final boolean logged;

    private final String helpCode = UUID.randomUUID().toString();

    private final List<Map<String, Object>> errors = new ArrayList<>();

    /**
     * Use this method if exception is already logged
     */
    public LogFlagException() {
        this.logged = true;
    }

    /**
     * Use this method if exception and root cause should be logged
     * @param cause
     */
    public LogFlagException(Throwable cause) {
        super(cause);
        logged = false;
    }

    public boolean isLogged() {
        return logged;
    }

    public String getHelpCode() {
        return helpCode;
    }

    public List<Map<String, Object>> getErrors() {
        return errors;
    }

    public void addErrorDetails(Map<String, Object> details) {
        errors.add(details);
    }

}
