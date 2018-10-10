package com.baishancloud.orchsym.processors.soap.service.load;

import org.apache.axis2.AxisFault;
import org.apache.axis2.dataretrieval.WSDL20SupplierTemplate;
import org.apache.axis2.description.AxisService;

/**
 * @author GU Guoqiang
 *
 */
public class DescWSDL20SupplierTemplate extends WSDL20SupplierTemplate {

    @Override
    public Object getWSDL(AxisService service) throws AxisFault {
        // because no axisconfig, so can't
        // Parameter wsdlParameter = service.getParameter(WSDLConstants.WSDL_20_DESCRIPTION);
        // if (wsdlParameter != null) {
        // Description description = (Description) wsdlParameter.getValue();
        // if (description != null) {
        // return description;
        // }
        // }
        return super.getWSDL(service);
    }

}
