package com.neto.Storingator.Security;

import com.neto.Storingator.Entities.Activeusers;
import com.neto.Storingator.Exceptions.UnauthorizedException;
import com.neto.Storingator.Service.SessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
public class BearerAuthFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(BearerAuthFilter.class);

    @Autowired
    private SessionService sessionService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            try {
                Activeusers active = sessionService.authenticateFromBearer(header);
                UsernamePasswordAuthenticationToken authentication =
                        UsernamePasswordAuthenticationToken.authenticated(active.getUsername(), active.getLoginkey(), List.of());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (UnauthorizedException e) {
                SecurityContextHolder.clearContext();
                log.warn("Rejected bearer token for {} from {}: {}",
                        request.getRequestURI(), request.getRemoteAddr(), e.getMessage());
            }
        }

        filterChain.doFilter(request, response);
    }
}