package com.cgs.smartclass.service;

import com.cgs.smartclass.model.entity.AiAvatar;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.ChatMessageVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;

/**
 * 任务编排服务 — 基于 CompletableFuture 实现串并行混合调度
 * 典型场景：用户请求学习推荐时，并行拉取多源数据，再串行生成最终推荐
 */
@Service
@Slf4j
public class TaskOrchestrationService {

    @Resource
    private AiAvatarChatHistoryService chatHistoryService;
    @Resource
    private AiAvatarService aiAvatarService;
    @Resource
    private UserService userService;

    // 自定义线程池，避免共用 ForkJoinPool
    private final ExecutorService executor = Executors.newFixedThreadPool(4);

    /**
     * 自定义调度器：串并行混合任务编排
     *
     * 阶段一（并行）：同时拉取用户画像、学习历史、AI分身信息
     * 阶段二（串行）：基于阶段一结果构建 Prompt → 调用 AI 生成推荐
     */
    public String orchestrateRecommendation(Long userId, Long aiAvatarId) {
        long start = System.currentTimeMillis();

        // ===== 阶段一：并行执行独立任务 =====
        CompletableFuture<User> userFuture = CompletableFuture.supplyAsync(
                () -> userService.getById(userId), executor);

        CompletableFuture<String> historyFuture = CompletableFuture.supplyAsync(
                () -> {
                    List<ChatMessageVO> messages = chatHistoryService.getUserMessages(userId, aiAvatarId);
                    if (messages != null && !messages.isEmpty()) {
                        return messages.stream()
                                .map(ChatMessageVO::getContent)
                                .collect(Collectors.joining("; "));
                    }
                    return "无历史记录";
                }, executor);

        CompletableFuture<String> avatarInfoFuture = CompletableFuture.supplyAsync(
                () -> {
                    AiAvatar avatar = aiAvatarService.getById(aiAvatarId);
                    return avatar != null ? avatar.getName() + ":" + avatar.getDescription() : "默认助手";
                }, executor);

        // 等待全部并行任务完成
        CompletableFuture.allOf(userFuture, historyFuture, avatarInfoFuture).join();

        // ===== 阶段二：串行编排（依赖阶段一结果） =====
        String result = CompletableFuture
                .supplyAsync(() -> {
                    User user = userFuture.join();
                    String history = historyFuture.join();
                    String avatarInfo = avatarInfoFuture.join();

                    // 构建推荐 Prompt
                    return buildRecommendationPrompt(user, history, avatarInfo);
                }, executor)
                .thenApply(prompt -> {
                    // 基于 Prompt 生成推荐（调 Dify 或其他推荐逻辑）
                    return generateRecommendation(prompt);
                })
                .join();

        log.info("任务编排完成，耗时: {}ms", System.currentTimeMillis() - start);
        return result;
    }

    private String buildRecommendationPrompt(User user, String history, String avatarInfo) {
        return String.format(
                "用户%s，学习历史：%s。AI分身：%s。请推荐下一步学习内容。",
                user.getUserName(), history, avatarInfo);
    }

    private String generateRecommendation(String prompt) {
        // 此处调用实际的 AI 推荐逻辑（Dify / 规则引擎 / RAG）
        return "基于编排的推荐结果: " + prompt.substring(0, Math.min(prompt.length(), 50)) + "...";
    }
}
