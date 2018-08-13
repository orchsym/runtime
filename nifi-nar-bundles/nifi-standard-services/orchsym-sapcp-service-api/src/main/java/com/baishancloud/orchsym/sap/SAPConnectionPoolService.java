package com.baishancloud.orchsym.sap;

import java.util.Map;

import org.apache.nifi.controller.ControllerService;

/**
 * Definition for SAP Client Connection Pooling Service.
 * 
 * @author GU Guoqiang
 *
 */
public interface SAPConnectionPoolService extends ControllerService {

    void connect() throws SAPException;

    Map<String, String> getAttributes() throws SAPException;

}
