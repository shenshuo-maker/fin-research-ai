package com.finresearch.web;

import com.finresearch.security.SecurityUser;
import com.finresearch.security.SecurityUserStub;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class AuthHelper {

    private AuthHelper() {}

    public static Long currentUserId() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || auth.getPrincipal() == null) {
            return null;
        }
        if (auth.getPrincipal() instanceof SecurityUserStub stub) {
            return stub.getId();
        }
        if (auth.getPrincipal() instanceof SecurityUser su) {
            return su.getId();
        }
        return null;
    }
}
