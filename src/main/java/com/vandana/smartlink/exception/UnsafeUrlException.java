package com.vandana.smartlink.exception;

public class UnsafeUrlException extends RuntimeException {

    public UnsafeUrlException(String message) {
        super(message);
    }

    public UnsafeUrlException(String message, Throwable cause) {
        super(message, cause);
    }
}
