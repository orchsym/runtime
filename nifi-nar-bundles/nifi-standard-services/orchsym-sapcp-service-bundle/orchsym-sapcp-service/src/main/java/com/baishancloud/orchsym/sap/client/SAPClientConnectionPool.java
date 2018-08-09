/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.baishancloud.orchsym.sap.client;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

import org.apache.avro.generic.GenericRecord;
import org.apache.commons.lang3.StringUtils;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.avro.AvroTypeUtil;

import com.baishancloud.orchsym.sap.SAPConnectionPool;
import com.baishancloud.orchsym.sap.SAPDataManager;
import com.baishancloud.orchsym.sap.SAPException;
import com.baishancloud.orchsym.sap.i18n.Messages;
import com.baishancloud.orchsym.sap.record.JCoRecordUtil;
import com.sap.conn.jco.JCoAttributes;
import com.sap.conn.jco.JCoDestination;
import com.sap.conn.jco.JCoException;
import com.sap.conn.jco.JCoFunction;

/**
 * Implementation of for SAP Connection Pooling Service.
 * 
 * @author GU Guoqiang
 */
@Tags({ "SAP", "RFC", "ABAP", "JCo", "client", "connection", "pooling", "Orchsym" })
@CapabilityDescription("Provides SAP Connection Pooling Service. Connections can be asked from pool and returned after usage.")
public class SAPClientConnectionPool extends SAPConnectionPool implements SAPClientConnectionPoolService {
    protected volatile JCoDestination destination;

    public void connect() throws SAPException {
        try {
            SAPDataManager.getInstance().updateClientProp(this.getIdentifier(), serverType, clientProperties);
            destination = SAPDataManager.getInstance().getDestination(this.getIdentifier(), serverType); // registry

            destination.ping();
        } catch (JCoException e) {
            throw new SAPException(Messages.getString("SAPConnectionPool.Disconnect"), e); //$NON-NLS-1$
        }
    }

    @Override
    public void shutdown() {
        destination = null;
        if (serverType != null)
            SAPDataManager.getInstance().updateClientProp(this.getIdentifier(), serverType, null);
    }

    @Override
    public boolean isConnected() {
        if (destination != null) {
            try {
                destination.ping();
                return true;
            } catch (JCoException e) {
                //
            }
        }
        return false;
    }

    public Map<String, String> getAttributes() throws SAPException {
        Map<String, String> attributes = new HashMap<>();
        if (destination != null) {
            try {
                final JCoAttributes jcoAttributes = destination.getAttributes();

                attributes.put(KEY_SYSID, jcoAttributes.getSystemID());
                attributes.put(KEY_SYSNO, jcoAttributes.getSystemNumber());
            } catch (JCoException e) {
                throw new SAPException(e.getMessage(), e);
            }
        }
        return attributes;
    }

    public Object call(String function, GenericRecord readRecord, boolean ignoreEmptyValues, String... exportTables) throws SAPException {
        if (StringUtils.isBlank(function)) {
            throw new SAPException(Messages.getString("SAPConnectionPool.EmptyFun")); //$NON-NLS-1$
        }

        try {
            final JCoFunction jcoFunction = destination.getRepository().getFunction(function);
            if (jcoFunction == null)
                throw new SAPException(Messages.getString("SAPConnectionPool.NotFoundFun", function)); //$NON-NLS-1$

            if (readRecord != null) {
                // because when string, it's utf8 object, so reuse the util to convert to map directly.
                final Map<String, Object> readMap = AvroTypeUtil.convertAvroRecordToMap(readRecord, AvroTypeUtil.createSchema(readRecord.getSchema()));

                JCoRecordUtil.setParams(jcoFunction.getImportParameterList(), readMap);
                JCoRecordUtil.setParams(jcoFunction.getTableParameterList(), readMap);
            }

            try {
                // start
                // JCoContext.begin(destination);

                // execute
                jcoFunction.execute(destination);
            } finally {
                // try {
                // JCoContext.end(destination);
                // } catch (JCoException e) {
                // throw new SAPException(e);
                // }
            }

            final Map<String, Object> exportResults = new LinkedHashMap<>();

            // get export
            exportResults.putAll(JCoRecordUtil.convertToMap(jcoFunction.getExportParameterList(), ignoreEmptyValues));

            // get export table
            exportResults.putAll(JCoRecordUtil.convertTablesToMap(jcoFunction, ignoreEmptyValues, exportTables));

            return exportResults;
        } catch (JCoException e) {
            throw new SAPException(e);
        }

    }

}
