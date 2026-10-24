package com.smartclass.smartclassbackendintelligence.dubbo;

import com.smartclass.smartclassbackendmodel.model.entity.AiAvatar;
import com.smartclass.smartclassbackendmodel.model.entity.AiAvatarChatHistory;
import com.smartclass.smartclassbackendmodel.model.vo.AiAvatarBriefVO;

import java.util.List;

/**
 * 智能服务 Dubbo 接口
 * 
 * 替代原 Feign 内部调用，使用 Dubbo Triple 协议
 */
public interface IntelligenceDubboService {

    /**
     * 获取 AI 分身列表
     *
     * @return AI 分身简要信息列表
     */
    List<AiAvatarBriefVO> listAllAiAvatarBrief();

    /**
     * 根据ID获取 AI 分身详情
     *
     * @param id AI 分身ID
     * @return AI 分身详情
     */
    AiAvatar getAiAvatarById(Long id);

    /**
     * 发送聊天消息（非流式）
     *
     * @param aiAvatarId   AI 分身ID
     * @param content      消息内容
     * @param sessionId    会话ID
     * @param userId       用户ID
     * @param conversationId 对话ID
     * @return 聊天响应
     */
    ChatResponse sendChatMessage(Long aiAvatarId, String content, String sessionId, Long userId, String conversationId);

    /**
     * 发送聊天消息（流式） - 返回消息ID，实际流式通过 SSE 推送
     *
     * @param aiAvatarId   AI 分身ID
     * @param content      消息内容
     * @param sessionId    会话ID
     * @param userId       用户ID
     * @param conversationId 对话ID
     * @return 消息ID
     */
    String sendChatMessageStreaming(Long aiAvatarId, String content, String sessionId, Long userId, String conversationId);

    /**
     * 获取聊天历史
     *
     * @param aiAvatarId AI 分身ID
     * @param sessionId  会话ID
     * @param userId     用户ID
     * @param page       页码
     * @param size       每页大小
     * @return 聊天历史列表
     */
    List<AiAvatarChatHistory> getChatHistory(Long aiAvatarId, String sessionId, Long userId, int page, int size);

    /**
     * 聊天响应
     */
    class ChatResponse {
        private String messageId;
        private String content;
        private int tokens;
        private String sessionId;
        private boolean done;

        public ChatResponse() {}

        public ChatResponse(String messageId, String content, int tokens, String sessionId, boolean done) {
            this.messageId = messageId;
            this.content = content;
            this.tokens = tokens;
            this.sessionId = sessionId;
            this.done = done;
        }

        public String getMessageId() { return messageId; }
        public void setMessageId(String messageId) { this.messageId = messageId; }
        public String getContent() { return content; }
        public void setContent(String content) { this.content = content; }
        public int getTokens() { return tokens; }
        public void setTokens(int tokens) { this.tokens = tokens; }
        public String getSessionId() { return sessionId; }
        public void setSessionId(String sessionId) { this.sessionId = sessionId; }
        public boolean isDone() { return done; }
        public void setDone(boolean done) { this.done = done; }
    }
}