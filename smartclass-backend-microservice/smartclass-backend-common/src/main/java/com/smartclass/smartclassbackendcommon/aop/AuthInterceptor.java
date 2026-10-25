package com.smartclass.smartclassbackendcommon.aop;

import com.smartclass.smartclassbackendcommon.annotation.AuthCheck;
import com.smartclass.smartclassbackendcommon.common.ErrorCode;
import com.smartclass.smartclassbackendcommon.constant.UserConstant;
import com.smartclass.smartclassbackendcommon.exception.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.ProceedingJoinPoint;
import org.springframework.beans.factory.annotation.Value;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * {@code @AuthCheck} 注解 AOP 切面实现
 *
 * <p>职责：</p>
 * <ul>
 *   <li>读取方法上 {@link AuthCheck#mustRole()} 指定的角色要求</li>
 *   <li>从 Session / {@code X-User-Role} 头中获取当前用户角色</li>
 *   <li>角色不匹配时抛出 {@link BusinessException}(NO_AUTH_ERROR)</li>
 * </ul>
 *
 * <p>common 模块不直接依赖 model 模块，因此 Session 中的用户对象通过反射读取 userRole 字段，
 * 避免在 common 中引入 model 依赖。</p>
 *
 * <p>兼容两种调用方式：</p>
 * <ol>
 *   <li>网关已校验 JWT，把用户信息注入到 {@code X-User-Id} / {@code X-User-Role} 请求头</li>
 *   <li>服务内部调用 / 直连场景，从 Session 中读取登录用户</li>
 * </ol>
 */
@Aspect
public class AuthInterceptor {

    private static final Logger log = LoggerFactory.getLogger(AuthInterceptor.class);

    /**
     * 网关注入的用户 ID 头。
     */
    private static final String HEADER_USER_ID = "X-User-Id";

    /**
     * 网关注入的用户角色头。
     */
    private static final String HEADER_USER_ROLE = "X-User-Role";

    /**
     * 内部调用凭证头：X-Internal-Call 只有携带匹配令牌才被采信，
     * 防止外部直连服务端口伪造内部头绕过鉴权。
     * 未配置 INTERNAL_CALL_TOKEN 时一律不信任（fail-closed）。
     */
    private static final String HEADER_INTERNAL_TOKEN = "X-Internal-Token";

    @Value("${INTERNAL_CALL_TOKEN:}")
    private String internalCallToken;

    /**
     * 拦截所有标注 {@link AuthCheck} 的方法。
     */
    @Around("@annotation(com.smartclass.smartclassbackendcommon.annotation.AuthCheck)")
    public Object doAuthCheck(ProceedingJoinPoint point) throws Throwable {
        MethodSignature signature = (MethodSignature) point.getSignature();
        Method method = signature.getMethod();
        AuthCheck authCheck = method.getAnnotation(AuthCheck.class);
        if (authCheck == null) {
            return point.proceed();
        }
        String mustRole = authCheck.mustRole();
        // 无角色要求，直接放行
        if (StringUtils.isBlank(mustRole)) {
            return point.proceed();
        }

        // 获取当前请求
        HttpServletRequest request = currentRequest();

        // 内部调用（Feign / 网关）跳过角色校验，但必须携带共享令牌
        if (isTrustedInternalCall(request)) {
            return point.proceed();
        }

        // 解析当前用户角色：优先从 Session 取，其次从网关注入的请求头取
        String currentUserRole = resolveUserRole(request);

        if (StringUtils.isBlank(currentUserRole)) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR, "未登录");
        }
        // 被封号用户禁止任何操作
        if (UserConstant.BAN_ROLE.equalsIgnoreCase(currentUserRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "账号已被封禁");
        }
        // 管理员拥有所有权限
        if (UserConstant.ADMIN_ROLE.equalsIgnoreCase(currentUserRole)) {
            return point.proceed();
        }
        // 角色不匹配
        if (!mustRole.equalsIgnoreCase(currentUserRole)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限");
        }
        return point.proceed();
    }

    /**
     * 获取当前 HTTP 请求（不存在时返回 null，例如异步线程或定时任务）。
     */
    private HttpServletRequest currentRequest() {
        RequestAttributes attrs = RequestContextHolder.getRequestAttributes();
        if (attrs instanceof ServletRequestAttributes servletAttrs) {
            return servletAttrs.getRequest();
        }
        return null;
    }

    /**
     * 解析当前用户角色。
     *
     * <p>优先从 Session 中读取（兼容已有 Session 登录流程），
     * 由于 common 不依赖 model，User 实体的 userRole 通过反射读取。</p>
     */
    private String resolveUserRole(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        Object sessionUser = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        if (sessionUser != null) {
            String role = readUserRoleByReflection(sessionUser);
            if (StringUtils.isNotBlank(role)) {
                return role;
            }
        }
        // 退化为网关注入的请求头（仅信任携带内部令牌的请求）
        if (isTrustedInternalCall(request)) {
            return request.getHeader(HEADER_USER_ROLE);
        }
        return null;
    }

    /**
     * 校验内部调用凭证：X-Internal-Token 与配置令牌常量时间比较。
     * 未配置令牌时一律返回 false（fail-closed，内部跳过鉴权能力整体关闭）。
     */
    private boolean isTrustedInternalCall(HttpServletRequest request) {
        if (request == null || StringUtils.isBlank(internalCallToken)) {
            return false;
        }
        String token = request.getHeader(HEADER_INTERNAL_TOKEN);
        return token != null && MessageDigest.isEqual(
                internalCallToken.getBytes(StandardCharsets.UTF_8),
                token.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * 通过反射读取对象的 getUserRole() 方法返回值。
     */
    private String readUserRoleByReflection(Object user) {
        try {
            Method getUserRole = user.getClass().getMethod("getUserRole");
            Object role = getUserRole.invoke(user);
            return role == null ? null : String.valueOf(role);
        } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException e) {
            log.debug("无法通过反射读取 userRole: {}", e.getMessage());
            return null;
        }
    }
}

