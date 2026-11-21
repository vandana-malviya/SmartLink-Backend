package com.vandana.smartlink.exception;

public class LinkExpiredException extends RuntimeException {

    public LinkExpiredException(String message) {
        super(message);
    }

    public LinkExpiredException(String message, Throwable cause) {
        super(message, cause);
    }
}
