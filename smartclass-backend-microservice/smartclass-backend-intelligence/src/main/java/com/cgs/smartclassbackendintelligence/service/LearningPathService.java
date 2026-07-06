package com.cgs.smartclassbackendintelligence.service;

/**
 * 学习路径推荐服务
 * 基于用户学习记录构建用户画像，供 AI 个性化推荐"接下来学什么"
 */
public interface LearningPathService {

    /**
     * 构建用户学习画像文本
     * 聚合各微服务的学习记录，拼接为格式化文本（控制在 1500 字符以内），
     * 用于注入到 Dify 对话的 inputs.user_profile 字段
     *
     * @param userId 用户ID
     * @return 用户画像文本；若全部数据获取失败则返回空字符串
     */
    String buildUserProfile(Long userId);
}
