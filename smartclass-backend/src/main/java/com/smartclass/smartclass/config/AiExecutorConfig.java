package com.smartclass.smartclass.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

/**
 * AI/Dify 链路专用线程池。
 *
 * <p>SSE 流式读取与会话总结等任务此前跑在 ForkJoinPool.commonPool() 上：
 * 长连接读取会长期占住并行度，阻塞并行的 Stream/parallel 流与其他
 * commonPool 使用方。此线程池将 AI 外呼隔离在有界队列内，
 * 饱和时退化为调用线程执行（CallerRuns），不丢任务也不无界扩张。</p>
 */
@Configuration
@Slf4j
public class AiExecutorConfig {

    @Bean("aiStreamExecutor")
    public Executor aiStreamExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(4);
        executor.setMaxPoolSize(16);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("ai-stream-");
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.initialize();
        log.info("AI 流式线程池已初始化: core=4, max=16, queue=100");
        return executor;
    }
}
