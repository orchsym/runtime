package com.baishancloud.orchsym.processors.soap.model;

import org.apache.axis2.transport.http.server.HttpUtils;
import org.apache.commons.lang3.StringUtils;

/**
 * @author GU Guoqiang
 *
 */
public enum ESOAPParam {
    WSDL2, WSDL, XSD,;
    
    public static final String EQ = "="; //$NON-NLS-1$
    public static final String PATH_CONTENTS = "contents"; //$NON-NLS-1$
    
    public String getQueryParam() {
        return '?' + getName();
    }

    public String getName() {
        return this.name().toLowerCase();
    }

    public static ESOAPParam matchURI(String uri) {
        if (StringUtils.isNoneBlank(uri))
            for (ESOAPParam p : ESOAPParam.values())
                if (HttpUtils.endsWithIgnoreCase(uri, p.getQueryParam()))
                    return p;

        return null;
    }

    public static ESOAPParam matchQuery(String query) {
        if (StringUtils.isNoneBlank(query)) {
            int index = HttpUtils.indexOfIngnoreCase(query, EQ); // with param
            if (index > 0) {
                query = query.substring(0, index);
            }
            for (ESOAPParam p : ESOAPParam.values())
                if (query.equals(p.getName()))
                    return p;
        }
        return null;
    }
}
