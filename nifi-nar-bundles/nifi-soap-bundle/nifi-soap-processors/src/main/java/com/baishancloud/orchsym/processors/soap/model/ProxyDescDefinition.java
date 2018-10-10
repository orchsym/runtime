package com.baishancloud.orchsym.processors.soap.model;

import org.apache.woden.wsdl20.Description;

import com.ibm.wsdl.DefinitionImpl;

/**
 * @author GU Guoqiang
 *
 */
public class ProxyDescDefinition extends DefinitionImpl {
    private static final long serialVersionUID = -7110275246098339017L;
    private final Description wsdlDesc;

    public ProxyDescDefinition(Description wsdlDesc) {
        this.wsdlDesc = wsdlDesc;
        init();
    }

    public Description getWsdlDesciption() {
        return wsdlDesc;
    }

    private void init() {
        wsdlDesc.getBindings();
        wsdlDesc.getServices();
        wsdlDesc.getTypeDefinitions();
    }

}
