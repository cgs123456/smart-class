package com.smartclass.smartclassbackendcourse.service;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;

/**
 * 课程学习画像服务
 * 统计用户收藏课程数、课程评价数及平均评分，并返回收藏课程信息。
 * 供内部接口（Feign）与 Dubbo 服务共用，避免逻辑重复。
 */
public interface CourseLearningProfileService {

    /**
     * 获取用户课程学习摘要
     *
     * @param userId 用户ID
     * @return 课程学习画像数据
     */
    LearningProfileDTO getCourseLearningSummary(Long userId);
}
