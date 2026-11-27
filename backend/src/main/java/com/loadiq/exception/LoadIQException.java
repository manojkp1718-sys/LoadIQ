
package com.loadiq.exception;

public class LoadIQException extends RuntimeException {

    public LoadIQException(String message) {
        super(message);
    }

    public LoadIQException(String message, Throwable cause) {
        super(message, cause);
    }
}
