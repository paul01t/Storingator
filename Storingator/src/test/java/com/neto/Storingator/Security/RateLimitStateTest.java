package com.neto.Storingator.Security;

import com.neto.Storingator.Service.AuthRateLimitService;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RateLimitStateTest {

    @Test
    void resolverUsesTrustedRightmostIpRatherThanCallerSuppliedFirstIp() {
        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxyHeader", "X-Forwarded-For");
        HttpServletRequest request = new MockHttpServletRequest();
        ((MockHttpServletRequest) request).addHeader(
                "X-Forwarded-For", "198.51.100.100, 203.0.113.7");

        assertEquals("203.0.113.7", resolver.resolve(request));
    }

    @Test
    void invalidOrOversizedForwardedValueFallsBackToPeerAddress() {
        ClientIpResolver resolver = new ClientIpResolver();
        ReflectionTestUtils.setField(resolver, "trustedProxyHeader", "X-Forwarded-For");
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("172.30.0.2");
        request.addHeader("X-Forwarded-For", "not-an-ip");

        assertEquals("172.30.0.2", resolver.resolve(request));
    }

    @Test
    void requestRateLimitStateHasHardBound() throws Exception {
        RequestRateLimitService service = new RequestRateLimitService();
        for (int i = 0; i < RequestRateLimitService.MAX_TRACKED_KEYS + 1; i++) {
            assertTrue(service.allow("ip:" + i, 20, Duration.ofMinutes(1)));
        }

        Map<?, ?> windows = (Map<?, ?>) ReflectionTestUtils.getField(service, "windows");
        assertEquals(RequestRateLimitService.MAX_TRACKED_KEYS, windows.size());
    }

    @Test
    void authRateLimitStateHasHardBoundForArbitraryUsernames() throws Exception {
        AuthRateLimitService service = new AuthRateLimitService();
        for (int i = 0; i < AuthRateLimitService.MAX_TRACKED_KEYS + 1; i++) {
            service.recordFailure("signin:user-" + i, Duration.ofMinutes(15));
        }

        Map<?, ?> windows = (Map<?, ?>) ReflectionTestUtils.getField(service, "windows");
        assertEquals(AuthRateLimitService.MAX_TRACKED_KEYS, windows.size());
    }
}
