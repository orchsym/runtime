package com.baishancloud.orchsym.sap.server;

import com.baishancloud.orchsym.sap.SAPConnectionPoolService;

/**
 * @author GU Guoqiang
 *
 */
public interface SAPServerConnectionPoolService extends SAPConnectionPoolService {

    void registryRequest(final SAPRequestCallback requestCallback) throws SAPServerException;

    void unregistryRequest(final String identifier);
}
