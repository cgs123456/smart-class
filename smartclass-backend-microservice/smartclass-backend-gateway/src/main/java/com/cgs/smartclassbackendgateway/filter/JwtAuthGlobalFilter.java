package com.cgs.smartclassbackendgateway.filter;

import com.cgs.smartclassbackendgateway.config.GatewayJwtUtil;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

/**
 * 网关 JWT 全局过滤器
 *
 * <p>职责：</p>
 * <ul>
 *   <li>校验请求中的 JWT，无效则直接返回 401</li>
 *   <li>拦截外部对内部接口（/inner/）的访问，仅允许内网调用</li>
 *   <li>校验通过后，把 userId / userRole 注入下游请求头，供微服务使用</li>
 * </ul>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthGlobalFilter implements GlobalFilter, Ordered {

    private final GatewayJwtUtil jwtUtil;
    private final AntPathMatcher pathMatcher = new AntPathMatcher();

    /**
     * 公开访问白名单（不需要 JWT 即可访问）。
     */
    private static final List<String> WHITELIST = Arrays.asList(
            // 用户登录注册
            "/api/user/login",
            "/api/user/login/phone",
            "/api/user/register",
            "/api/user/register/phone",
            // 验证码
            "/api/captcha",
            "/api/captcha/**",
            // Knife4j / Swagger
            "/api/doc.html",
            "/api/webjars/**",
            "/api/v3/api-docs/**",
            "/api/swagger-resources/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            // 网关自身文档
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            // 健康检查
            "/api/actuator/health",
            "/api/actuator/health/**",
            "/api/actuator/info",
            // 支付回调（依赖签名验签保护）
            "/api/pay/payment/order/callback/**",
            // 微信公众号入口
            "/api/wx/**"
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        String path = request.getURI().getPath();

        // 1. 拦截外部对 /inner/** 的访问：内部接口禁止外部调用
        if (isInnerPath(path)) {
            log.warn("拦截外部访问 inner 接口: {}", path);
            return writeError(exchange, HttpStatus.FORBIDDEN, "禁止访问内部接口");
        }

        // 2. 白名单放行
        if (isWhitelisted(path)) {
            return chain.filter(exchange);
        }

        // 3. 解析并校验 JWT
        String authHeader = request.getHeaders().getFirst(jwtUtil.getHeader());
        if (authHeader == null || !authHeader.startsWith(jwtUtil.getTokenPrefix())) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "未登录或 token 缺失");
        }
        String token = authHeader.substring(jwtUtil.getTokenPrefix().length()).trim();
        Claims claims = jwtUtil.parseAndValidate(token);
        if (claims == null) {
            return writeError(exchange, HttpStatus.UNAUTHORIZED, "token 无效或已过期");
        }

        // 4. 把 userId / userRole 注入下游请求头（去除外部传入的伪造值，防止越权）
        Long userId = jwtUtil.extractUserId(claims);
        String userRole = jwtUtil.extractUserRole(claims);
        ServerHttpRequest mutated = request.mutate()
                .header("X-User-Id", userId == null ? "" : String.valueOf(userId))
                .header("X-User-Role", userRole == null ? "" : userRole)
                .build();
        // 移除外部可能伪造的内部头（防御性，避免被下游信任）
        HttpHeaders headers = mutated.getHeaders();
        headers.remove("X-Internal-Call");

        return chain.filter(exchange.mutate().request(mutated).build());
    }

    /**
     * 判断是否为 inner 接口路径（按服务前缀剥离后的内部接口）。
     */
    private boolean isInnerPath(String path) {
        if (path == null) {
            return false;
        }
        // 匹配 /api/{service}/inner/** 模式
        return pathMatcher.match("/api/*/inner/**", path)
                || pathMatcher.match("/api/*/inner", path);
    }

    /**
     * 判断是否在白名单内。
     */
    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 返回错误响应。
     */
    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, String message) {
        ServerHttpResponse response = exchange.getResponse();
        response.setStatusCode(status);
        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);
        String body = "{\"code\":" + status.value() + ",\"message\":\"" + message + "\",\"data\":null}";
        DataBuffer buffer = response.bufferFactory().wrap(body.getBytes(StandardCharsets.UTF_8));
        return response.writeWith(Mono.just(buffer));
    }

    @Override
    public int getOrder() {
        // 限流过滤器之后、路由过滤器之前
        return -100;
    }
}
