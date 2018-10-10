package com.baishancloud.orchsym.processors.soap.service;

import java.io.IOException;
import java.util.Arrays;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.transport.http.HTTPConstants;

/**
 * @author GU Guoqiang
 *
 */
public abstract class AbstractHandleService {
    protected static final int BUFFER_SIZE = 1024 * 8;

    protected final WSDLDoc wsdlDoc;

    protected volatile String uri, query, soapAction;

    public AbstractHandleService(WSDLDoc wsdlDoc) {
        super();
        this.wsdlDoc = wsdlDoc;
    }

    public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        if (!wsdlDoc.valid()) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
            return true;
        }
        // response.setBufferSize(BUFFER_SIZE);

        uri = request.getRequestURI();
        query = request.getQueryString();
        soapAction = request.getHeader(HTTPConstants.HEADER_SOAP_ACTION);

        // ListingAgent agent=new ListingAgent(null);
        // HTTPWorker httpWorker = new HTTPWorker();
        // httpWorker.service(request, response, null)

        final String method = request.getMethod().toUpperCase();
        if (Arrays.asList(supportMethods()).contains(method)) {
            return handleMethod(request, response);
        } else {
            // ignore others
            return false;
        }
    }

    protected abstract String[] supportMethods();

    protected abstract boolean handleMethod(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException;

    protected String getServicename() {
        return getServiceName(uri);
    }

    public static String getServiceName(String uri) {
        if (uri != null && uri.contains("/")) {
            String serviceName = uri.substring(uri.lastIndexOf("/") + 1);
            final int paramIndex = serviceName.indexOf('?');
            if (paramIndex > 0) {
                serviceName = serviceName.substring(0, paramIndex); // remove ?xxx
            }
            return serviceName;
        }
        return uri;
    }
}
