package com.smartclass.smartclass.config;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

/**
 * 业务指标收集器
 * 用于收集 AI 聊天、API 调用等业务指标
 *
 * <p>Micrometer 的 meter 按 name + tags 唯一标识，动态 tag 需要在构建时传入；
 * 运行时通过 registry 按 name + tags 查找（框架内部自动去重复用），因此
 * 带 tag 的方法直接在调用时构建/查找 meter。</p>
 */
@Component
public class BusinessMetrics {

    private final MeterRegistry registry;

    public BusinessMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void incrementAiChatRequests(String status) {
        Counter.builder("ai.chat.requests.total")
                .description("AI 聊天请求总数")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public void incrementAiChatErrors(String errorType) {
        Counter.builder("ai.chat.errors.total")
                .description("AI 聊天错误总数")
                .tag("error_type", errorType)
                .register(registry)
                .increment();
    }

    public Timer.Sample startAiChatTimer() {
        return Timer.start(registry);
    }

    public void stopAiChatTimer(Timer.Sample sample, String status) {
        sample.stop(aiChatLatency(status));
    }

    public void incrementDifyApiRequests(String endpoint, String status) {
        Counter.builder("dify.api.requests.total")
                .description("Dify API 调用总数")
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public Timer.Sample startDifyApiTimer() {
        return Timer.start(registry);
    }

    public void stopDifyApiTimer(Timer.Sample sample, String endpoint, String status) {
        sample.stop(difyApiLatency(endpoint, status));
    }

    public void incrementRagSearch(String status) {
        Counter.builder("rag.search.total")
                .description("RAG 搜索总数")
                .tag("status", status)
                .register(registry)
                .increment();
    }

    public Timer.Sample startRagSearchTimer() {
        return Timer.start(registry);
    }

    public void stopRagSearchTimer(Timer.Sample sample, String status) {
        sample.stop(ragSearchLatency(status));
    }

    private Timer aiChatLatency(String status) {
        return Timer.builder("ai.chat.latency")
                .description("AI 聊天延迟")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("status", status)
                .register(registry);
    }

    private Timer difyApiLatency(String endpoint, String status) {
        return Timer.builder("dify.api.latency")
                .description("Dify API 调用延迟")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("endpoint", endpoint)
                .tag("status", status)
                .register(registry);
    }

    private Timer ragSearchLatency(String status) {
        return Timer.builder("rag.search.latency")
                .description("RAG 搜索延迟")
                .publishPercentiles(0.5, 0.95, 0.99)
                .tag("status", status)
                .register(registry);
    }
}
