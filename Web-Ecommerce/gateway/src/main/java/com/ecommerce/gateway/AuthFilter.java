package com.ecommerce.gateway;

import com.ecommerce.security.JwtUtils;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
@RequiredArgsConstructor
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtUtils jwtUtils;
    private final ReactiveRedisTemplate<String, String> reactiveRedisTemplate;

    private static final List<String> PUBLIC_PATHS = List.of(
        "/api/auth", "/api/products", "/api/categories",
        "/api/banners", "/api/announcements", "/api/seckill",
        "/api/coupons", "/api/admin/auth", "/api/upload"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, org.springframework.cloud.gateway.filter.GatewayFilterChain chain) {
        String path = exchange.getRequest().getURI().getPath();

        boolean isPublic = PUBLIC_PATHS.stream().anyMatch(p ->
            path.equals(p) || path.startsWith(p + "/"));
        if (isPublic) {
            return chain.filter(exchange);
        }

        String token = extractToken(exchange.getRequest());
        if (token == null || jwtUtils.isTokenExpired(token)) {
            return writeJsonResponse(exchange, HttpStatus.UNAUTHORIZED,
                    "{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
        }

        Claims claims = jwtUtils.parseToken(token);
        Long userId = claims.get("userId", Long.class);
        String role = claims.get("role", String.class);

        return checkBlacklist(userId, token)
                .flatMap(isBlacklisted -> {
                    if (Boolean.TRUE.equals(isBlacklisted)) {
                        return writeJsonResponse(exchange, HttpStatus.UNAUTHORIZED,
                                "{\"code\":401,\"message\":\"Token已失效，请重新登录\",\"data\":null}");
                    }

                    ServerHttpRequest request = exchange.getRequest().mutate()
                            .header("X-User-Id", userId.toString())
                            .header("X-User-Role", role != null ? role : "")
                            .build();

                    return chain.filter(exchange.mutate().request(request).build());
                });
    }

    private Mono<Boolean> checkBlacklist(Long userId, String token) {
        String blacklistKey = "bl:" + userId + ":" + token.substring(0, Math.min(20, token.length()));
        return reactiveRedisTemplate.hasKey(blacklistKey);
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
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
