package com.baishancloud.orchsym.processors.soap.service.post;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.nifi.processors.standard.util.HTTPUtils;

import com.baishancloud.orchsym.processors.soap.service.AbstractHandleService;
import com.baishancloud.orchsym.processors.soap.service.WSDLDoc;

/**
 * @author GU Guoqiang
 *
 */
public class HandlePostService extends AbstractHandleService {

    public HandlePostService(WSDLDoc wsdlDoc) {
        super(wsdlDoc);
    }

    @Override
    protected String[] supportMethods() {
        return new String[] { HTTPUtils.METHOD_POST };
    }

    @Override
    protected boolean handleMethod(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        return false;
    }

}
