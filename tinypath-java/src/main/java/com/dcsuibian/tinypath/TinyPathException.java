package com.dcsuibian.tinypath;

/**
 * Thrown when a TinyPath expression contains a syntax error or the input JSON is invalid.
 */
public class TinyPathException extends RuntimeException {
    /** @param message the detail message */
    public TinyPathException(String message) {
        super(message);
    }
}
