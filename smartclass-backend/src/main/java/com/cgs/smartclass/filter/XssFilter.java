package com.cgs.smartclass.filter;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletRequestWrapper;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * 全局 XSS 过滤器
 *
 * <p>对请求参数和 Header 中的危险字符进行转义，防止 XSS 攻击被存储到数据库或反射回页面。</p>
 *
 * <p>实现策略：</p>
 * <ul>
 *   <li>对 QueryString 参数和表单参数做 HTML4 转义</li>
 *   <li>对常见危险 Header 也做转义</li>
 *   <li>请求体（JSON）由 Jackson 反序列化时负责，如有需要可在 WebMvcConfigurer 中扩展</li>
 * </ul>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 100)
public class XssFilter implements Filter {

    /**
     * 需要做 XSS 转义的 Header 名称（不区分大小写）。
     */
    private static final String[] SANITIZED_HEADERS = {
            "referer", "user-agent", "x-forwarded-for", "x-real-ip"
    };

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        if (request instanceof HttpServletRequest httpRequest) {
            chain.doFilter(new XssRequestWrapper(httpRequest), response);
        } else {
            chain.doFilter(request, response);
        }
    }

    /**
     * 对参数值进行 HTML 转义。
     */
    static String escapeHtml(String value) {
        if (value == null || value.isEmpty()) {
            return value;
        }
        // 仅当包含 < > & " ' 时才转义，避免性能损失
        if (!Pattern.compile("[<>\"'&]").matcher(value).find()) {
            return value;
        }
        StringBuilder sb = new StringBuilder(value.length() + 16);
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            switch (c) {
                case '<' -> sb.append("&lt;");
                case '>' -> sb.append("&gt;");
                case '&' -> sb.append("&amp;");
                case '"' -> sb.append("&quot;");
                case '\'' -> sb.append("&#39;");
                default -> sb.append(c);
            }
        }
        return sb.toString();
    }

    /**
     * 判断 Header 是否需要转义。
     */
    private static boolean shouldSanitizeHeader(String name) {
        if (name == null) {
            return false;
        }
        String lower = name.toLowerCase();
        for (String h : SANITIZED_HEADERS) {
            if (lower.equals(h)) {
                return true;
            }
        }
        return false;
    }

    /**
     * 请求包装器：在读取参数和 Header 时做 XSS 转义。
     */
    private static class XssRequestWrapper extends HttpServletRequestWrapper {

        private Map<String, String[]> sanitizedParams;

        XssRequestWrapper(HttpServletRequest request) {
            super(request);
        }

        @Override
        public String getParameter(String name) {
            String value = super.getParameter(name);
            return escapeHtml(value);
        }

        @Override
        public Map<String, String[]> getParameterMap() {
            if (sanitizedParams == null) {
                Map<String, String[]> original = super.getParameterMap();
                Map<String, String[]> result = new LinkedHashMap<>(original.size());
                for (Map.Entry<String, String[]> entry : original.entrySet()) {
                    String[] values = entry.getValue();
                    if (values != null) {
                        String[] escaped = new String[values.length];
                        for (int i = 0; i < values.length; i++) {
                            escaped[i] = escapeHtml(values[i]);
                        }
                        result.put(entry.getKey(), escaped);
                    } else {
                        result.put(entry.getKey(), null);
                    }
                }
                sanitizedParams = result;
            }
            return sanitizedParams;
        }

        @Override
        public String[] getParameterValues(String name) {
            String[] values = super.getParameterValues(name);
            if (values == null) {
                return null;
            }
            String[] escaped = new String[values.length];
            for (int i = 0; i < values.length; i++) {
                escaped[i] = escapeHtml(values[i]);
            }
            return escaped;
        }

        @Override
        public String getHeader(String name) {
            String value = super.getHeader(name);
            if (shouldSanitizeHeader(name)) {
                return escapeHtml(value);
            }
            return value;
        }
    }
}
