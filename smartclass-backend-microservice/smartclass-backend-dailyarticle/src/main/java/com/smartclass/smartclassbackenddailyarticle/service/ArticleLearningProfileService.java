package com.smartclass.smartclassbackenddailyarticle.service;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;

/**
 * 文章学习画像服务
 * 供内部接口（Feign）与 Dubbo 服务共用，避免逻辑重复。
 */
public interface ArticleLearningProfileService {

    /**
     * 获取用户文章学习摘要
     *
     * @param userId 用户ID
     * @return 文章学习画像数据
     */
    LearningProfileDTO getArticleLearningSummary(Long userId);
}
