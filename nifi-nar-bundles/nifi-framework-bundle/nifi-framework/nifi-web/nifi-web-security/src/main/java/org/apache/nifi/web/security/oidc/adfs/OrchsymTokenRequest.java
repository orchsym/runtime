package org.apache.nifi.web.security.oidc.adfs;
import static com.nimbusds.openid.connect.sdk.claims.UserInfo.EMAIL_CLAIM_NAME;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Map;

import com.nimbusds.oauth2.sdk.AuthorizationGrant;
import com.nimbusds.oauth2.sdk.Scope;
import com.nimbusds.oauth2.sdk.SerializeException;
import com.nimbusds.oauth2.sdk.TokenRequest;
import com.nimbusds.oauth2.sdk.auth.ClientAuthentication;
import com.nimbusds.oauth2.sdk.http.CommonContentTypes;
import com.nimbusds.oauth2.sdk.http.HTTPRequest;
import com.nimbusds.oauth2.sdk.util.URLUtils;
import com.nimbusds.openid.connect.sdk.claims.IDTokenClaimsSet;

public class OrchsymTokenRequest extends TokenRequest{
    
    public OrchsymTokenRequest(URI uri, AuthorizationGrant authzGrant) {
        super(uri, authzGrant);
        // TODO Auto-generated constructor stub
    }
    
    public OrchsymTokenRequest(final URI uri,
            final ClientAuthentication clientAuth,
            final AuthorizationGrant authzGrant,
            final Scope scope) {
        super(uri, clientAuth, authzGrant, scope);
    }
    
    @Override
    public OrchsymHTTPRequest toHTTPRequest() {
        if (getEndpointURI() == null)
            throw new SerializeException("The endpoint URI is not specified");
        
        URL url;
        boolean adfs = isADFS(getEndpointURI());
        
        try {
            url = getEndpointURI().toURL();
            
        } catch (MalformedURLException e) {
            
            throw new SerializeException(e.getMessage(), e);
        }
        
        OrchsymHTTPRequest httpRequest = new OrchsymHTTPRequest(HTTPRequest.Method.POST, url);
        httpRequest.setContentType(CommonContentTypes.APPLICATION_URLENCODED);
        
        if (!adfs && getClientAuthentication() != null) {
            getClientAuthentication().applyTo(httpRequest);
        }
        
        Map<String,String> params = httpRequest.getQueryParameters();
        
        params.putAll(getAuthorizationGrant().toParameters());
        
        if (getScope() != null && ! getScope().isEmpty()) {
            params.put("scope", getScope().toString());
        }
        if (adfs && getClientAuthentication().getClientID() != null) {
            params.put("client_id", getClientAuthentication().getClientID().getValue());
        }
        if (!adfs && getClientID() != null) {
            params.put("client_id", getClientID().getValue());
        }
        if (! getCustomParameters().isEmpty()) {
            params.putAll(getCustomParameters());
        }
        
        httpRequest.setQuery(URLUtils.serializeParameters(params));
        
        return httpRequest;
    }
    
    private boolean isADFS(URI endpointURI) {
        boolean isADFS = false;
        try {
            if(getEndpointURI().getPath().toLowerCase().startsWith("/adfs/")){
                isADFS = true;
            }
        } catch (Exception e) {
        }
        return isADFS;
    }
    
    public static String getEmailKey(IDTokenClaimsSet claimsSet){
        String iss = claimsSet.getStringClaim("iss"); //https://orchsymServer.ORCHSYM.local/adfs
        String emailKey = EMAIL_CLAIM_NAME;
        if(iss!=null && iss.toLowerCase().endsWith("adfs")){
            emailKey = "upn";
        }
        return emailKey;
    }
}
