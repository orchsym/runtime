package com.baishancloud.orchsym.processors.soap;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.TimeUnit;

import javax.servlet.AsyncContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.io.FileUtils;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.http.HttpContextMap;
import org.apache.nifi.processors.standard.util.HTTPUtils;
import org.apache.nifi.reporting.InitializationException;
import org.apache.nifi.stream.io.NullOutputStream;
import org.apache.nifi.stream.io.StreamUtils;
import org.apache.nifi.util.MockFlowFile;
import org.apache.nifi.util.TestRunner;
import org.apache.nifi.util.TestRunners;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import com.baishancloud.orchsym.processors.soap.model.ESOAPParam;
import com.baishancloud.orchsym.processors.soap.model.EWSDLOptions;
import com.baishancloud.orchsym.processors.soap.service.load.WSDLLoadServiceTest;

/**
 * @author GU Guoqiang
 *
 *         FIXME, will fix the junit later
 */
@Ignore
public class TestHandleSOAPHttpRequest4Get {
    static final String BASE_PATH = "/my/test";
    static String wsdlContents;

    @BeforeClass
    public static void init() throws IOException {
        wsdlContents = FileUtils.readFileToString(WSDLLoadServiceTest.weatherWSDLFile);
    }

    /*
     * ?wsdl
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl(null);
    }

    /*
     * ?wsdl=
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl("");
    }

    /*
     * ?wsdl=xxx
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl_unknown() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl("xxx");
    }

    /*
     * ?wsdl=<name>
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl_param() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl("WeatherWebService");
    }

    private void test_contents_query_wsdl(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query(ESOAPParam.WSDL, paramValue);
    }

    /*
     * ?wsdl2
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl2() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl2(null);
    }

    /*
     * ?wsdl2=
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl2_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl2("");
    }

    /*
     * ?wsdl2=xxx
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl2_unknown() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl2("xxx");
    }

    /*
     * ?wsdl2=<name>
     */
    @Test(timeout = 10000)
    public void test_contents_query_wsdl2_param() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_wsdl2("WeatherWebService");
    }

    private void test_contents_query_wsdl2(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query(ESOAPParam.WSDL2, paramValue);
    }

    /*
     * ?xsd
     */
    @Test(timeout = 10000)
    public void test_contents_query_xsd() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_xsd(null);
    }

    /*
     * ?xsd=
     */
    @Test(timeout = 10000)
    public void test_contents_query_xsd_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query_xsd("");
    }

    private void test_contents_query_xsd(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents_query(ESOAPParam.XSD, paramValue);
    }

    private void test_contents_query(ESOAPParam param, String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents("?" + param.getName() + (paramValue != null ? ESOAPParam.EQ + paramValue : ""));
    }

    /*
     * wsdl
     */
    @Test(timeout = 10000)
    public void test_contents_path_wsdl() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents("/" + ESOAPParam.WSDL.getName());
    }

    /*
     * wsdl2
     */
    @Test(timeout = 10000)
    public void test_contents_path_wsdl2() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents("/" + ESOAPParam.WSDL2.getName());
    }

    /*
     * xsd
     */
    @Test(timeout = 10000)
    public void test_contents_path_xsd() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents("/" + ESOAPParam.XSD.getName());
    }

    /*
     * contents
     */
    @Test(timeout = 10000)
    public void test_contents_path_contents() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_contents("/" + ESOAPParam.PATH_CONTENTS);
    }

    private void test_contents(String path) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        final TestRunner runner = TestRunners.newTestRunner(HandleSOAPHttpRequest.class);
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_OPTIONS, EWSDLOptions.CONTENTS.getValue());
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_CONTENTS, wsdlContents);
        doTest(runner, path);
    }

    /*
     * ?wsdl
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl(null);
    }

    /*
     * ?wsdl=
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl("");
    }

    /*
     * ?wsdl=xxx
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl_unknown() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl("xxx");
    }

    /*
     * ?wsdl=<name>
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl_param() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl("WeatherWebService");
    }

    private void test_uri_query_wsdl(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query(ESOAPParam.WSDL, paramValue);
    }

    /*
     * ?wsdl2
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl2() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl2(null);
    }

    /*
     * ?wsdl2=
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl2_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl2("");
    }

    /*
     * ?wsdl2=xxx
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl2_unknown() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl2("xxx");
    }

    /*
     * ?wsdl2=<name>
     */
    @Test(timeout = 10000)
    public void test_uri_query_wsdl2_param() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_wsdl2("WeatherWebService");
    }

    private void test_uri_query_wsdl2(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query(ESOAPParam.WSDL2, paramValue);
    }

    /*
     * ?xsd
     */
    @Test(timeout = 10000)
    public void test_uri_query_xsd() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_xsd(null);
    }

    /*
     * ?xsd=
     */
    @Test(timeout = 10000)
    public void test_uri_query_xsd_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query_xsd("");
    }

    private void test_uri_query_xsd(String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri_query(ESOAPParam.XSD, paramValue);
    }

    private void test_uri_query(ESOAPParam param, String paramValue) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri("?" + param.getName() + (paramValue != null ? ESOAPParam.EQ + paramValue : ""));
    }

    /*
     * wsdl
     */
    @Test(timeout = 10000)
    public void test_uri_path_wsdl() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri("/" + ESOAPParam.WSDL.getName());
    }

    /*
     * wsdl2
     */
    @Test(timeout = 10000)
    public void test_uri_path_wsdl2() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri("/" + ESOAPParam.WSDL2.getName());
    }

    /*
     * xsd
     */
    @Test(timeout = 10000)
    public void test_uri_path_xsd() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri("/" + ESOAPParam.XSD.getName());
    }

    /*
     * contents
     */
    @Test(timeout = 10000)
    public void test_uri_path_contents() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        test_uri("/" + ESOAPParam.PATH_CONTENTS);
    }

    private void test_uri(String path) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        final TestRunner runner = TestRunners.newTestRunner(HandleSOAPHttpRequest.class);
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_OPTIONS, EWSDLOptions.URI.getValue());
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_URI, "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx?wsdl");
        doTest(runner, path);
    }

    /*
     * contents
     */
    @Test(timeout = 10000)
    public void test_uri_path_no_query() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        final TestRunner runner = TestRunners.newTestRunner(HandleSOAPHttpRequest.class);
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_OPTIONS, EWSDLOptions.URI.getValue());
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_URI, "http://www.webxml.com.cn/WebServices/WeatherWebService.asmx");
        doTest(runner, "/" + ESOAPParam.PATH_CONTENTS);
    }

    /*
     * only basic path
     */
    @Test(timeout = 10000)
    public void test_uri_empty() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        final TestRunner runner = TestRunners.newTestRunner(HandleSOAPHttpRequest.class);
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_OPTIONS, EWSDLOptions.CONTENTS.getValue());
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_CONTENTS, wsdlContents);
        doTest(runner, "");
    }

    /*
     * abcd
     */
    @Test(timeout = 10000)
    public void test_uri_any_path() throws InitializationException, MalformedURLException, IOException, InterruptedException {
        final TestRunner runner = TestRunners.newTestRunner(HandleSOAPHttpRequest.class);
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_OPTIONS, EWSDLOptions.CONTENTS.getValue());
        runner.setProperty(HandleSOAPHttpRequest.SOAP_WSDL_CONTENTS, wsdlContents);
        doTest(runner, "abcd");
    }

    private void doTest(final TestRunner runner, String path) throws InitializationException, MalformedURLException, IOException, InterruptedException {
        runner.setProperty(HandleSOAPHttpRequest.SOAP_PORT, "0");
        runner.setProperty(HandleSOAPHttpRequest.SOAP_PATH_REGEX, BASE_PATH);

        final MockHttpContextMap contextMap = new MockHttpContextMap();
        runner.addControllerService("http-context-map", contextMap);
        runner.enableControllerService(contextMap);
        runner.setProperty(HandleSOAPHttpRequest.HTTP_CONTEXT_MAP, "http-context-map");

        // trigger processor to stop but not shutdown.
        runner.run(1, false);
        try {
            final Thread httpThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        final int port = ((HandleSOAPHttpRequest) runner.getProcessor()).getPort();
                        final HttpURLConnection connection = (HttpURLConnection) new URL("http://localhost:" + port + BASE_PATH + path).openConnection();
                        connection.setDoOutput(false);
                        connection.setRequestMethod(HTTPUtils.METHOD_GET);
                        connection.setRequestProperty("header1", "value1");
                        connection.setRequestProperty("header2", "");
                        connection.setRequestProperty("header3", "apple=orange");
                        connection.setConnectTimeout(3000);
                        connection.setReadTimeout(3000);

                        StreamUtils.copy(connection.getInputStream(), new NullOutputStream());
                    } catch (final Throwable t) {
                        t.printStackTrace();
                        Assert.fail(t.toString());
                    }
                }
            });
            httpThread.start();

            while (runner.getFlowFilesForRelationship(HandleSOAPHttpRequest.REL_SUCCESS).isEmpty()) {
                // process the request.
                runner.run(1, false, false);
            }

            runner.assertAllFlowFilesTransferred(HandleSOAPHttpRequest.REL_SUCCESS, 1);
            assertEquals(1, contextMap.size());

            final MockFlowFile mff = runner.getFlowFilesForRelationship(HandleSOAPHttpRequest.REL_SUCCESS).get(0);
            // if (paramValue != null) {
            // mff.assertAttributeEquals("http.query.param." + param.getName(), paramValue);
            // }
            mff.assertAttributeEquals("http.headers.header1", "value1");
            mff.assertAttributeEquals("http.headers.header3", "apple=orange");
        } finally {
            // shut down the server
            runner.run(1, true);
        }
    }

    private static class MockHttpContextMap extends AbstractControllerService implements HttpContextMap {

        private boolean registerSuccessfully = true;

        private final ConcurrentMap<String, HttpServletResponse> responseMap = new ConcurrentHashMap<>();

        @Override
        public boolean register(final String identifier, final HttpServletRequest request, final HttpServletResponse response, final AsyncContext context) {
            return register(identifier, request, response, context, null);
        }

        @Override
        public boolean register(String identifier, HttpServletRequest request, HttpServletResponse response, AsyncContext context, Map<String, Object> additions) {
            if (registerSuccessfully) {
                responseMap.put(identifier, response);
            }
            return registerSuccessfully;
        }

        @Override
        public HttpServletResponse getResponse(final String identifier) {
            return responseMap.get(identifier);
        }

        @Override
        public Map<String, Object> getAdditions(String identifier) {
            return null;
        }

        @Override
        public void complete(final String identifier) {
            responseMap.remove(identifier);
        }

        public int size() {
            return responseMap.size();
        }

        public boolean isRegisterSuccessfully() {
            return registerSuccessfully;
        }

        public void setRegisterSuccessfully(boolean registerSuccessfully) {
            this.registerSuccessfully = registerSuccessfully;
        }

        @Override
        public long getRequestTimeout(TimeUnit timeUnit) {
            return timeUnit.convert(30000, TimeUnit.MILLISECONDS);
        }
    }

}
