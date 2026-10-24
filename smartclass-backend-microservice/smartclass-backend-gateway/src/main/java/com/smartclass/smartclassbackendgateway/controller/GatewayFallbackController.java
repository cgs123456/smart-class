package com.smartclass.smartclassbackendgateway.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

/**
 * 网关降级处理控制器
 * 
 * 当熔断器开启或限流触发时，提供友好的降级响应
 */
@RestController
@RequestMapping("/fallback")
public class GatewayFallbackController {

    private static final Logger log = LoggerFactory.getLogger(GatewayFallbackController.class);

    /**
     * 用户服务降级
     */
    @GetMapping("/user")
    public ResponseEntity<Map<String, Object>> userFallback() {
        log.warn("用户服务熔断降级触发");
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", "用户服务暂时不可用，请稍后重试");
        response.put("data", null);
        response.put("fallback", true);
        return ResponseEntity.status(503).body(response);
    }

    /**
     * 智能服务降级
     */
    @GetMapping("/intelligence")
    public ResponseEntity<Map<String, Object>> intelligenceFallback() {
        log.warn("智能服务熔断降级触发");
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", "AI 服务暂时不可用，请稍后重试");
        response.put("data", null);
        response.put("fallback", true);
        return ResponseEntity.status(503).body(response);
    }

    /**
     * 文件服务降级
     */
    @GetMapping("/file")
    public ResponseEntity<Map<String, Object>> fileFallback() {
        log.warn("文件服务熔断降级触发");
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", "文件服务暂时不可用，请稍后重试");
        response.put("data", null);
        response.put("fallback", true);
        return ResponseEntity.status(503).body(response);
    }

    /**
     * 通用降级
     */
    @GetMapping("/default")
    public ResponseEntity<Map<String, Object>> defaultFallback() {
        log.warn("通用熔断降级触发");
        Map<String, Object> response = new HashMap<>();
        response.put("code", 503);
        response.put("message", "服务暂时不可用，请稍后重试");
        response.put("data", null);
        response.put("fallback", true);
        return ResponseEntity.status(503).body(response);
    }
}