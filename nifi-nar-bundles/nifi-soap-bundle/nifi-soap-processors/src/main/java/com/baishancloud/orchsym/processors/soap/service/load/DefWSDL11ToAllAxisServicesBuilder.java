package com.baishancloud.orchsym.processors.soap.service.load;

import javax.wsdl.Definition;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL11ToAllAxisServicesBuilder;

/**
 * @author GU Guoqiang
 *
 */
public class DefWSDL11ToAllAxisServicesBuilder extends WSDL11ToAllAxisServicesBuilder {

    public DefWSDL11ToAllAxisServicesBuilder(Definition def) {
        super(def);
    }

    @Override
    public AxisService populateService() throws AxisFault {
        final AxisService service = super.populateService();
        // try generate, can disable
        service.addParameter(Constants.WSDL_11_SUPPLIER_CLASS_PARAM, DefWSDL11SupplierTemplate.class.getName());

        // will set dynamic
        // service.addParameter(MODIFY_PORT_ADDRESS, Boolean.TRUE.toString());

        // have set one wrapper
        // service.addParameter(WSDLConstants.WSDL_4_J_DEFINITION, wsdl4jDefinition);
        return service;
    }

}
