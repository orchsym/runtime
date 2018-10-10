package com.baishancloud.orchsym.processors.soap.service.post;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.description.AxisDescription;
import org.apache.axis2.description.AxisOperation;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.nifi.processors.standard.util.HTTPUtils;

import com.baishancloud.orchsym.processors.soap.service.AbstractHandleService;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;

/**
 * @author GU Guoqiang
 *
 */
public class HandleOperationValidateService extends AbstractHandleService {

    public HandleOperationValidateService(WSDLDoc wsdlDoc) {
        super(wsdlDoc);
    }

    @Override
    protected String[] supportMethods() {
        return new String[] { HTTPUtils.METHOD_POST };
    }

    @Override
    protected boolean handleMethod(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        final List<AxisService> services = wsdlDoc.getServices();
        if (services == null || services.isEmpty()) {
            response.sendError(HttpServletResponse.SC_NO_CONTENT);
            return true;
        }
        final String serviceName = getServiceName(uri);
        final Optional<AxisService> serviceOp = services.stream().filter(s -> s.getName().equals(serviceName)).findFirst();

        if (serviceOp.isPresent()) {
            final AxisService axisService = serviceOp.get();
            AxisOperation operation = axisService.getOperationBySOAPAction(soapAction);
            if (operation == null) {
                final String action = getServiceName(soapAction);
                operation = axisService.getOperationBySOAPAction(action);
            }
            if (operation == null) {
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, uri);
                return true;
            } else {
                final AxisDescription inMessage = operation.getChild(WSDLConstants.WSDL_MESSAGE_IN_MESSAGE);

                return checkInputMessage(request, response, inMessage);

            }
        }
        return false; // don't deal with all POST, only for valid wsdl
    }

    /**
     * 
     * return true, means, have done response
     */
    protected boolean checkInputMessage(HttpServletRequest request, HttpServletResponse response, final AxisDescription inMessage) throws IOException {
        return false;
    }
}
