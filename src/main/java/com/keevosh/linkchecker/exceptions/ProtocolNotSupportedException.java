package com.keevosh.linkchecker.exceptions;

import java.net.MalformedURLException;

public class ProtocolNotSupportedException extends MalformedURLException {

    /**
     * 
     */
    private static final long serialVersionUID = 1L;

    /**
     * 
     */
    public ProtocolNotSupportedException() {
        super();
    }

    /**
     * @param msg
     */
    public ProtocolNotSupportedException(String msg) {
        super(msg);
    }
}