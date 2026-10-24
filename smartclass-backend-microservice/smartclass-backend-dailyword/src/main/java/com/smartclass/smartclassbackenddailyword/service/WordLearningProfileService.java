package com.smartclass.smartclassbackenddailyword.service;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;

/**
 * 单词学习画像服务
 * 供内部接口（Feign）与 Dubbo 服务共用，避免逻辑重复。
 */
public interface WordLearningProfileService {

    /**
     * 获取用户单词学习摘要
     *
     * @param userId 用户ID
     * @return 单词学习画像数据
     */
    LearningProfileDTO getWordLearningSummary(Long userId);
}
