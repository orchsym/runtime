package com.baishancloud.orchsym.sap.server;

/**
 * @author GU Guoqiang
 *
 */
public interface SAPRequestCallback {

    String getIdentifier();

    boolean ignoreEmptyValue();

    void process(final Object importData);

    /**
     * will block to wait the flow to send response.
     */
    String waitResponse() throws SAPServerException;
}
