package com.baishancloud.orchsym.processors.soap.service.get;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.axis2.AxisFault;
import org.apache.axis2.context.ConfigurationContext;
import org.apache.axis2.description.AxisService;
import org.apache.axis2.description.Parameter;
import org.apache.axis2.engine.AxisConfiguration;
import org.apache.axis2.transport.http.HTTPTransportReceiver;
import org.apache.axis2.transport.http.ListingAgent;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpStatus;
import org.apache.nifi.processors.standard.util.HTTPUtils;
import org.eclipse.jetty.http.MimeTypes;

import com.baishancloud.orchsym.processors.soap.model.ESOAPParam;
import com.baishancloud.orchsym.processors.soap.service.AbstractHandleService;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;
import com.baishancloud.orchsym.processors.soap.util.FormatUtil;

/**
 * @author GU Guoqiang
 *
 *         support the GET <base path>/<param> or <base path>?<param>[=[name]] ,the param related to {@link ESOAPParam}.
 * 
 *         if set the name will print the original user setting contents, else will replace the host.
 * 
 *         for example, .../orchsym/soap/wsdl, .../orchsym/soap?wsdl
 * 
 *         Addition: .../orchsym/soap/contents, will return the original wsdl contents
 */
public class HandleGetService extends AbstractHandleService {
    static final String MODIFY_PORT_ADDRESS = "modifyUserWSDLPortAddress";
    static final String SET_ENDPOINTS = "setEndpointsToAllUsedBindings";

    public HandleGetService(WSDLDoc wsdlDoc) {
        super(wsdlDoc);
    }

    @Override
    protected String[] supportMethods() {
        return new String[] { HTTPUtils.METHOD_GET };
    }

    /**
     * @see ListingAgent and HTTPWorker
     */
    @Override
    protected boolean handleMethod(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        // ?param
        ESOAPParam soapParam = ESOAPParam.matchURI(uri);
        if (soapParam == null) {
            soapParam = ESOAPParam.matchQuery(query);
        }
        // xxx/param
        final String serviceName = AbstractHandleService.getServiceName(uri);
        final ESOAPParam pathParam = ESOAPParam.matchQuery(serviceName);

        ParamGetService soapGetService = null;
        if (ESOAPParam.WSDL.equals(soapParam) || ESOAPParam.WSDL.equals(pathParam)) {
            soapGetService = new ParamGetService(request, response, ESOAPParam.WSDL) {

                @Override
                void print(AxisService service, String host, String wsdlName) throws AxisFault, IOException {
                    super.print(service, host, wsdlName);
                    setModifyPortAddressParam(service, false);
                    setEndpoints(service, false);

                    final ServletOutputStream outputStream = response.getOutputStream();

                    if (StringUtils.isNotBlank(wsdlName)) { // ?wsdl=xxx
                        service.printUserWSDL(outputStream, wsdlName, host);

                    } else if (query != null && query.contains(ESOAPParam.EQ)) { // ?wsdl=
                        service.printUserWSDL(outputStream, null, host);

                    } else { // ?wsdl
                        setModifyPortAddressParam(service, true);
                        setEndpoints(service, true);
                        service.printWSDL(outputStream, host);
                    }
                    setModifyPortAddressParam(service, false);
                    setEndpoints(service, false);
                }

            };
        } else if (ESOAPParam.WSDL2.equals(soapParam) || ESOAPParam.WSDL2.equals(pathParam)) {
            soapGetService = new ParamGetService(request, response, ESOAPParam.WSDL2) {

                @Override
                void print(AxisService service, String host, String wsdlName) throws AxisFault, IOException {
                    super.print(service, host, wsdlName);
                    setModifyPortAddressParam(service, false);

                    final ServletOutputStream outputStream = response.getOutputStream();

                    if (StringUtils.isNotBlank(wsdlName)) { // ?wsdl2=xxx
                        service.printUserWSDL2(outputStream, wsdlName, host);

                    } else if (query != null && query.contains(ESOAPParam.EQ)) { // ?wsdl2=
                        service.printUserWSDL2(outputStream, null, host);

                    } else {// wsdl2
                        setModifyPortAddressParam(service, true);
                        service.printWSDL2(outputStream, host);
                    }
                    setModifyPortAddressParam(service, false);
                }

            };
        } else if (ESOAPParam.XSD.equals(soapParam) || ESOAPParam.XSD.equals(pathParam)) {
            soapGetService = new ParamGetService(request, response, ESOAPParam.XSD) {

                @Override
                void print(AxisService service, String host, String wsdlName) throws AxisFault, IOException {
                    super.print(service, host, wsdlName);
                    final ServletOutputStream outputStream = response.getOutputStream();
                    service.printSchema(outputStream);

                    // same ?
                    // int ret = service.printXSD(response.getOutputStream(), wsdlName);
                    // if (ret == 0) {
                    // response.sendRedirect("");
                    // } else if (ret == -1) {
                    // response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    // }
                }

            };
        }

        if (soapGetService != null) {
            final boolean get = soapGetService.doGet(wsdlDoc);
            if (!get) {
                response.sendError(HttpServletResponse.SC_NOT_FOUND, request.getRequestURL().toString());
            }
            return true;
        }

        // xxx/contents
        
        if (serviceName.equals(ESOAPParam.PATH_CONTENTS)) { // output original wsdl contents
            response.setStatus(HttpStatus.SC_OK);
            response.setContentType(MimeTypes.Type.TEXT_XML.asString());

            try {
                FormatUtil.formatXML(wsdlDoc.getDocument(), response.getOutputStream());
                return true;
            } catch (Exception e) {
                throw new IOException(e);
            }
        }

        // no query
        if (request.getRequestURI().indexOf('?') == -1 && StringUtils.isBlank(query)) {
            // return usage? HTTPTransportReceiver
            ConfigurationContext context = new ConfigurationContext(new AxisConfiguration());
            wsdlDoc.getServices().forEach(s -> {
                try {
                    context.getAxisConfiguration().addService(s);
                } catch (AxisFault e) {
                    //
                }
            });
            final String html = HTTPTransportReceiver.getServicesHTML(context);
            IOUtils.write(html, response.getOutputStream());
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, request.getRequestURL().toString());
        }
        response.flushBuffer();

        return true; // all GET.
    }

    private void setModifyPortAddressParam(AxisService service, boolean flag) throws AxisFault {
        setServiceParameter(service, MODIFY_PORT_ADDRESS, flag);
    }

    private void setEndpoints(AxisService service, boolean flag) throws AxisFault {
        setServiceParameter(service, SET_ENDPOINTS, flag);
    }

    private void setServiceParameter(AxisService service, String name, boolean flag) throws AxisFault {
        final Parameter parameter = service.getParameter(name);
        if (parameter != null) {
            service.removeParameter(parameter);
        }
        service.addParameter(name, Boolean.toString(flag));
    }
}
