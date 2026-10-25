package com.smartclass.smartclassbackendgateway.filter;

import org.reactivestreams.Publisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.http.server.reactive.ServerHttpResponseDecorator;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 响应装饰器 - 用于记录响应日志
 */
public class LoggingResponseDecorator extends org.springframework.http.server.reactive.ServerHttpResponseDecorator {

    private static final Logger log = org.slf4j.LoggerFactory.getLogger(LoggingResponseDecorator.class);

    private final org.springframework.http.server.reactive.ServerHttpRequest request;
    private final String traceId;
    private final String spanId;
    private final Instant startTime;
    private final ServerWebExchange exchange;
    private final StructuredLoggingFilter filter;

    public LoggingResponseDecorator(org.springframework.http.server.reactive.ServerHttpResponse delegate,
                                    org.springframework.http.server.reactive.ServerHttpRequest request,
                                    String traceId,
                                    String spanId,
                                    Instant startTime,
                                    ServerWebExchange exchange,
                                    StructuredLoggingFilter filter) {
        super(delegate);
        this.request = request;
        this.traceId = traceId;
        this.spanId = spanId;
        this.startTime = startTime;
        this.exchange = exchange;
        this.filter = filter;
    }

    @Override
    public reactor.core.publisher.Mono<Void> writeWith(org.reactivestreams.Publisher<? extends DataBuffer> body) {
        Instant endTime = java.time.Instant.now();
        java.time.Duration latency = java.time.Duration.between(startTime, endTime);
        
        // 记录响应日志
        Map<String, Object> requestLog = (Map<String, Object>) exchange.getAttributes().getOrDefault("requestLog", new java.util.concurrent.ConcurrentHashMap<String, Object>());
        Map<String, Object> responseLog = filter.buildResponseLog(
                request, getDelegate(), traceId, spanId, 
                java.time.Duration.between(startTime, endTime), 
                requestLog,
                exchange);
        log.info("gateway_response: {}", filter.toJson(responseLog));
        
        return super.writeWith(body);
    }
    
    @Override
    public reactor.core.publisher.Mono<Void> writeAndFlushWith(org.reactivestreams.Publisher<? extends org.reactivestreams.Publisher<? extends DataBuffer>> body) {
        Instant endTime = java.time.Instant.now();
        java.time.Duration latency = java.time.Duration.between(startTime, endTime);
        
        Map<String, Object> requestLog2 = (Map<String, Object>) exchange.getAttributes().getOrDefault("requestLog", new java.util.concurrent.ConcurrentHashMap<String, Object>());
        Map<String, Object> responseLog = filter.buildResponseLog(
                request, getDelegate(), traceId, spanId,
                java.time.Duration.between(startTime, endTime),
                requestLog2,
                exchange);
        log.info("gateway_response: {}", filter.toJson(responseLog));
        
        return super.writeAndFlushWith(body);
    }
}