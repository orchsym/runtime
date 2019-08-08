package org.apache.nifi.web.security.oidc.cache;

import java.util.concurrent.TimeUnit;

import org.apache.nifi.web.security.util.CacheKey;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.nimbusds.oauth2.sdk.id.State;

public class OidcCache {

    private static Cache<CacheKey, State> stateLookupForPendingRequests; // identifier from cookie -> state value
    private static Cache<CacheKey, String> jwtLookupForCompletedRequests; // identifier from cookie -> jwt or identity (and generate jwt on retrieval)

    public static Cache<CacheKey, String> getJwtLookupForCompletedRequests(final int duration, final TimeUnit units){
        if(jwtLookupForCompletedRequests == null){
            jwtLookupForCompletedRequests = CacheBuilder.newBuilder().expireAfterWrite(duration, units).build();
        }
        return jwtLookupForCompletedRequests;
    }

    public static Cache<CacheKey, State> getStateLookupForPendingRequests(final int duration, final TimeUnit units) {
        if(stateLookupForPendingRequests == null){
            stateLookupForPendingRequests = CacheBuilder.newBuilder().expireAfterWrite(duration, units).build();
        }
        return stateLookupForPendingRequests;
    }

}
