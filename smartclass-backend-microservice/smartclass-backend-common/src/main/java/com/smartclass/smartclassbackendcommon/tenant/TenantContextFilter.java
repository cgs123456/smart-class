package com.smartclass.smartclassbackendcommon.tenant;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;

/**
 * 租户上下文拦截器
 * 从 HTTP 请求头解析租户信息并设置到 TenantContextHolder
 */
@Component
public class TenantContextFilter extends OncePerRequestFilter {

    private static final Logger log = LoggerFactory.getLogger(TenantContextFilter.class);

    private final AntPathMatcher pathMatcher = new AntPathMatcher();
    private final TenantRegistry tenantRegistry;

    // 租户识别白名单 (不需要租户上下文的路径)
    private static final List<String> WHITELIST = java.util.Arrays.asList(
            "/api/user/login",
            "/api/user/register",
            "/api/user/login/phone",
            "/api/user/register/phone",
            "/api/captcha",
            "/api/captcha/**",
            "/api/actuator/health",
            "/api/actuator/health/**",
            "/api/actuator/info",
            "/api/doc.html",
            "/api/webjars/**",
            "/api/v3/api-docs/**",
            "/api/swagger-resources/**",
            "/api/swagger-ui/**",
            "/api/swagger-ui.html",
            "/doc.html",
            "/webjars/**",
            "/v3/api-docs/**",
            "/api/pay/payment/order/callback/**",
            "/api/wx/**"
    );

    // 租户识别头
    private static final String TENANT_HEADER = "X-Tenant-Id";

    public TenantContextFilter(TenantRegistry tenantRegistry) {
        this.tenantRegistry = tenantRegistry;
    }

    @Override
    protected void doFilterInternal(jakarta.servlet.http.HttpServletRequest request,
                                     jakarta.servlet.http.HttpServletResponse response,
                                     FilterChain filterChain) throws ServletException, IOException {

        String path = request.getRequestURI();

        // 1. 白名单跳过租户校验
        if (isWhitelisted(path)) {
            filterChain.doFilter(request, response);
            return;
        }

        // 2. 解析租户 ID
        String tenantId = resolveTenantId(request);

        // 3. 校验租户状态
        TenantInfo tenant = tenantRegistry.get(tenantId);
        if (tenant == null) {
            log.warn("租户不存在: {}", tenantId);
            writeError(response, 404, "租户不存在: " + tenantId);
            return;
        }

        if (!tenant.isActive()) {
            log.warn("租户状态异常: {} - {}", tenantId, tenant.getStatus());
            writeError(response, 403, "租户已禁用或过期: " + tenantId);
            return;
        }

        // 设置租户上下文
        TenantContext context = tenant.toContext();
        TenantContextHolder.set(context);

        try {
            filterChain.doFilter(request, response);
        } finally {
            // 清理上下文，防止内存泄漏
            TenantContextHolder.clear();
        }
    }

    /**
     * 解析租户 ID (按优先级)
     */
    private String resolveTenantId(jakarta.servlet.http.HttpServletRequest request) {
        // 优先级 1: Header (最高，网关/内部调用显式指定)
        String tenantId = request.getHeader("X-Tenant-Id");
        if (tenantId != null && !tenantId.isEmpty()) {
            return tenantId;
        }

        // 优先级 2: Subdomain (tenant.smartclass.cn)
        String host = request.getServerName();
        if (host != null) {
            String subdomain = extractSubdomain(host);
            if (subdomain != null && !subdomain.equals("www") && !subdomain.equals("api")) {
                // 验证子域名是否为合法租户编码
                if (tenantRegistry.getByCode(subdomain) != null) {
                    return subdomain;
                }
            }
        }

        // 优先级 3: Path (/t/{tenantId}/...)
        String path = request.getRequestURI();
        AntPathMatcher pathMatcher = new AntPathMatcher();
        if (pathMatcher.match("/t/{tenantId}/**", path)) {
            String pathTenantId = pathMatcher.extractUriTemplateVariables("/t/{tenantId}/**", path).get("tenantId");
            if (pathTenantId != null) {
                return pathTenantId;
            }
        }

        // 默认租户
        return "default";
    }

    private String extractSubdomain(String host) {
        int portIndex = host.indexOf(':');
        String hostname = portIndex > 0 ? host.substring(0, portIndex) : host;
        String[] parts = hostname.split("\\.");
        if (parts.length >= 3) {
            return parts[0];
        }
        return null;
    }

    private boolean isWhitelisted(String path) {
        for (String pattern : WHITELIST) {
            if (pathMatcher.match(pattern, path)) {
                return true;
            }
        }
        return false;
    }

    private void writeError(jakarta.servlet.http.HttpServletResponse response, int status, String message) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        String body = String.format("{\"code\":%d,\"message\":\"%s\",\"data\":null}", status, message);
        response.getWriter().write(body);
    }
}