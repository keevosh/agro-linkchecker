package com.keevosh.linkchecker.exceptions;

public class LinkCheckerException extends RuntimeException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public LinkCheckerException() {
        super();
    }

    /**
     * @param message
     * @param cause
     */
    public LinkCheckerException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message
     */
    public LinkCheckerException(String message) {
        super(message);
    }
}
