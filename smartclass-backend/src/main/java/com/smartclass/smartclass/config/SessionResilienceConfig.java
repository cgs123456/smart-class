package com.smartclass.smartclass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.config.BeanPostProcessor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.session.SessionRepository;
import org.springframework.session.data.redis.RedisSessionRepository;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

/**
 * 会话仓库韧性装饰。
 *
 * <p>Spring Session 的 RedisSessionRepository.save() 在提交期发现会话的 Redis key 已消失时
 * 会抛 IllegalStateException("Session was invalidated")。该异常发生在响应序列化阶段，
 * 会打断正在写出的业务响应（含 GlobalExceptionHandler 的 JSON 输出与 /error 转发），
 * 把一次可正常返回的请求级联成 500。
 *
 * <p>触发向量与项目代码无关：Redis 实例重启丢数据、多项目共用实例时对方清库、运维删除 key 等。
 * 此处对 save() 做吞并降级——会话数据本已丢失，丢弃本次写入即可，下一个请求会自动获得全新会话。
 */
@Slf4j
@Configuration
public class SessionResilienceConfig {

    private static final String INVALIDATED_MESSAGE = "Session was invalidated";

    @Bean
    public static BeanPostProcessor sessionResilienceDecorator() {
        return new BeanPostProcessor() {
            @Override
            public Object postProcessAfterInitialization(Object bean, String beanName) {
                if (bean instanceof RedisSessionRepository repository) {
                    log.info("Session repository decorated with commit-time invalidation resilience");
                    return proxy(repository);
                }
                return bean;
            }
        };
    }

    private static SessionRepository<?> proxy(RedisSessionRepository delegate) {
        InvocationHandler handler = (Object proxy, Method method, Object[] args) -> {
            if ("save".equals(method.getName()) && args != null && args.length == 1) {
                try {
                    method.invoke(delegate, args);
                } catch (InvocationTargetException e) {
                    if (isSessionInvalidated(e.getCause())) {
                        log.warn("Session key vanished during request processing, commit dropped: {}", args[0]);
                        return null;
                    }
                    throw e;
                }
                return null;
            }
            try {
                return method.invoke(delegate, args);
            } catch (InvocationTargetException e) {
                throw e.getCause();
            }
        };
        return (SessionRepository<?>) Proxy.newProxyInstance(
                RedisSessionRepository.class.getClassLoader(),
                new Class<?>[]{SessionRepository.class},
                handler);
    }

    private static boolean isSessionInvalidated(Throwable cause) {
        return cause instanceof IllegalStateException
                && cause.getMessage() != null
                && cause.getMessage().contains(INVALIDATED_MESSAGE);
    }
}
