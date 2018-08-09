package com.baishancloud.orchsym.sap.metadata.param;

import com.sap.conn.jco.JCoListMetaData;

/**
 * @author GU Guoqiang
 *
 */
public enum ESAPParamType {
    IMPORT(JCoListMetaData.IMPORT_PARAMETER), // $NON-NLS-1$
    EXPORT(JCoListMetaData.EXPORT_PARAMETER), // $NON-NLS-1$
    TABLE(JCoListMetaData.OPTIONAL_PARAMETER), // $NON-NLS-1$
    CHANGING(JCoListMetaData.CHANGING_PARAMETER),// $NON-NLS-1$
    ;

    private int jcoType;

    private ESAPParamType(int jcoType) {
        this.jcoType = jcoType;
    }

    public int getJcoType() {
        return jcoType;
    }

    public String getMetaName() {
        return this.name();
    }

}
