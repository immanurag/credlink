package com.credlink.security;

import com.credlink.common.exception.ApiException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

/**
 * Resolves the authenticated merchant's id strictly from the JWT-backed SecurityContext.
 * Services must call this instead of trusting any merchantId sent by the client.
 */
@Component
public class CurrentMerchant {

    public Long id() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            throw new ApiException("UNAUTHORIZED", "No authenticated merchant in context.", HttpStatus.UNAUTHORIZED);
        }
        return (Long) auth.getPrincipal();
    }
}
