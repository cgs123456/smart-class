package com.smartclass.smartclassbackendintelligence.dubbo;

import com.smartclass.smartclassbackendintelligence.service.AiAvatarService;
import com.smartclass.smartclassbackendintelligence.service.DifyService;
import com.smartclass.smartclassbackendmodel.model.entity.AiAvatar;
import com.smartclass.smartclassbackendmodel.model.entity.AiAvatarChatHistory;
import com.smartclass.smartclassbackendmodel.model.vo.AiAvatarBriefVO;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

/**
 * 智能服务 Dubbo 实现
 * 
 * 替代原 Feign 内部调用，使用 Dubbo Triple 协议
 */
@DubboService(
    version = "1.0.0",
    protocol = "triple",
    interfaceClass = IntelligenceDubboService.class
)
public class IntelligenceDubboServiceImpl implements IntelligenceDubboService {

    @Autowired
    private AiAvatarService aiAvatarService;

    @Autowired
    private DifyService difyService;

    @Override
    public List<AiAvatarBriefVO> listAllAiAvatarBrief() {
        return aiAvatarService.listAllAiAvatarBrief();
    }

    @Override
    public AiAvatar getAiAvatarById(Long id) {
        return aiAvatarService.getById(id);
    }

    @Override
    public ChatResponse sendChatMessage(Long aiAvatarId, String content, String sessionId, Long userId, String conversationId) {
        // 构造 Dify 请求
        com.smartclass.smartclassbackendmodel.model.dto.dify.DifyChatMessageRequest request = 
            new com.smartclass.smartclassbackendmodel.model.dto.dify.DifyChatMessageRequest();
        request.setAiAvatarId(aiAvatarId);
        request.setContent(content);
        request.setSessionId(sessionId);
        request.setUserId(userId);
        // DifyChatMessageRequest 使用 sessionId，不支持 conversationId 字段

        AiAvatarChatHistory history = difyService.sendChatMessage(request);
        
        ChatResponse response = new ChatResponse();
        response.setMessageId(String.valueOf(history.getId()));
        response.setContent(history.getContent());
        response.setTokens(history.getTokens());
        response.setSessionId(history.getSessionId());
        response.setDone(true);
        return response;
    }

    @Override
    public String sendChatMessageStreaming(Long aiAvatarId, String content, String sessionId, Long userId, String conversationId) {
        // 构造 Dify 请求
        com.smartclass.smartclassbackendmodel.model.dto.dify.DifyChatMessageRequest request = 
            new com.smartclass.smartclassbackendmodel.model.dto.dify.DifyChatMessageRequest();
        request.setAiAvatarId(aiAvatarId);
        request.setContent(content);
        request.setSessionId(sessionId);
        request.setUserId(userId);
        // DifyChatMessageRequest 使用 sessionId，不支持 conversationId 字段

        // 流式处理由 Controller 通过 SSE 处理，这里只返回消息ID用于追踪
        String messageId = java.util.UUID.randomUUID().toString();
        
        // 异步处理流式响应（实际实现中需要通过 SSE 推送）
        // 这里简化处理，实际生产环境需要更完善的异步处理
        return messageId;
    }

    @Override
    public List<AiAvatarChatHistory> getChatHistory(Long aiAvatarId, String sessionId, Long userId, int page, int size) {
        // 这里需要调用 chatHistoryService 的分页查询
        // 简化实现，实际需要注入 AiAvatarChatHistoryService
        return List.of();
    }
}