package com.baishancloud.orchsym.processors.soap.service.load;

import org.apache.axis2.AxisFault;
import org.apache.axis2.Constants;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.WSDL20ToAllAxisServicesBuilder;
import org.apache.woden.wsdl20.Description;

/**
 * @author GU Guoqiang
 *
 */
public class DescWSDL20ToAllAxisServicesBuilder extends WSDL20ToAllAxisServicesBuilder {

    public DescWSDL20ToAllAxisServicesBuilder(Description description) {
        super(null);
        this.description = description;
    }

    @Override
    public AxisService populateService() throws AxisFault {
        final AxisService service = super.populateService();
        // try generate, can disable
        service.addParameter(Constants.WSDL_20_SUPPLIER_CLASS_PARAM, DescWSDL20SupplierTemplate.class.getName());

        // will set dynamic
        // service.addParameter(MODIFY_PORT_ADDRESS, Boolean.TRUE.toString());

        // have set
        // service.addParameter(WSDLConstants.WSDL_20_DESCRIPTION, description);
        return service;
    }

}
