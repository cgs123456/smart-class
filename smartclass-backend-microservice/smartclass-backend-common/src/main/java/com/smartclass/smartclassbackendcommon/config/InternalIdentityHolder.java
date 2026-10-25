package com.smartclass.smartclassbackendcommon.config;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Value;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 网关身份头采信判定（静态入口）。
 *
 * <p>网关在 JWT 校验通过后向下游注入 X-User-Id / X-User-Role / X-Internal-Token。
 * 下游服务（含 service-client 的静态工具类）必须在共享内部令牌匹配时才采信这些头，
 * 否则外部请求可直接伪造 X-User-Id 绕过鉴权。</p>
 *
 * <p>common 包的 bean 不经组件扫描，须在
 * {@link SmartclassCommonAutoConfiguration} 中注册本类。</p>
 */
public class InternalIdentityHolder {

    private static volatile String internalCallToken;

    @Value("${INTERNAL_CALL_TOKEN:}")
    void setInternalCallToken(String token) {
        internalCallToken = token;
    }

    /**
     * 仅当请求携带与配置一致的 X-Internal-Token 时，返回网关注入的 X-User-Id；
     * 否则一律返回 null（不可信身份）。
     */
    public static Long resolveTrustedUserId(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        String expected = internalCallToken;
        String presented = request.getHeader("X-Internal-Token");
        if (expected == null || expected.isEmpty() || presented == null || presented.isEmpty()) {
            return null;
        }
        boolean trusted = MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                presented.getBytes(StandardCharsets.UTF_8));
        if (!trusted) {
            return null;
        }
        String id = request.getHeader("X-User-Id");
        if (id == null || id.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(id.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
