package com.cgs.smartclass.service.dify;

/**
 * Dify会话管理服务接口
 * 负责会话的创建、获取、删除、总结等操作
 */
public interface DifyConversationService {

    /**
     * 获取会话总结
     *
     * @param sessionId    会话ID
     * @param baseUrl      Dify API基础URL
     * @param avatarAuth   AI分身授权token
     * @return             会话总结内容
     */
    String getSessionSummary(String sessionId, String baseUrl, String avatarAuth);

    /**
     * 删除Dify会话
     *
     * @param userId       用户ID
     * @param sessionId    会话ID
     * @param baseUrl      Dify API基础URL
     * @param avatarAuth   AI分身授权token
     * @return             是否删除成功
     */
    boolean deleteConversation(Long userId, String sessionId, String baseUrl, String avatarAuth);

    /**
     * 停止流式响应
     *
     * @param userId       用户ID
     * @param taskId       任务ID
     * @param baseUrl      Dify API基础URL
     * @param avatarAuth   AI分身授权token
     * @return             是否成功
     */
    boolean stopStreamingResponse(Long userId, String taskId, String baseUrl, String avatarAuth);
}
