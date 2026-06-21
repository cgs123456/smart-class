package com.cgs.smartclass.service.impl;

import com.cgs.smartclass.model.entity.AiAvatar;
import com.cgs.smartclass.model.entity.AiAvatarChatHistory;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.ChatMessageVO;
import com.cgs.smartclass.service.AiAvatarChatHistoryService;
import com.cgs.smartclass.service.AiAvatarService;
import com.cgs.smartclass.service.DifyService;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.service.dify.DifyChatService;
import com.cgs.smartclass.service.dify.DifyConversationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.annotation.Resource;
import java.util.concurrent.CompletableFuture;

/**
 * DifyAPI服务实现（Facade模式）
 * 委托给DifyChatService、DifyConversationService、DifyFileService
 */
@Service
@Slf4j
public class DifyServiceImpl implements DifyService {

    @Resource
    private DifyChatService difyChatService;

    @Resource
    private DifyConversationService difyConversationService;

    @Resource
    private AiAvatarChatHistoryService aiAvatarChatHistoryService;

    @Resource
    private AiAvatarService aiAvatarService;

    @Resource
    private UserService userService;

    @Override
    @Transactional
    public AiAvatarChatHistory sendChatMessage(Long userId, Long aiAvatarId, String sessionId, String content,
                                               String baseUrl, String avatarAuth) {
        return difyChatService.sendChatMessage(userId, aiAvatarId, sessionId, content, baseUrl, avatarAuth);
    }

    @Override
    @Transactional
    public AiAvatarChatHistory sendChatMessageStreaming(Long userId, Long aiAvatarId, String sessionId,
                                                        String content, String baseUrl, String avatarAuth,
                                                        DifyStreamCallback callback) {
        return difyChatService.sendChatMessageStreaming(userId, aiAvatarId, sessionId, content, baseUrl, avatarAuth, callback);
    }

    @Override
    public String getSessionSummary(String sessionId, String baseUrl, String avatarAuth) {
        return difyConversationService.getSessionSummary(sessionId, baseUrl, avatarAuth);
    }

    @Override
    public boolean deleteConversation(Long userId, String sessionId, String baseUrl, String avatarAuth) {
        return difyConversationService.deleteConversation(userId, sessionId, baseUrl, avatarAuth);
    }

    @Override
    public boolean stopStreamingResponse(Long userId, String taskId, String baseUrl, String avatarAuth) {
        return difyConversationService.stopStreamingResponse(userId, taskId, baseUrl, avatarAuth);
    }

    /**
     * 处理常规消息发送请求的完整业务逻辑
     */
    @Override
    public ChatMessageVO handleSendMessageRequest(Long userId, Long aiAvatarId, String sessionId, String content,
                                               boolean endChat, AiAvatarChatHistoryService chatHistoryService,
                                               AiAvatarService aiAvatarService, UserService userService) {
        // 获取或创建会话ID
        if (org.apache.commons.lang3.StringUtils.isBlank(sessionId)) {
            sessionId = chatHistoryService.createNewSession(userId, aiAvatarId);
        }

        // 获取AI分身信息
        AiAvatar aiAvatar = aiAvatarService.getById(aiAvatarId);
        if (aiAvatar == null) {
            throw new RuntimeException("AI分身不存在");
        }

        // 验证API信息
        if (org.apache.commons.lang3.StringUtils.isAnyBlank(aiAvatar.getBaseUrl(), aiAvatar.getAvatarAuth())) {
            throw new RuntimeException("AI分身配置不完整");
        }

        // 发送消息
        AiAvatarChatHistory result = sendChatMessage(
                userId, aiAvatarId, sessionId,
                content, aiAvatar.getBaseUrl(), aiAvatar.getAvatarAuth()
        );

        // 构建响应
        ChatMessageVO chatMessageVO = new ChatMessageVO();
        BeanUtils.copyProperties(result, chatMessageVO);
        chatMessageVO.setAiAvatarName(aiAvatar.getName());
        chatMessageVO.setAiAvatarImgUrl(aiAvatar.getAvatarImgUrl());

        // 填充用户信息
        User user = userService.getById(userId);
        if (user != null) {
            chatMessageVO.setUserName(user.getUserName());
            chatMessageVO.setUserAvatar(user.getUserAvatar());
        }

        // 异步获取会话总结
        if (endChat) {
            processSessionSummary(sessionId, aiAvatar.getBaseUrl(), aiAvatar.getAvatarAuth());
        }

        return chatMessageVO;
    }

