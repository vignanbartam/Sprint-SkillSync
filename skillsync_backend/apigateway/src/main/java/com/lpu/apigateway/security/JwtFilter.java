package com.lpu.apigateway.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import reactor.core.publisher.Mono;

import java.security.Key;

@Component
public class JwtFilter implements GlobalFilter, Ordered {

    @Value("${jwt.secret}")
    private String secret;

    private Key key;

    @PostConstruct
    public void init() {
        this.key = Keys.hmacShaKeyFor(secret.getBytes());
    }

    @Override
    public int getOrder() {
        return -1; // Run before other filters
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {

        String path = exchange.getRequest().getURI().getPath();

        if (isPublicPath(path)) {
            return chain.filter(exchange);
        }

        String token = resolveToken(exchange);

        if (token == null) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }

        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(key)
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            Object userIdObj = claims.get("userId");
            if (userIdObj == null) {
                exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            }

            String userId = String.valueOf(userIdObj);
            String role   = String.valueOf(claims.get("role"));

            ServerHttpRequest mutatedRequest = exchange.getRequest()
                    .mutate()
                    .header("X-User-Id", userId)
                    .header("X-User-Role", role)
                    .build();

            return chain.filter(exchange.mutate().request(mutatedRequest).build());

        } catch (Exception e) {
            exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
            return exchange.getResponse().setComplete();
        }
    }

    private String resolveToken(ServerWebExchange exchange) {
        String header = exchange.getRequest().getHeaders().getFirst("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            return header.substring(7);
        }

        String path = exchange.getRequest().getURI().getPath();
        if (!isWebSocketPath(path)) {
            return null;
        }

        String queryToken = exchange.getRequest().getQueryParams().getFirst("access_token");
        return queryToken == null || queryToken.isBlank() ? null : queryToken;
    }

    private boolean isWebSocketPath(String path) {
        return path.endsWith("/ws") || path.contains("/ws/");
    }

    private boolean isPublicPath(String path) {
        return path.contains("/auth/login")
            || path.contains("/auth/register")
            || path.contains("/auth/register/verify")
            || path.contains("/auth/mentors")
            || path.contains("/auth/profile-picture/")
            || path.contains("/auth/biodata/")
            // ── Swagger UI assets ────────────────────────────────
            || path.contains("/swagger-ui")
            || path.contains("/swagger-ui.html")
            || path.contains("/v3/api-docs")          // all services' api-docs
            || path.contains("/webjars/")
            || path.contains("/swagger-resources")
            // ── Internal Feign calls ──────────────────────────────
            || path.contains("/session/check")
            // ── Actuator (Eureka health checks) ──────────────────
            || path.contains("/actuator");
    }
}
