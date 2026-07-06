package com.cgs.smartclass.service.dify;

import com.cgs.smartclass.config.DifyConfig;
import com.cgs.smartclass.model.dto.dify.DifyChatRequest;
import com.cgs.smartclass.model.dto.dify.DifyChatResponse;
import com.cgs.smartclass.model.dto.dify.DifyStreamChunk;
import com.cgs.smartclass.model.entity.AiAvatarChatHistory;
import com.cgs.smartclass.service.AiAvatarChatHistoryService;
import com.cgs.smartclass.service.CourseRagService;
import com.cgs.smartclass.service.DifyService;
import com.cgs.smartclass.service.LearningPathService;
import com.cgs.smartclass.utils.ChatMessageHelper;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Dify聊天服务实现
 * 负责聊天消息的发送（阻塞式和流式）
 */
@Service
@Slf4j
public class DifyChatServiceImpl implements DifyChatService {

    @Resource
    private DifyConfig difyConfig;

    @Resource
    private AiAvatarChatHistoryService aiAvatarChatHistoryService;

    @Resource
    private ChatMessageHelper chatMessageHelper;

    @Resource
    private DifyHttpHelper difyHttpHelper;

    @Autowired(required = false)
    private CourseRagService courseRagService;

    @Autowired(required = false)
    private LearningPathService learningPathService;

