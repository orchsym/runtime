package org.apache.nifi.web.security.oidc.adfs;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;

import org.apache.nifi.util.HttpRequestUtil;

import com.nimbusds.oauth2.sdk.http.HTTPRequest;

public class OrchsymHTTPRequest extends HTTPRequest{
    
    public OrchsymHTTPRequest(Method method, URL url) {
        super(method, url);
    }
    
    public HttpURLConnection toHttpURLConnection()
            throws IOException {
        
        URL finalURL = super.getURL();
        
        if (super.getQuery() != null && (super.getMethod().equals(HTTPRequest.Method.GET) || super.getMethod().equals(Method.DELETE))) {
            
            // Append query string
            StringBuilder sb = new StringBuilder(super.getURL().toString());
            sb.append('?');
            sb.append(super.getQuery());
            
            try {
                finalURL = new URL(sb.toString());
                
            } catch (MalformedURLException e) {
                
                throw new IOException("Couldn't append query string: " + e.getMessage(), e);
            }
        }
        
        if (super.getFragment() != null) {
            
            // Append raw fragment
            StringBuilder sb = new StringBuilder(finalURL.toString());
            sb.append('#');
            sb.append(super.getFragment());
            
            try {
                finalURL = new URL(sb.toString());
                
            } catch (MalformedURLException e) {
                
                throw new IOException("Couldn't append raw fragment: " + e.getMessage(), e);
            }
        }
        try {
            if("https".equalsIgnoreCase(finalURL.getProtocol())){
                HttpRequestUtil.trustAll();
            }
        } catch (Exception e) {
           //ignore
        }
        HttpURLConnection conn = (HttpURLConnection)finalURL.openConnection();
        
        for (Map.Entry<String,String> header: getHeaders().entrySet()) {
            conn.setRequestProperty(header.getKey(), header.getValue());
        }
        
        conn.setRequestMethod(super.getMethod().name());
        conn.setConnectTimeout(super.getConnectTimeout());
        conn.setReadTimeout(super.getReadTimeout());
        conn.setInstanceFollowRedirects(super.getFollowRedirects());
        
        if (super.getMethod().equals(HTTPRequest.Method.POST) || super.getMethod().equals(Method.PUT)) {
            conn.setDoOutput(true);
            
            if (getContentType() != null)
                conn.setRequestProperty("Content-Type", getContentType().toString());
            
            if (super.getQuery() != null) {
                try {
                    OutputStreamWriter writer = new OutputStreamWriter(conn.getOutputStream());
                    writer.write(super.getQuery());
                    writer.close();
                } catch (IOException e) {
                    closeStreams(conn);
                    throw e; // Rethrow
                }
            }
        }
        
        return conn;
    }
    
    private void closeStreams(final HttpURLConnection conn) {
        
        if (conn == null) {
            return;
        }
        
        try {
            if (conn.getInputStream() != null) {
                conn.getInputStream().close();
            }
        } catch (Exception e) {
            // ignore
        }
        
        try {
            if (conn.getOutputStream() != null) {
                conn.getOutputStream().close();
            }
        } catch (Exception e) {
            // ignore
        }
        
        try {
            if (conn.getErrorStream() != null) {
                conn.getOutputStream().close();
            }
        } catch (Exception e) {
            // ignore
        }
    }
}