package com.neto.Storingator.Security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Component
public class ClientIpResolver {

    /** Longest textual form of an IPv6 address, e.g. 39 chars for ::ffff:255.255.255.255. */
    private static final int MAX_IP_LENGTH = 45;

    private static final Pattern IPV4 =
            Pattern.compile("(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})\\.(\\d{1,3})");
    private static final Pattern IPV6 = Pattern.compile("[0-9a-fA-F:.]+");

    @Value("${app.security.trusted-proxy-header:}")
    private String trustedProxyHeader;

    public String resolve(HttpServletRequest request) {
        if (trustedProxyHeader != null && !trustedProxyHeader.isBlank()) {
            String value = request.getHeader(trustedProxyHeader);
            if (value != null && !value.isBlank()) {
                String candidate = lastEntry(value);
                if (isIpLiteral(candidate)) {
                    return candidate;
                }
            }
        }
        return request.getRemoteAddr();
    }

    /**
     * A trusted proxy appends the address it saw to the right of anything the caller
     * supplied. Only the last entry is vouched for by the proxy; using the leftmost
     * value would let a caller choose its own rate-limit bucket.
     */
    private String lastEntry(String headerValue) {
        int end = headerValue.length();
        while (end > 0) {
            int comma = headerValue.lastIndexOf(',', end - 1);
            String candidate = headerValue.substring(comma + 1, end).trim();
            if (!candidate.isEmpty()) {
                return candidate;
            }
            end = comma;
        }
        return "";
    }

    /** Keep arbitrary header text out of the rate-limit maps (and therefore out of memory). */
    private boolean isIpLiteral(String value) {
        if (value == null || value.isEmpty() || value.length() > MAX_IP_LENGTH) {
            return false;
        }
        Matcher ipv4 = IPV4.matcher(value);
        if (ipv4.matches()) {
            for (int i = 1; i <= 4; i++) {
                if (Integer.parseInt(ipv4.group(i)) > 255) {
                    return false;
                }
            }
            return true;
        }
        return value.indexOf(':') >= 0 && IPV6.matcher(value).matches();
    }
}