    /**
     * 处理流式消息发送请求的完整业务逻辑
     */
    @Override
    public SseEmitter handleStreamMessageRequest(Long userId, Long aiAvatarId, String sessionId, String content,
                                              AiAvatarChatHistoryService chatHistoryService, AiAvatarService aiAvatarService) {
        // 获取或创建会话ID
        if (org.apache.commons.lang3.StringUtils.isBlank(sessionId)) {
            sessionId = chatHistoryService.createNewSession(userId, aiAvatarId);
        }

        // 创建SseEmitter，超时设置为5分钟
        final SseEmitter emitter = new SseEmitter(300000L);

        try {
            // 获取AI分身信息
            AiAvatar aiAvatar = aiAvatarService.getById(aiAvatarId);
            if (aiAvatar == null || org.apache.commons.lang3.StringUtils.isAnyBlank(aiAvatar.getBaseUrl(), aiAvatar.getAvatarAuth())) {
                throw new RuntimeException("AI分身不存在或配置不完整");
            }

            // 发送初始连接事件
            sendInitialConnectEvent(emitter);

            // 处理流式消息发送
            handleStreamingMessage(emitter, userId, aiAvatarId, sessionId,
                    content, aiAvatar.getBaseUrl(), aiAvatar.getAvatarAuth());

            // 设置事件监听器
            setupEmitterListeners(emitter);

        } catch (Exception e) {
            log.error("设置流式聊天时出错: {}", e.getMessage());
            safeCompleteEmitter(emitter, e);
        }

        return emitter;
    }

    /**
     * 异步处理会话总结
     */
    private void processSessionSummary(String sessionId, String baseUrl, String avatarAuth) {
        CompletableFuture.runAsync(() -> {
            try {
                String summary = getSessionSummary(sessionId, baseUrl, avatarAuth);
                aiAvatarChatHistoryService.updateSessionSummary(sessionId, summary);
            } catch (Exception e) {
                log.error("获取会话总结失败", e);
            }
        });
    }

    /**
     * 发送初始连接事件
     */
    private void sendInitialConnectEvent(SseEmitter emitter) {
        try {
            SseEmitter.SseEventBuilder initialEvent = SseEmitter.event()
                .data("{\"event\":\"connected\",\"message\":\"SSE连接已建立\"}")
                .id("connect-" + System.currentTimeMillis())
                .name("connect");
            emitter.send(initialEvent);
        } catch (Exception e) {
            log.warn("发送初始连接事件失败，客户端可能已断开", e);
            safeCompleteEmitter(emitter, null);
            throw new RuntimeException("连接建立失败");
        }
    }

    /**
     * 处理流式消息发送
     */
    private void handleStreamingMessage(SseEmitter emitter, Long userId, Long aiAvatarId,
                                       String sessionId, String content, String baseUrl, String avatarAuth) {
        CompletableFuture.runAsync(() -> {
            try {
                sendChatMessageStreaming(
                        userId, aiAvatarId, sessionId, content, baseUrl, avatarAuth,
                        new DifyStreamCallback() {
                            @Override
                            public void onMessage(String chunk) {
                                try {
                                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                                        .data(chunk)
                                        .id(String.valueOf(System.currentTimeMillis()))
                                        .name("message");
                                    emitter.send(event);
                                } catch (Exception e) {
                                    safeCompleteEmitter(emitter, e);
                                }
                            }

                            @Override
                            public void onComplete(String fullResponse) {
                                try {
                                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                                        .data("{\"event\":\"complete\",\"message\":\"流式响应已完成\"}")
                                        .id("complete-" + System.currentTimeMillis())
                                        .name("complete");
                                    emitter.send(event);
                                    safeCompleteEmitter(emitter, null);
                                } catch (Exception e) {
                                    safeCompleteEmitter(emitter, e);
                                }
                            }

                            @Override
                            public void onError(Throwable error) {
                                try {
                                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                                        .data("{\"event\":\"error\",\"message\":\"" + error.getMessage() + "\"}")
                                        .id("error-" + System.currentTimeMillis())
                                        .name("error");
                                    emitter.send(event);
                                    safeCompleteEmitter(emitter, error);
                                } catch (Exception e) {
                                    safeCompleteEmitter(emitter, error);
                                }
                            }
                        }
                );
            } catch (Exception e) {
                log.error("流式聊天过程中出错: {}", e.getMessage());
                try {
                    SseEmitter.SseEventBuilder event = SseEmitter.event()
                        .data("{\"event\":\"error\",\"message\":\"" + e.getMessage() + "\"}")
                        .id("error-" + System.currentTimeMillis())
                        .name("error");
                    emitter.send(event);
                } catch (Exception ignored) { }
                safeCompleteEmitter(emitter, e);
            }
        });
    }

    /**
     * 设置SseEmitter的监听器
     */
    private void setupEmitterListeners(SseEmitter emitter) {
        emitter.onTimeout(() -> {
            try {
                SseEmitter.SseEventBuilder event = SseEmitter.event()
                    .data("{\"event\":\"timeout\",\"message\":\"连接超时\"}")
                    .id("timeout-" + System.currentTimeMillis())
                    .name("timeout");
                emitter.send(event);
            } catch (Exception ignored) { }
        });

        emitter.onCompletion(() -> {});
        emitter.onError(error -> {});
    }

    /**
     * 安全地完成SseEmitter，避免重复完成导致的异常
     */
    private void safeCompleteEmitter(SseEmitter emitter, Throwable error) {
        try {
            if (error != null) {
                emitter.completeWithError(error);
            } else {
                emitter.complete();
            }
        } catch (Exception e) {
            // 通常这意味着emitter已经被完成或关闭了
        }
    }
}
