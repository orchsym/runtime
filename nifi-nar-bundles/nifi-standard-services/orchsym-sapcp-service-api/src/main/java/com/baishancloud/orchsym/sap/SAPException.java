package com.baishancloud.orchsym.sap;

/**
 * @author GU Guoqiang
 *
 */
public class SAPException extends Exception {

    private static final long serialVersionUID = -2298108603372230169L;

    public SAPException(String message, Throwable cause) {
        super(message, cause);
    }

    public SAPException(String message) {
        super(message);
    }

    public SAPException(Throwable cause) {
        super(cause);
    }

}
