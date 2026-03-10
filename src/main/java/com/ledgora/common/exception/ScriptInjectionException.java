package com.ledgora.common.exception;

/** Exception thrown when script injection is detected in user input. */
public class ScriptInjectionException extends RuntimeException {

    public ScriptInjectionException(String message) {
        super(message);
    }
}
