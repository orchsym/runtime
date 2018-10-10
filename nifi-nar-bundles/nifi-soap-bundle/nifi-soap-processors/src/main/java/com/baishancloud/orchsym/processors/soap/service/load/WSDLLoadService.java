package com.baishancloud.orchsym.processors.soap.service.load;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import javax.wsdl.Definition;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.axis2.AxisFault;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.util.XMLUtils;
import org.apache.axis2.wsdl.WSDLConstants;
import org.apache.commons.lang3.StringUtils;
import org.apache.woden.WSDLSource;
import org.apache.woden.wsdl20.Description;
import org.w3c.dom.Document;
import org.xml.sax.SAXException;

import com.baishancloud.orchsym.processors.soap.model.ESOAPParam;
import com.baishancloud.orchsym.processors.soap.model.ProxyDescDefinition;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;

/**
 * @author GU Guoqiang
 *
 */
public class WSDLLoadService {

    public WSDLDoc populateURIDoc(String wsdlURI) throws Exception {
        return populateAllServices(loadURI(wsdlURI));
    }

    WSDLDoc loadURI(String wsdlURI) throws Exception {
        if (StringUtils.isBlank(wsdlURI)) {
            return null;
        }
        try {
            URL url = com.ibm.wsdl.util.StringUtils.getURL(null, wsdlURI);
            // if http without ?wsdl
            final String protocol = url.getProtocol();
            if ((protocol.equals(org.apache.axis2.Constants.TRANSPORT_HTTP) || protocol.equals(org.apache.axis2.Constants.TRANSPORT_HTTPS)) //
                    && !wsdlURI.endsWith(ESOAPParam.WSDL.getQueryParam())) {
                wsdlURI += ESOAPParam.WSDL.getQueryParam();
            }
        } catch (MalformedURLException e) {
            // will check again in readWSDL method.
        }
        return loadDoc(wsdlURI, null);
    }

    public WSDLDoc populateContentDoc(String contents) throws Exception {
        return populateAllServices(loadContent(contents));
    }

    WSDLDoc loadContent(String contents) throws Exception {
        if (StringUtils.isBlank(contents)) {
            return null;
        }
        Document wsdlDocument = XMLUtils.newDocument(new ByteArrayInputStream(contents.getBytes(StandardCharsets.UTF_8)));
        return loadDoc(null, wsdlDocument);
    }

    WSDLDoc populateAllServices(WSDLDoc wsdlDoc) throws AxisFault {
        final Definition definition = wsdlDoc.getDefinition();

        List<AxisService> services = null;
        if (wsdlDoc.isWSDL20() && definition instanceof ProxyDescDefinition) {
            final Description wsdlDescription = ((ProxyDescDefinition) definition).getWsdlDesciption();
            DescWSDL20ToAllAxisServicesBuilder builder = new DescWSDL20ToAllAxisServicesBuilder(wsdlDescription);
            services = builder.populateAllServices();

        } else if (wsdlDoc.isWSDL11()) {
            DefWSDL11ToAllAxisServicesBuilder builder = new DefWSDL11ToAllAxisServicesBuilder(definition);
            services = builder.populateAllServices();
        }
        if (services != null) {
            wsdlDoc.setServices(services);
            return wsdlDoc;
        }
        return null;
    }

    WSDLDoc loadDoc(String wsdlURI, Document wsdlDocument) throws Exception {
        if (wsdlURI != null && wsdlDocument == null) {
            try {
                wsdlDocument = XMLUtils.newDocument(wsdlURI);
            } catch (ParserConfigurationException | SAXException | IOException e) {
                throw new javax.wsdl.WSDLException(javax.wsdl.WSDLException.INVALID_WSDL, e.getMessage(), e);
            }
        }
        if (wsdlDocument == null) {
            return null;
        }

        final String namespaceURI = wsdlDocument.getDocumentElement().getNamespaceURI();
        if (WSDLConstants.WSDL1_1_NAMESPACE.equals(namespaceURI)) {
            final Definition wsdl11Def = load11(wsdlURI, wsdlDocument);
            if (wsdl11Def != null)
                return new WSDLDoc(wsdlDocument, wsdl11Def);
        } else if (WSDLConstants.WSDL2_0_NAMESPACE.equals(namespaceURI)) {
            final Description wsdl20 = load20(wsdlURI, wsdlDocument);
            if (wsdl20 != null)
                return new WSDLDoc(wsdlDocument, new ProxyDescDefinition(wsdl20));

        }
        return null;
    }

    private Definition load11(String wsdlURI, Document wsdlDocument) throws javax.wsdl.WSDLException {
        final javax.wsdl.xml.WSDLReader wsdlReader = org.apache.axis2.wsdl.WSDLUtil.newWSDLReaderWithPopulatedExtensionRegistry();
        wsdlReader.setFeature(com.ibm.wsdl.Constants.FEATURE_VERBOSE, false);
        wsdlReader.setFeature(com.ibm.wsdl.Constants.FEATURE_IMPORT_DOCUMENTS, true);

        Definition wsdlDef = null;
        if (wsdlURI != null) {
            wsdlDef = wsdlReader.readWSDL(wsdlURI);
        } else if (wsdlDocument != null) {
            wsdlDef = wsdlReader.readWSDL(null, wsdlDocument);
        }
        return wsdlDef;
    }

    private Description load20(String wsdlURI, Document wsdlDocument) throws org.apache.woden.WSDLException {
        org.apache.woden.WSDLReader wsdlReader = org.apache.woden.WSDLFactory.newInstance().newWSDLReader();
        wsdlReader.setFeature(org.apache.woden.WSDLReader.FEATURE_VERBOSE, false);
        wsdlReader.setFeature(org.apache.woden.WSDLReader.FEATURE_VALIDATION, true);
        wsdlReader.setFeature(org.apache.woden.WSDLReader.FEATURE_CONTINUE_ON_ERROR, false);

        Description wsdlDes = null;
        if (wsdlURI != null) {
            wsdlDes = wsdlReader.readWSDL(wsdlURI);
        } else if (wsdlDocument != null) {
            final WSDLSource wsdlSource = wsdlReader.createWSDLSource();
            wsdlSource.setBaseURI(null);
            wsdlSource.setSource(wsdlDocument);

            wsdlDes = wsdlReader.readWSDL(wsdlSource);
        }
        return wsdlDes;
    }

}
