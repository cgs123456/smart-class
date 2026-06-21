package com.cgs.smartclass.service.dify;

import com.cgs.smartclass.model.entity.AiAvatarChatHistory;
import com.cgs.smartclass.service.DifyService;

/**
 * Dify聊天服务接口
 * 负责聊天消息的发送（阻塞式和流式）
 */
public interface DifyChatService {

    /**
     * 发送聊天消息并获取回复（阻塞式）
     *
     * @param userId       用户ID
     * @param aiAvatarId   AI分身ID
     * @param sessionId    会话ID
     * @param content      发送的消息内容
     * @param baseUrl      Dify API基础URL
     * @param avatarAuth   AI分身授权token
     * @return             保存的消息记录
     */
    AiAvatarChatHistory sendChatMessage(Long userId, Long aiAvatarId, String sessionId, String content,
                                        String baseUrl, String avatarAuth);

    /**
     * 发送聊天消息流式处理
     *
     * @param userId       用户ID
     * @param aiAvatarId   AI分身ID
     * @param sessionId    会话ID
     * @param content      发送的消息内容
     * @param baseUrl      Dify API基础URL
     * @param avatarAuth   AI分身授权token
     * @param callback     处理响应块的回调函数
     * @return             保存的消息记录
     */
    AiAvatarChatHistory sendChatMessageStreaming(Long userId, Long aiAvatarId, String sessionId, String content,
                                                 String baseUrl, String avatarAuth, DifyService.DifyStreamCallback callback);
}
