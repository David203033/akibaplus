package com.akibaplus.saccos.akibaplus.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Collection;

@Component
public class RoleBasedAuthenticationSuccessHandler implements AuthenticationSuccessHandler {

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) throws IOException, ServletException {
        Collection<? extends GrantedAuthority> authorities = authentication.getAuthorities();
        String targetUrl = "/dashboard";
        for (GrantedAuthority auth : authorities) {
            String role = auth.getAuthority();
            if ("ROLE_ADMIN".equals(role)) {
                targetUrl = "/admin/dashboard";
                break;
            }
        }
        response.sendRedirect(request.getContextPath() + targetUrl);
    }
}
