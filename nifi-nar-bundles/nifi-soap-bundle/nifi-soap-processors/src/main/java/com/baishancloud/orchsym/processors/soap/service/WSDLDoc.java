/**
 * 
 */
package com.baishancloud.orchsym.processors.soap.service;

import java.util.List;

import javax.wsdl.Definition;

import org.apache.axis2.description.AxisService;
import org.apache.axis2.wsdl.WSDLConstants;
import org.w3c.dom.Document;

/**
 * @author GU Guoqiang
 *
 */
public class WSDLDoc {
    public static final String KEY="WSDL-DOC";
    
    private final Document doc;
    private final Definition def;
    private List<AxisService> services;

    public WSDLDoc(Document doc, Definition def) {
        super();
        this.doc = doc;
        this.def = def;
    }

    public Document getDocument() {
        return doc;
    }

    public Definition getDefinition() {
        return def;
    }

    public List<AxisService> getServices() {
        return services;
    }

    public void setServices(List<AxisService> services) {
        this.services = services;
    }

    public boolean valid() {
        return doc != null && def != null && services != null && !services.isEmpty();
    }

    public String getNamespaceURI() {
        return doc != null ? doc.getDocumentElement().getNamespaceURI() : null;
    }

    public boolean isWSDL11() {
        return WSDLConstants.WSDL1_1_NAMESPACE.equals(getNamespaceURI());
    }

    public boolean isWSDL20() {
        return WSDLConstants.WSDL2_0_NAMESPACE.equals(getNamespaceURI());
    }
}
