package com.baishancloud.orchsym.sap.server;

/**
 * @author GU Guoqiang
 *
 */
public class SAPServerException extends RuntimeException {

    private static final long serialVersionUID = -2298108603372230169L;

    public SAPServerException(String message, Throwable cause) {
        super(message, cause);
    }

    public SAPServerException(String message) {
        super(message);
    }

    public SAPServerException(Throwable cause) {
        super(cause);
    }

}
