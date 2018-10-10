/**
 * 
 */
package com.baishancloud.orchsym.processors.soap.service.get;

import java.io.IOException;
import java.util.Enumeration;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.util.JavaUtils;
import org.apache.http.HttpStatus;
import org.eclipse.jetty.http.MimeTypes;

import com.baishancloud.orchsym.processors.soap.model.ESOAPParam;
import com.baishancloud.orchsym.processors.soap.service.AbstractHandleService;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;

/**
 * @author GU Guoqiang
 *
 */
class ParamGetService {
    protected final HttpServletRequest request;
    protected final HttpServletResponse response;
    protected final ESOAPParam param;

    ParamGetService(HttpServletRequest request, HttpServletResponse response, ESOAPParam param) {
        super();
        this.request = request;
        this.response = response;
        this.param = param;
    }

    boolean doGet(WSDLDoc wsdlDoc) throws AxisFault, IOException {
        if (!wsdlDoc.valid()) {
            return false;
        }

        response.setStatus(HttpStatus.SC_OK);
        response.setContentType(MimeTypes.Type.TEXT_XML.asString());

        final String host = extractHost(request.getRequestURL().toString());
        final String wsdlName = getParamtereIgnoreCase(request, param.getName());

        final Map<String, AxisService> axisServicesMap = wsdlDoc.getServices().stream().collect(Collectors.toMap(AxisService::getName, Function.identity()));

        final String serviceName = AbstractHandleService.getServiceName(request.getRequestURI());
        AxisService service = (AxisService) axisServicesMap.get(serviceName);
        if (service != null) {
            print(service, host, wsdlName);
        } else {// all
            for (AxisService s : wsdlDoc.getServices()) {
                print(s, host, wsdlName);
            }
        }
        response.flushBuffer();

        return true;

    }

    private String extractHost(String filePart) {
        int ipindex = filePart.indexOf("//");
        String ip = null;
        if (ipindex >= 0) {
            ip = filePart.substring(ipindex + 2, filePart.length());
            int seperatorIndex = ip.indexOf(":");
            int slashIndex = ip.indexOf("/");
            if (seperatorIndex >= 0) {
                ip = ip.substring(0, seperatorIndex);
            } else {
                ip = ip.substring(0, slashIndex);
            }
        }
        return ip;
    }

    @SuppressWarnings("rawtypes")
    private String getParamtereIgnoreCase(HttpServletRequest req, String paraName) {
        Enumeration e = req.getParameterNames();
        while (e.hasMoreElements()) {
            String name = (String) e.nextElement();
            if (name.equalsIgnoreCase(paraName)) {
                String value = req.getParameter(name);
                return value;
            }

        }
        return null;
    }

    void print(AxisService service, String host, String wsdlName) throws AxisFault, IOException {
        canExposeServiceMetadata(service);

    }

    private boolean canExposeServiceMetadata(AxisService service) throws IOException {
        Parameter exposeServiceMetadata = service.getParameter("exposeServiceMetadata");
        if (exposeServiceMetadata != null && JavaUtils.isFalseExplicitly(exposeServiceMetadata.getValue())) {
            return false;
        }
        return true;
    }
}
