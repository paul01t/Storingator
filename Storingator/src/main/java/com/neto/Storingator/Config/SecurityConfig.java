package com.neto.Storingator.Config;

import com.neto.Storingator.Security.BearerAuthFilter;
import com.neto.Storingator.Security.ClientIpResolver;
import com.neto.Storingator.Security.GlobalRateLimitFilter;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authorization.AuthorizationDecision;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.util.matcher.IpAddressMatcher;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private static final Logger log = LoggerFactory.getLogger(SecurityConfig.class);

    @Value("${app.security.require-https:true}")
    private boolean requireHttps;

    @Value("${app.cors.allowed-origin-patterns:http://localhost:*}")
    private String allowedOriginPatterns;

    @Value("${app.security.registration-allowed-ips:127.0.0.0/8,::1}")
    private String registrationAllowedIps;

    private final GlobalRateLimitFilter globalRateLimitFilter;
    private final BearerAuthFilter bearerAuthFilter;
    private final ClientIpResolver clientIpResolver;

    public SecurityConfig(GlobalRateLimitFilter globalRateLimitFilter,
                          BearerAuthFilter bearerAuthFilter,
                          ClientIpResolver clientIpResolver) {
        this.globalRateLimitFilter = globalRateLimitFilter;
        this.bearerAuthFilter = bearerAuthFilter;
        this.clientIpResolver = clientIpResolver;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(csrf -> csrf.disable())
            .cors(cors -> cors.configurationSource(corsConfigurationSource()))
            .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/api/v1/User/New", "/api/v1/User/ChangePassword", "/api/v1/User/Delete").access((authentication, context) ->
                        new AuthorizationDecision(isRequestIpAllowed(context.getRequest())))
                .requestMatchers("/api/v1/User/Signin", "/api/v1/User/", "/health").permitAll()
                .anyRequest().authenticated())
            .headers(headers -> headers
                .frameOptions(frame -> frame.deny())
                .contentSecurityPolicy(csp -> csp.policyDirectives("default-src 'self'"))
                .referrerPolicy(referrer -> referrer.policy(
                        org.springframework.security.web.header.writers.ReferrerPolicyHeaderWriter.ReferrerPolicy.SAME_ORIGIN)))
            .exceptionHandling(handling -> handling
                .authenticationEntryPoint((request, response, ex) -> {
                    log.warn("Auth entry point triggered for {} from {}: {}",
                            request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
                    response.setStatus(HttpStatus.UNAUTHORIZED.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Unauthorized\"}");
                })
                .accessDeniedHandler((request, response, ex) -> {
                    log.warn("Access denied for {} from {}: {}",
                            request.getRequestURI(), request.getRemoteAddr(), ex.getMessage());
                    response.setStatus(HttpStatus.FORBIDDEN.value());
                    response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                    response.getWriter().write("{\"error\":\"Forbidden\"}");
                }));

        if (requireHttps) {
            http.redirectToHttps(Customizer.withDefaults());
        }

        http
            .addFilterBefore(globalRateLimitFilter, UsernamePasswordAuthenticationFilter.class)
            .addFilterBefore(bearerAuthFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(allowedOriginPatterns.split(",")));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type"));
        config.setMaxAge(3600L);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    private boolean isRequestIpAllowed(HttpServletRequest request) {
        String ip = clientIpResolver.resolve(request);
        boolean allowed = false;
        for (String entry : registrationAllowedIps.split(",")) {
            try {
                IpAddressMatcher matcher = new IpAddressMatcher(entry.trim());
                if (matcher.matches(ip)) {
                    allowed = true;
                    log.info("Request IP {} matched allowlist {}", ip, entry.trim());
                    break;
                }
            } catch (IllegalArgumentException e) {
                log.warn("Ignoring invalid allowed IP: {}", entry);
            }
        }
        if (!allowed) {
            log.warn("Request denied for IP '{}' from {}. Allowed: {}",
                    ip, request.getRemoteAddr(), registrationAllowedIps);
        }
        return allowed;
    }
}