    @Override
    @Transactional
    public AiAvatarChatHistory sendChatMessage(Long userId, Long aiAvatarId, String sessionId, String content,
                                               String baseUrl, String avatarAuth) {
        // 创建用户消息对象
        AiAvatarChatHistory userMessage = chatMessageHelper.createUserMessage(userId, aiAvatarId, sessionId, content);

        // 保存用户消息
        boolean saved = aiAvatarChatHistoryService.save(userMessage);
        if (!saved) {
            log.error("Failed to save user message");
            throw new RuntimeException("保存用户消息失败");
        }

        try {
            return sendChatMessageWithRetry(userId, aiAvatarId, sessionId, content, baseUrl, avatarAuth, userMessage, false);
        } catch (Exception e) {
            log.error("Error sending chat message to Dify", e);
            throw new RuntimeException("发送聊天消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送聊天消息并处理会话不存在的情况，使用 OkHttp 实现
     */
    private AiAvatarChatHistory sendChatMessageWithRetry(Long userId, Long aiAvatarId, String sessionId,
                                                         String content, String baseUrl, String avatarAuth,
                                                         AiAvatarChatHistory userMessage, boolean retried) {
        try {
            // 构建请求对象
            DifyChatRequest chatRequest = buildChatRequest(userId, sessionId, content);

            // 如果是重试并且会话不存在，则不传会话ID
            if (retried) {
                chatRequest.setConversation_id(null);
            }

            // 发送请求
            String chatMessagesPath = "/chat-messages";
            String url = baseUrl + chatMessagesPath;
            String requestJson = JSONUtil.toJsonStr(chatRequest);

            // 添加请求头
            Map<String, String> headers = difyHttpHelper.buildAuthHeaders(avatarAuth);

            // 使用 OkHttp 发送请求
            Response response = difyHttpHelper.postJson(url, requestJson, headers);

            try {
                if (!response.isSuccessful()) {
                    String responseBody = difyHttpHelper.readResponseBody(response);

                    log.error("Dify API error: {}, Headers: {}, Body: {}",
                            response.code(), response.headers(), responseBody);

                    // 检查是否为会话不存在的错误
                    if (response.code() == 404 && !retried) {
                        if (responseBody.contains("Conversation Not Exists")) {
                            // 递归调用，但设置retried标志，不传会话ID
                            return sendChatMessageWithRetry(userId, aiAvatarId, null, content, baseUrl, avatarAuth, userMessage, true);
                        }
                    }

                    throw new RuntimeException("调用Dify API失败: " + response.code() + ", " + responseBody);
                }

                // 解析响应
                String responseBody = difyHttpHelper.readResponseBody(response);

                DifyChatResponse chatResponse = JSONUtil.toBean(responseBody, DifyChatResponse.class);

                // 检查是否返回了新的会话ID，如果有且与原会话ID不同，则更新会话记录
                if (chatResponse.getConversation_id() != null && !chatResponse.getConversation_id().equals(sessionId)) {
                    log.info("Dify创建了新会话ID: {}, 原会话ID: {}", chatResponse.getConversation_id(), sessionId);

                    // 更新用户消息的会话ID
                    AiAvatarChatHistory updatedUserMessage = new AiAvatarChatHistory();
                    updatedUserMessage.setId(userMessage.getId());
                    updatedUserMessage.setSessionId(chatResponse.getConversation_id());
                    aiAvatarChatHistoryService.updateById(updatedUserMessage);

                    // 更新会话ID
                    sessionId = chatResponse.getConversation_id();
                }

                // 创建并保存AI响应
                AiAvatarChatHistory aiResponse = chatMessageHelper.createAiResponse(userId, aiAvatarId, sessionId, chatResponse);
                boolean saved = aiAvatarChatHistoryService.save(aiResponse);
                if (!saved) {
                    log.error("Failed to save AI response");
                    throw new RuntimeException("保存AI响应失败");
                }

                return aiResponse;
            } finally {
                response.close();
            }

        } catch (Exception e) {
            if (!retried && (e.getMessage().contains("Conversation Not Exists") || e.getMessage().contains("404"))) {
                // 递归调用，但设置retried标志
                return sendChatMessageWithRetry(userId, aiAvatarId, null, content, baseUrl, avatarAuth, userMessage, true);
            }
            throw e;
        }
    }

    @Override
    @Transactional
    public AiAvatarChatHistory sendChatMessageStreaming(Long userId, Long aiAvatarId, String sessionId,
                                                        String content, String baseUrl, String avatarAuth,
                                                        DifyService.DifyStreamCallback callback) {
        // 创建用户消息对象
        AiAvatarChatHistory userMessage = chatMessageHelper.createUserMessage(userId, aiAvatarId, sessionId, content);

        // 保存用户消息
        boolean saved = aiAvatarChatHistoryService.save(userMessage);
        if (!saved) {
            log.error("Failed to save user message");
            throw new RuntimeException("保存用户消息失败");
        }

        // 用于存储完整响应
        final AtomicReference<String> fullResponseRef = new AtomicReference<>("");
        final AtomicReference<String> messageIdRef = new AtomicReference<>("");
        final AtomicReference<String> conversationIdRef = new AtomicReference<>(sessionId);

        try {
            return sendStreamingWithRetry(userId, aiAvatarId, sessionId, content, baseUrl, avatarAuth,
                    callback, userMessage, fullResponseRef, messageIdRef, conversationIdRef, false);
        } catch (Exception e) {
            log.error("Error sending streaming chat message to Dify", e);
            callback.onError(e);
            throw new RuntimeException("发送流式聊天消息失败: " + e.getMessage());
        }
    }

    /**
     * 发送流式消息并处理会话不存在的情况，使用 OkHttp 实现
     */
    private AiAvatarChatHistory sendStreamingWithRetry(Long userId, Long aiAvatarId, String sessionId,
                                                       String content, String baseUrl, String avatarAuth,
                                                       DifyService.DifyStreamCallback callback,
                                                       AiAvatarChatHistory userMessage,
                                                       AtomicReference<String> fullResponseRef,
                                                       AtomicReference<String> messageIdRef,
                                                       AtomicReference<String> conversationIdRef,
                                                       boolean retried) throws IOException {
        try {
            // 构建请求对象
            DifyChatRequest chatRequest = buildChatRequest(userId, sessionId, content);
            chatRequest.setResponse_mode("streaming"); // 强制使用流式模式

            // 如果是重试并且会话不存在，则不传会话ID
            if (retried) {
                chatRequest.setConversation_id(null);
            }

            // 发送请求
            String chatMessagesPath = "/chat-messages";
            String url = baseUrl + chatMessagesPath;
            String requestJson = JSONUtil.toJsonStr(chatRequest);

            // 添加请求头
            Map<String, String> headers = difyHttpHelper.buildStreamAuthHeaders(avatarAuth);

            // 使用 OkHttp 发送流式请求
            Response response = difyHttpHelper.postJsonStream(url, requestJson, headers);

            try {
                if (!response.isSuccessful()) {
                    String responseBody = "";
                    try (okhttp3.ResponseBody body = response.body()) {
                        if (body != null) {
                            responseBody = body.string();
                        }
                    }

                    log.error("Dify API error: {}, Headers: {}, Body: {}",
                            response.code(), response.headers(), responseBody);

                    // 检查是否为会话不存在的错误
                    if (response.code() == 404 && !retried) {
                        if (responseBody.contains("Conversation Not Exists")) {
                            // 关闭响应
                            response.close();
                            // 递归调用，但设置retried标志，不传会话ID
                            return sendStreamingWithRetry(userId, aiAvatarId, null, content, baseUrl, avatarAuth,
                                    callback, userMessage, fullResponseRef, messageIdRef, conversationIdRef, true);
                        }
                    }

                    callback.onError(new RuntimeException("调用Dify API失败: " + response.code() + ", " + responseBody));
                    throw new RuntimeException("调用Dify API失败: " + response.code() + ", " + responseBody);
                }

                // 获取响应体
                final okhttp3.ResponseBody responseBody = response.body();
                if (responseBody == null) {
                    callback.onError(new RuntimeException("Dify API返回空响应"));
                    throw new RuntimeException("Dify API返回空响应");
                }

                // 使用CompletableFuture异步处理流式响应
                CompletableFuture.runAsync(() -> {
                    // 使用BufferedReader逐行读取SSE流
                    try (BufferedReader reader = new BufferedReader(new InputStreamReader(responseBody.byteStream()))) {
                        String line;

                        try {
                            while ((line = reader.readLine()) != null) {
                                // 处理SSE数据行
                                if (line.startsWith("data: ")) {
                                    // 提取JSON数据内容
                                    String jsonData = line.substring(6); // 移除 "data: " 前缀

                                    // 立即将数据传递给回调，不等待整个事件结束
                                    if (!jsonData.isEmpty()) {
                                        try {
                                            // 首先发送给回调，确保前端立即收到
                                            callback.onMessage(jsonData);

                                            // 解析数据进行额外处理
                                            try {
                                                DifyStreamChunk chunk = JSONUtil.toBean(jsonData, DifyStreamChunk.class);

                                                // 处理不同类型的事件
                                                if ("message".equals(chunk.getEvent()) && chunk.getAnswer() != null) {
                                                    // 累积完整响应
                                                    fullResponseRef.updateAndGet(prev -> prev + chunk.getAnswer());

                                                    // 保存消息ID
                                                    if (messageIdRef.get().isEmpty() && chunk.getId() != null) {
                                                        messageIdRef.set(chunk.getId());
                                                    }

                                                    // 保存会话ID
                                                    if (chunk.getConversation_id() != null) {
                                                        conversationIdRef.set(chunk.getConversation_id());
                                                    }
                                                } else if ("message_end".equals(chunk.getEvent())) {
                                                    // 消息结束事件，记录但不做特殊处理
                                                } else if ("error".equals(chunk.getEvent())) {
                                                    // 错误事件
                                                } else if ("ping".equals(chunk.getEvent())) {
                                                    // ping事件，用于保持连接活跃
                                                }
                                            } catch (Exception e) {
                                                // 解析异常，保留异常处理逻辑
                                            }
                                        } catch (Exception e) {
                                            // 回调异常，保留异常处理逻辑
                                        }
                                    }
                                } else if (line.trim().isEmpty()) {
                                    // 空行，忽略
                                    continue;
                                }
                            }
                        } catch (IOException e) {
                            log.error("读取流式数据IO异常");
                            callback.onError(e);
                        }

                        // 流结束，处理最终工作
                        String fullResponse = fullResponseRef.get();
                        String finalConversationId = conversationIdRef.get();

                        // 检查会话ID是否有变化
                        if (!finalConversationId.equals(sessionId)) {
                            // 更新用户消息的会话ID
                            AiAvatarChatHistory updatedUserMessage = new AiAvatarChatHistory();
                            updatedUserMessage.setId(userMessage.getId());
                            updatedUserMessage.setSessionId(finalConversationId);
                            aiAvatarChatHistoryService.updateById(updatedUserMessage);
                        }

                        // 保存到数据库
                        if (!fullResponse.isEmpty()) {
                            boolean savedResult = aiAvatarChatHistoryService.saveMessage(
                                    userId, aiAvatarId, finalConversationId, "ai", fullResponse);

                            if (!savedResult) {
                                log.error("Failed to save AI response to database");
                            }
                        }

                        // 处理完成通知回调
                        callback.onComplete(fullResponse);

                    } catch (IOException e) {
                        log.error("处理流式响应异常");
                        callback.onError(e);
                    } finally {
                        try {
                            responseBody.close();
                        } catch (Exception e) {
                            log.error("关闭响应体异常");
                        }
                    }
                });

                // 创建一个消息对象返回，实际内容会被异步更新
                AiAvatarChatHistory aiResponse = chatMessageHelper.createEmptyAiResponse(userId, aiAvatarId, sessionId);
                return aiResponse;

            } catch (Exception e) {
                if (response != null) {
                    response.close();
                }
                throw e;
            }

        } catch (Exception e) {
            if (!retried && (e.getMessage().contains("Conversation Not Exists") || e.getMessage().contains("404"))) {
                // 递归调用，但设置retried标志
                return sendStreamingWithRetry(userId, aiAvatarId, null, content, baseUrl, avatarAuth,
                        callback, userMessage, fullResponseRef, messageIdRef, conversationIdRef, true);
            }
            throw e;
        }
    }

    /**
     * 构建聊天请求对象
     */
    private DifyChatRequest buildChatRequest(Long userId, String sessionId, String content) {
        DifyChatRequest chatRequest = new DifyChatRequest();
        chatRequest.setQuery(content);
        Map<String, Object> inputs = new HashMap<>();
        if (courseRagService != null) {
            String ragContext = courseRagService.retrieveRelevantContent(content, 3);
            if (StrUtil.isNotBlank(ragContext)) {
                inputs.put("course_context", ragContext);
            }
        }
        // 用户学习画像注入，供 Dify AI 基于画像推荐"接下来学什么"
        if (learningPathService != null) {
            String userProfile = learningPathService.buildUserProfile(userId);
            if (StrUtil.isNotBlank(userProfile)) {
                inputs.put("user_profile", userProfile);
            }
        }
        chatRequest.setInputs(inputs);
        chatRequest.setResponse_mode("blocking");
        chatRequest.setUser(difyConfig.getUserPrefix() + userId);

        // 验证会话ID格式，如果不是UUID则设为null让Dify自动创建
        if (sessionId != null && isValidUUID(sessionId)) {
            chatRequest.setConversation_id(sessionId);
        } else {
            chatRequest.setConversation_id(null);
        }

        chatRequest.setAuto_generate_name(true);
        return chatRequest;
    }

    /**
     * 验证字符串是否为有效的UUID
     */
    private boolean isValidUUID(String uuidStr) {
        try {
            UUID.fromString(uuidStr);
            return true;
        } catch (IllegalArgumentException e) {
            return false;
        }
    }
}
