package com.ecommerce.gateway;

import com.ecommerce.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class AdminAuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;

    private static final String ADMIN_PREFIX = "/api/admin/";
    private static final String ADMIN_AUTH_PREFIX = "/api/admin/auth/";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        if (!path.startsWith(ADMIN_PREFIX) || path.startsWith(ADMIN_AUTH_PREFIX)) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null || jwtUtils.isTokenExpired(token)) {
            return writeJsonResponse(exchange, HttpStatus.UNAUTHORIZED,
                    "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        }

        Claims claims = jwtUtils.parseToken(token);
        String role = claims.get("role", String.class);

        if (!"ADMIN".equals(role) && !"SUPER_ADMIN".equals(role)) {
            return writeJsonResponse(exchange, HttpStatus.FORBIDDEN,
                    "{\"code\":403,\"message\":\"无管理员权限\",\"data\":null}");
        }

        return chain.filter(exchange);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 20;
    }

    private String extractToken(ServerHttpRequest request) {
        String auth = request.getHeaders().getFirst("Authorization");
        if (auth != null && auth.startsWith("Bearer ")) {
            return auth.substring(7);
        }
        return null;
    }

    private Mono<Void> writeJsonResponse(ServerWebExchange exchange, HttpStatus status, String body) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        DataBuffer buffer = exchange.getResponse()
                .bufferFactory()
                .wrap(body.getBytes(StandardCharsets.UTF_8));
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }
}
