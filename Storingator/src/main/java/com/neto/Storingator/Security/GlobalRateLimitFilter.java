package com.neto.Storingator.Security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Component
public class GlobalRateLimitFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(GlobalRateLimitFilter.class);
    private static final int MAX_REQUESTS_PER_MINUTE = 20;
    private static final Duration WINDOW = Duration.ofMinutes(1);

    @Autowired
    private RequestRateLimitService requestRateLimitService;

    @Autowired
    private ClientIpResolver clientIpResolver;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = clientIpResolver.resolve(request);
        String key = "ip:" + clientIp;

        if (!requestRateLimitService.allow(key, MAX_REQUESTS_PER_MINUTE, WINDOW)) {
            log.warn("Rate limit exceeded for IP: {}", clientIp);
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write("{\"error\":\"Too many requests\"}");
            return;
        }

        filterChain.doFilter(request, response);
    }
}