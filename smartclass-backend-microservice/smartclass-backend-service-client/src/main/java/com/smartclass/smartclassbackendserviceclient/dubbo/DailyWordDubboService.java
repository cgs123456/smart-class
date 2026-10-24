package com.smartclass.smartclassbackendserviceclient.dubbo;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;

/**
 * 每日单词 Dubbo 服务接口
 *
 * 定义内部 RPC 方法，替代原 Feign DailyWordFeignClient
 *
 * 迁移对照:
 * - DailyWordFeignClient.getWordLearningSummary -> getWordLearningSummary
 */
public interface DailyWordDubboService {

    /**
     * 获取用户单词学习摘要
     *
     * @param userId 用户ID
     * @return 单词学习画像数据
     */
    LearningProfileDTO getWordLearningSummary(Long userId);
}
