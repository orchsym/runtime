/*
 * Licensed to the Orchsym Runtime under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 * 
 * this file to You under the Orchsym License, Version 1.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * https://github.com/orchsym/runtime/blob/master/orchsym/LICENSE
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.nifi.orchsym;

import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.ResponseHandler;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.TrustSelfSignedStrategy;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.ssl.SSLContextBuilder;
import org.apache.http.util.EntityUtils;
import org.apache.nifi.authentication.AuthenticationResponse;
import org.apache.nifi.authentication.LoginCredentials;
import org.apache.nifi.authentication.LoginIdentityProvider;
import org.apache.nifi.authentication.LoginIdentityProviderConfigurationContext;
import org.apache.nifi.authentication.LoginIdentityProviderInitializationContext;
import org.apache.nifi.authentication.exception.IdentityAccessException;
import org.apache.nifi.authentication.exception.InvalidLoginCredentialsException;
import org.apache.nifi.authentication.exception.ProviderCreationException;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.KeyManagementException;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.SSLContext;

/**
 * @author Zhou Guoliang
 */
public class OrchsymLoginIdentityProvider implements LoginIdentityProvider {

    private final Map<String, String> users;
    private String authUrl;

    /**
     * Creates a new OrchsymLoginIdentityProvider.
     */
    public OrchsymLoginIdentityProvider() {
        users = new HashMap<>();
    }

    private boolean checkDefaultUser(final String user, final String password) {
        if (users.containsKey(user)) {
            if (users.get(user).equals(password)) {
                return true;
            }
        }
        return false;
    }
    
    private void checkOrchsymUser(final String user, final String password) {
        if(checkDefaultUser(user, password)) {
            return;
        }
        
        try {
            SSLContext sslContext = new SSLContextBuilder()
                .loadTrustMaterial(null, TrustSelfSignedStrategy.INSTANCE).build();
            CloseableHttpClient httpclient = HttpClients.custom()
                    .setSSLContext(sslContext)
                    .setSSLHostnameVerifier(new NoopHostnameVerifier())
                    .build();
        
            final URI url = new URI(authUrl);
            final HttpPost post = new HttpPost(url);
            String json = "{\"email\":\"" + user + "\",\"password\":\"" + password + "\", \"remember\": false}";
            StringEntity entity = new StringEntity(json);
            post.setEntity(entity);
            post.setHeader("Accept", "application/json");
            post.setHeader("Content-type", "application/json");
            // Create a custom response handler
            ResponseHandler<String> responseHandler = new ResponseHandler<String>() {
    
                @Override
                public String handleResponse(
                        final HttpResponse response) throws ClientProtocolException, IOException {
                    int status = response.getStatusLine().getStatusCode();
                    if (status >= 200 && status < 300) {
                        HttpEntity entity = response.getEntity();
                        return entity != null ? EntityUtils.toString(entity) : null;
                    } else if(status == 400 || status == 401) {
                        throw new InvalidLoginCredentialsException("Invalid username or password");
                    } else {
                        throw new ClientProtocolException("Unexpected response status: " + status);
                    }
                }
    
            };
            String responseBody = httpclient.execute(post, responseHandler);
            try {
                httpclient.close();
            } catch (IOException e) {
                // ignore exception
            }
        } catch (KeyManagementException | NoSuchAlgorithmException | KeyStoreException e1) {
            throw new IdentityAccessException("Unable to initialize SSL context.", e1);
        } catch (IOException | URISyntaxException e) {
            throw new IdentityAccessException("The Orchsym authentication provider is not initialized.", e);
        }
    }

    @Override
    public AuthenticationResponse authenticate(LoginCredentials credentials) throws InvalidLoginCredentialsException, IdentityAccessException {
        checkOrchsymUser(credentials.getUsername(), credentials.getPassword());
        return new AuthenticationResponse(credentials.getUsername(), credentials.getUsername(), TimeUnit.MILLISECONDS.convert(1, TimeUnit.DAYS), getClass().getSimpleName());
    }

    @Override
    public void initialize(LoginIdentityProviderInitializationContext initializationContext) throws ProviderCreationException {
    }

    @Override
    public void onConfigured(LoginIdentityProviderConfigurationContext configurationContext) throws ProviderCreationException {
        // built in user
        final String username = configurationContext.getProperty("Default User");
        final String password = configurationContext.getProperty("Default Password");
        if (!StringUtils.isBlank(username) && !StringUtils.isBlank(password)) {
            users.put(username, password);
        }
        // remote auth url
        final String url = configurationContext.getProperty("Url");

        if (StringUtils.isBlank(url)) {
            throw new ProviderCreationException("Orchsym identity provider 'Url' must be specified.");
        }
        
        this.authUrl = url;
    }

    @Override
    public void preDestruction() {
    }

}
