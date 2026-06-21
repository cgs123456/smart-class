package com.cgs.smartclass.filter;

import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ResultUtils;
import com.alibaba.fastjson.JSON;
import jakarta.annotation.Resource;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

@Slf4j
@Component
public class RateLimitFilter extends OncePerRequestFilter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    private static final String LUA_SCRIPT =
            "local key = KEYS[1]\n" +
            "local now = tonumber(ARGV[1])\n" +
            "local window = tonumber(ARGV[2])\n" +
            "local limit = tonumber(ARGV[3])\n" +
            "redis.call('ZREMRANGEBYSCORE', key, 0, now - window)\n" +
            "local count = redis.call('ZCARD', key)\n" +
            "if count < limit then\n" +
            "    redis.call('ZADD', key, now, now .. '-' .. math.random(1, 1000000))\n" +
            "    redis.call('EXPIRE', key, window / 1000)\n" +
            "    return 1\n" +
            "end\n" +
            "return 0";

    // 登录/注册接口每IP每分钟5次
    private static final int AUTH_LIMIT = 5;
    // 其他接口每IP每分钟60次
    private static final int COMMON_LIMIT = 60;
    // 滑动窗口：1分钟
    private static final long WINDOW_MS = 60 * 1000L;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String clientIp = getClientIp(request);
        String uri = request.getRequestURI();
        boolean isAuthEndpoint = isAuthEndpoint(uri);
        int limit = isAuthEndpoint ? AUTH_LIMIT : COMMON_LIMIT;

        String key = "rate_limit:" + (isAuthEndpoint ? "auth:" : "common:") + clientIp;
        long now = System.currentTimeMillis();

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>(LUA_SCRIPT, Long.class);
        Long result = stringRedisTemplate.execute(redisScript,
                Collections.singletonList(key),
                String.valueOf(now),
                String.valueOf(WINDOW_MS),
                String.valueOf(limit));

        if (result == null || result == 0L) {
            log.warn("请求限流，IP: {}, URI: {}", clientIp, uri);
            response.setStatus(429);
            response.setContentType("application/json;charset=UTF-8");
            BaseResponse<Void> baseResponse = ResultUtils.error(42900, "请求过于频繁，请稍后再试");
            response.getWriter().write(JSON.toJSONString(baseResponse));
            return;
        }

        filterChain.doFilter(request, response);
    }

    private boolean isAuthEndpoint(String uri) {
        return uri.contains("/login") || uri.contains("/register");
    }

    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        if (StringUtils.isBlank(ip) || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        // 多次反向代理后取第一个IP
        if (StringUtils.isNotBlank(ip) && ip.contains(",")) {
            ip = ip.split(",")[0].trim();
        }
        return ip;
    }
}
