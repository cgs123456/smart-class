package com.cgs.smartclass.aop;

import java.lang.reflect.Field;
import java.util.UUID;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StopWatch;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 请求响应日志 AOP
 *
 * <p>对请求参数进行脱敏处理，避免敏感信息（密码、手机号、token 等）写入日志文件。</p>
 **/
@Aspect
@Component
@Slf4j
public class LogInterceptor {

    /**
     * 敏感参数关键字（命中即脱敏）。
     */
    private static final String[] SENSITIVE_KEYWORDS = {
            "password", "userPassword", "oldPassword", "newPassword", "checkPassword",
            "secret", "token", "accessToken", "refreshToken", "apiKey", "api_key",
            "phone", "userPhone", "mobile", "tel",
            "idCard", "idCardNumber", "bankCard",
            "code", "captchaCode", "verifyCode",
            "wxMpSecret", "cosSecretKey", "aesKey"
    };

    /**
     * 执行拦截
     */
    @Around("execution(* com.cgs.smartclass.controller.*.*(..))")
    public Object doInterceptor(ProceedingJoinPoint point) throws Throwable {
        // 计时
        StopWatch stopWatch = new StopWatch();
        stopWatch.start();
        // 获取请求路径
        RequestAttributes requestAttributes = RequestContextHolder.currentRequestAttributes();
        HttpServletRequest httpServletRequest = ((ServletRequestAttributes) requestAttributes).getRequest();
        // 生成请求唯一 id
        String requestId = UUID.randomUUID().toString();
        String url = httpServletRequest.getRequestURI();
        // 获取请求参数（脱敏后）
        Object[] args = point.getArgs();
        String reqParam = "[" + maskSensitiveArgs(args) + "]";
        // 输出请求日志
        log.info("request start，id: {}, path: {}, ip: {}, params: {}", requestId, url,
                httpServletRequest.getRemoteHost(), reqParam);
        // 执行原方法
        Object result = point.proceed();
        // 输出响应日志（不记录响应体，避免敏感信息泄露）
        stopWatch.stop();
        long totalTimeMillis = stopWatch.getTotalTimeMillis();
        log.info("request end, id: {}, cost: {}ms", requestId, totalTimeMillis);
        return result;
    }

    /**
     * 对参数列表进行脱敏处理。
     * <ul>
     *   <li>HttpServletRequest 等非业务对象直接以类型名显示</li>
     *   <li>String 类型按字段名判断是否需要脱敏</li>
     *   <li>DTO 对象通过反射读取字段名进行脱敏</li>
     * </ul>
     */
    private String maskSensitiveArgs(Object[] args) {
        if (args == null || args.length == 0) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(maskSensitive(args[i]));
        }
        return sb.toString();
    }

    /**
     * 脱敏单个参数。
     */
    private String maskSensitive(Object arg) {
        if (arg == null) {
            return "null";
        }
        // HttpServletRequest / HttpServletResponse 等不打印
        if (arg instanceof HttpServletRequest) {
            return "HttpServletRequest";
        }
        if (arg instanceof jakarta.servlet.http.HttpServletResponse) {
            return "HttpServletResponse";
        }
        // 字符串：当 toString 中包含敏感信息时，统一脱敏为 ******（仅做粗略保护）
        // 实际敏感字段名通常在 DTO 中，下面通过反射处理
        if (arg instanceof CharSequence) {
            return maskIfSensitive("value", arg.toString());
        }
        // POJO / DTO：通过反射读取字段名脱敏
        try {
            return maskObjectFields(arg);
        } catch (Exception e) {
            // 反射失败时退化为类型名，避免日志异常
            return arg.getClass().getSimpleName();
        }
    }

    /**
     * 对 POJO 字段做脱敏。
     */
    private String maskObjectFields(Object obj) {
        Class<?> clazz = obj.getClass();
        StringBuilder sb = new StringBuilder(clazz.getSimpleName()).append("{");
        Field[] fields = clazz.getDeclaredFields();
        for (int i = 0; i < fields.length; i++) {
            Field field = fields[i];
            if (java.lang.reflect.Modifier.isStatic(field.getModifiers())) {
                continue;
            }
            field.setAccessible(true);
            Object value;
            try {
                value = field.get(obj);
            } catch (IllegalAccessException e) {
                continue;
            }
            String strValue = value == null ? "null" : maskIfSensitive(field.getName(), String.valueOf(value));
            if (i > 0) {
                sb.append(", ");
            }
            sb.append(field.getName()).append("=").append(strValue);
        }
        sb.append("}");
        return sb.toString();
    }

    /**
     * 字段名或值命中敏感关键字时脱敏。
     */
    private String maskIfSensitive(String fieldName, String value) {
        if (value == null) {
            return "null";
        }
        String lower = fieldName.toLowerCase();
        for (String kw : SENSITIVE_KEYWORDS) {
            if (lower.contains(kw.toLowerCase())) {
                return "******";
            }
        }
        // 防止超长参数污染日志
        if (value.length() > 500) {
            return value.substring(0, 500) + "...(truncated)";
        }
        return value;
    }
}
