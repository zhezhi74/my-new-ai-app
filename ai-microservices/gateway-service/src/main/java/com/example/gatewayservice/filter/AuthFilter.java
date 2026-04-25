package com.example.gatewayservice.filter;

import com.example.gatewayservice.util.JwtUtil;
import io.jsonwebtoken.Claims;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class AuthFilter implements GlobalFilter, Ordered {

    private final JwtUtil jwtUtil;

    // 使用构造函数注入
    public AuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        String path = request.getURI().getPath();

        // 对 OPTIONS 预检请求，直接放行，交给后续的 CORS 处理器
        if (request.getMethod() == HttpMethod.OPTIONS) {
            log.debug("放行 OPTIONS 预检请求: {}", path);
            return chain.filter(exchange);
        }

        // 对于登录和注册接口，同样直接放行
        if (path.contains("/api/user/login") || path.contains("/api/user/register")) {
            log.debug("放行公共接口: {}", path);
            return chain.filter(exchange);
        }

        log.debug("需要认证的接口: {}", path);
        // 从请求头或URL参数获取Token
        String token = resolveToken(request);

        // 校验Token
        Claims claims = jwtUtil.parseToken(token);
        if (claims == null) {
            log.warn("Token无效或已过期, 拒绝访问: {}", path);
            response.setStatusCode(HttpStatus.UNAUTHORIZED);
            return response.setComplete();
        }

        // 校验管理员权限
        if (path.contains("/api/admin/")) {
            String role = claims.get("role", String.class);
            if (!"admin".equals(role)) {
                log.warn("用户无管理员权限, 拒绝访问: {}", path);
                response.setStatusCode(HttpStatus.FORBIDDEN);
                return response.setComplete();
            }
        }

        // 将用户信息传递给下游服务
        ServerHttpRequest modifiedRequest = request.mutate()
                .header("X-User-Id", claims.get("userId").toString())
                .header("X-User-Name", claims.get("username", String.class))
                .header("X-User-Role", claims.get("role", String.class))
                .build();

        return chain.filter(exchange.mutate().request(modifiedRequest).build());
    }

    private String resolveToken(ServerHttpRequest request) {
        // 优先从请求头 "Authorization" 中获取
        String authHeader = request.getHeaders().getFirst("Authorization");
        if (StringUtils.hasText(authHeader) && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        // 其次从URL参数获取 (为了兼容SSE)
        return request.getQueryParams().getFirst("token");
    }

    @Override
    public int getOrder() {
        return -1; // 确保是优先级最高的过滤器之一
    }
}