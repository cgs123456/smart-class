package com.smartclass.smartclassbackendgateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 网关结构化日志 + TraceId 透传 Filter
 * 
 * 功能：
 * 1. 生成/透传 TraceId 和 SpanId
 * 2. 记录请求/响应结构化日志 (JSON 格式，ELK 友好)
 * 3. 脱敏敏感头信息
 * 4. 记录请求耗时、状态码、上下游信息
 */
@Component
public class StructuredLoggingFilter implements GlobalFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(StructuredLoggingFilter.class);

    private static final String TRACE_ID_HEADER = "X-Trace-Id";
    private static final String SPAN_ID_HEADER = "X-Span-Id";
    private static final String REQUEST_START_TIME = "requestStartTime";
    
    // 敏感头列表（不记录值，只记录存在性）
    private static final Map<String, Boolean> SENSITIVE_HEADERS = Map.of(
            "authorization", true,
            "cookie", true,
            "x-api-key", true,
            "x-auth-token", true
    );

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        ServerHttpRequest request = exchange.getRequest();
        ServerHttpResponse response = exchange.getResponse();
        
        // 1. 生成/提取 TraceId 和 SpanId
        String traceId = request.getHeaders().getFirst(TRACE_ID_HEADER);
        if (traceId == null || traceId.isEmpty()) {
            traceId = UUID.randomUUID().toString().replace("-", "");
        }
        String spanId = UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        
        // 2. 记录开始时间
        final Instant startTime = Instant.now();
        exchange.getAttributes().put(REQUEST_START_TIME, startTime);
        exchange.getAttributes().put("requestLog", new ConcurrentHashMap<>());
        
        // 3. 构建请求日志上下文
        Map<String, Object> requestLog = buildRequestLog(request, traceId, spanId);
        exchange.getAttributes().put("requestLog", requestLog);
        
        // 4. 记录请求日志
        log.info("gateway_request: {}", toJson(requestLog));
        
        // 5. 将 TraceId/SpanId 注入响应头
        response.getHeaders().add(TRACE_ID_HEADER, traceId);
        response.getHeaders().add(SPAN_ID_HEADER, spanId);
        
        // 6. 装饰响应以捕获响应体和状态码
        LoggingResponseDecorator decoratedResponse = new LoggingResponseDecorator(
                response, request, traceId, spanId, startTime, exchange, this);
        
        // 7. 继续过滤器链
        return chain.filter(exchange.mutate().response(decoratedResponse).build());
    }

    private Map<String, Object> buildRequestLog(ServerHttpRequest request, String traceId, String spanId) {
        InetSocketAddress remoteAddress = request.getRemoteAddress();
        String clientIp = remoteAddress != null ? remoteAddress.getAddress().getHostAddress() : "unknown";
        
        Map<String, Object> log = new ConcurrentHashMap<>();
        log.put("timestamp", Instant.now().toString());
        log.put("trace_id", traceId);
        log.put("span_id", spanId);
        log.put("level", "INFO");
        log.put("service", "gateway");
        log.put("event", "request_received");
        
        // HTTP 信息
        log.put("method", request.getMethod() != null ? request.getMethod().name() : "UNKNOWN");
        log.put("path", request.getPath().value());
        log.put("query", request.getURI().getRawQuery());
        log.put("protocol", "HTTP/1.1"); // 网关层通常是 HTTP/1.1
        
        // 客户端信息
        log.put("client_ip", clientIp);
        log.put("user_agent", request.getHeaders().getFirst(HttpHeaders.USER_AGENT));
        
        // 用户上下文 (从鉴权 Filter 注入)
        log.put("user_id", request.getHeaders().getFirst("X-User-Id"));
        log.put("user_role", request.getHeaders().getFirst("X-User-Role"));
        log.put("tenant_id", request.getHeaders().getFirst("X-Tenant-Id"));
        
        // 头信息 (脱敏)
        Map<String, Object> headers = new ConcurrentHashMap<>();
        request.getHeaders().forEach((k, v) -> {
            if (SENSITIVE_HEADERS.containsKey(k.toLowerCase())) {
                headers.put(k, "[REDACTED]");
            } else {
                headers.put(k, v.size() == 1 ? v.get(0) : v);
            }
        });
        log.put("headers", headers);
        
        return log;
    }

    Map<String, Object> buildResponseLog(
            ServerHttpRequest request,
            ServerHttpResponse response,
            String traceId,
            String spanId,
            Duration latency,
            Map<String, Object> requestLog,
            ServerWebExchange exchange) {
        
        Map<String, Object> log = new ConcurrentHashMap<>();
        log.put("timestamp", Instant.now().toString());
        log.put("trace_id", traceId);
        log.put("span_id", spanId);
        log.put("level", "INFO");
        log.put("service", "gateway");
        log.put("event", "response_sent");
        
        // 关联请求信息
        log.put("method", requestLog.get("method"));
        log.put("path", requestLog.get("path"));
        
        // 响应信息
        log.put("status", response.getStatusCode() != null ? response.getStatusCode().value() : 0);
        log.put("latency_ms", latency.toMillis());
        
        // 响应头 (脱敏)
        Map<String, Object> headers = new ConcurrentHashMap<>();
        response.getHeaders().forEach((k, v) -> {
            if (SENSITIVE_HEADERS.containsKey(k.toLowerCase())) {
                headers.put(k, "[REDACTED]");
            } else {
                headers.put(k, v.size() == 1 ? v.get(0) : v);
            }
        });
        log.put("response_headers", headers);
        
        // 路由信息
        String routeId = (String) exchange.getAttributes().getOrDefault("routeId", "unknown");
        log.put("route_id", routeId);
        
        // 状态分类
        int status = response.getStatusCode() != null ? response.getStatusCode().value() : 0;
        if (status >= 500) {
            log.put("level", "ERROR");
            log.put("error_type", "SERVER_ERROR");
        } else if (status >= 400) {
            log.put("level", "WARN");
            log.put("error_type", "CLIENT_ERROR");
        }
        
        return log;
    }

    String toJson(Map<String, Object> map) {
        // 简单的 JSON 序列化，生产环境建议用 Jackson ObjectMapper
        StringBuilder sb = new StringBuilder();
        sb.append('{');
        boolean first = true;
        for (Map.Entry<String, Object> entry : map.entrySet()) {
            if (!first) sb.append(',');
            sb.append('"').append(entry.getKey()).append("\":");
            sb.append(valueToJson(entry.getValue()));
            first = false;
        }
        sb.append('}');
        return sb.toString();
    }

    String valueToJson(Object value) {
        if (value == null) return "null";
        if (value instanceof String) return '"' + escapeJson((String) value) + '"';
        if (value instanceof Number || value instanceof Boolean) return value.toString();
        if (value instanceof Map) {
            return toJson((Map<String, Object>) value);
        }
        if (value instanceof Iterable) {
            StringBuilder sb = new StringBuilder("[");
            boolean first = true;
            for (Object item : (Iterable<?>) value) {
                if (!first) sb.append(',');
                sb.append(valueToJson(item));
                first = false;
            }
            sb.append(']');
            return sb.toString();
        }
        return '"' + escapeJson(value.toString()) + '"';
    }

    private String escapeJson(String s) {
        return s.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    @Override
    public int getOrder() {
        // 在鉴权之后、限流/熔断之前记录日志
        return -90;
    }
}