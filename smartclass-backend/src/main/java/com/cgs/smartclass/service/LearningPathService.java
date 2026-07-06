package com.cgs.smartclass.service;

/**
 * 个性化学习路径推荐服务
 * 基于学生学习记录构建用户画像，注入 Dify 对话用于 AI 推荐
 */
public interface LearningPathService {

    /**
     * 构建用户学习画像文本
     *
     * @param userId 用户ID
     * @return 格式化的用户画像文本，用于注入 Dify inputs；若未启用或无数据返回空字符串
     */
    String buildUserProfile(Long userId);
}
