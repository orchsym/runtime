package com.baishancloud.orchsym.sap.client;

import org.apache.avro.generic.GenericRecord;

import com.baishancloud.orchsym.sap.SAPConnectionPoolService;
import com.baishancloud.orchsym.sap.SAPException;

/**
 * Definition for SAP Client Connection Pooling Service.
 * 
 * @author GU Guoqiang
 *
 */
public interface SAPClientConnectionPoolService extends SAPConnectionPoolService {
    String KEY_SYSID = "systemID";
    String KEY_SYSNO = "systemNumber";

    boolean isConnected();

    Object call(String function, GenericRecord readRecord, boolean ignoreEmptyValues, String... exportTables) throws SAPException;
}
