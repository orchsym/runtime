package com.baishancloud.orchsym.processors.soap.service.load;

import org.apache.axis2.AxisFault;
import org.apache.axis2.dataretrieval.WSDL11SupplierTemplate;
import org.apache.axis2.description.AxisService;

/**
 * @author GU Guoqiang
 *
 */
public class DefWSDL11SupplierTemplate extends WSDL11SupplierTemplate {

    @Override
    public Object getWSDL(AxisService service) throws AxisFault {
        // because no axisconfig, so can't
        // Parameter wsdlParameter = service.getParameter(WSDLConstants.WSDL_4_J_DEFINITION);
        // if (wsdlParameter != null) {
        // Definition definition = (Definition) wsdlParameter.getValue();
        // if (definition != null) {
        // return definition;
        // }
        // }
        return super.getWSDL(service);
    }

}
