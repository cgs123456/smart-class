package com.smartclass.smartclassbackendserviceclient.dubbo;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;

/**
 * 每日文章 Dubbo 服务接口
 *
 * 定义内部 RPC 方法，替代原 Feign DailyArticleFeignClient
 *
 * 迁移对照:
 * - DailyArticleFeignClient.getArticleLearningSummary -> getArticleLearningSummary
 */
public interface DailyArticleDubboService {

    /**
     * 获取用户文章学习摘要
     *
     * @param userId 用户ID
     * @return 文章学习画像数据
     */
    LearningProfileDTO getArticleLearningSummary(Long userId);
}
