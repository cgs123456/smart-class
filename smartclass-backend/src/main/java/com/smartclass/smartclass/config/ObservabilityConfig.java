package com.smartclass.smartclass.config;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.binder.jvm.JvmMemoryMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmGcMetrics;
import io.micrometer.core.instrument.binder.jvm.JvmThreadMetrics;
import io.micrometer.core.instrument.binder.system.ProcessorMetrics;
import io.micrometer.core.instrument.binder.system.FileDescriptorMetrics;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 可观测性配置
 * 配置 Micrometer 指标、JVM 指标、系统指标
 */
@Configuration
public class ObservabilityConfig {

    @Value("${spring.application.name:smartclass}")
    private String applicationName;

    /**
     * 配置通用标签
     */
    @Bean
    public MeterRegistry meterRegistry(MeterRegistry registry) {
        registry.config()
                .commonTags("application", applicationName)
                .commonTags("environment", System.getenv().getOrDefault("ENVIRONMENT", "development"));
        return registry;
    }

    /**
     * JVM 内存指标
     */
    @Bean
    public JvmMemoryMetrics jvmMemoryMetrics(MeterRegistry registry) {
        return new JvmMemoryMetrics();
    }

    /**
     * JVM GC 指标
     */
    @Bean
    public JvmGcMetrics jvmGcMetrics(MeterRegistry registry) {
        return new JvmGcMetrics();
    }

    /**
     * JVM 线程指标
     */
    @Bean
    public JvmThreadMetrics jvmThreadMetrics(MeterRegistry registry) {
        return new JvmThreadMetrics();
    }

    /**
     * CPU 处理器指标
     */
    @Bean
    public ProcessorMetrics processorMetrics(MeterRegistry registry) {
        return new ProcessorMetrics();
    }

    /**
     * 文件描述符指标
     */
    @Bean
    public FileDescriptorMetrics fileDescriptorMetrics(MeterRegistry registry) {
        return new FileDescriptorMetrics();
    }
}