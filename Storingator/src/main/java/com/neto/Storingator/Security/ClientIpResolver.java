package com.neto.Storingator.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class ClientIpResolver {

    @Value("${app.security.trusted-proxy-header:}")
    private String trustedProxyHeader;

    public String resolve(HttpServletRequest request) {
        if (trustedProxyHeader != null && !trustedProxyHeader.isBlank()) {
            String value = request.getHeader(trustedProxyHeader);
            if (value != null && !value.isBlank()) {
                int comma = value.indexOf(',');
                return (comma > 0 ? value.substring(0, comma) : value).trim();
            }
        }
        return request.getRemoteAddr();
    }
}