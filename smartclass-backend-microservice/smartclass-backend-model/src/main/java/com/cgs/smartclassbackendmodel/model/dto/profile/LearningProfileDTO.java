package com.cgs.smartclassbackendmodel.model.dto.profile;

import lombok.Data;

import java.io.Serializable;
import java.util.List;

/**
 * 用户学习画像 DTO
 * 用于聚合各微服务的学习记录摘要，供 AI 个性化推荐使用
 */
@Data
public class LearningProfileDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    // ========== 文章统计 ==========
    /**
     * 文章总数（已记录的）
     */
    private int articleTotal;

    /**
     * 已读完文章数
     */
    private int articleCompleted;

    /**
     * 阅读中文章数
     */
    private int articleInProgress;

    // ========== 单词统计 ==========
    /**
     * 已学习单词数
     */
    private int wordStudied;

    /**
     * 已掌握单词数
     */
    private int wordMastered;

    /**
     * 熟悉单词数
     */
    private int wordFamiliar;

    /**
     * 生词数
     */
    private int wordNew;

    /**
     * 生词本总数
     */
    private int wordBookTotal;

    /**
     * 生词本已掌握数
     */
    private int wordBookMastered;

    // ========== 课程统计 ==========
    /**
     * 已报名/已购课程数
     */
    private int courseEnrolled;

    /**
     * 已完成课程数
     */
    private int courseCompleted;

    /**
     * 收藏课程数
     */
    private int courseFavorited;

    /**
     * 课程评价数
     */
    private int courseReviewed;

    /**
     * 平均评分
     */
    private double avgRating;

    // ========== 课程列表 ==========
    /**
     * 已购课程信息
     */
    private List<CourseSummary> enrolledCourses;

    /**
     * 收藏课程信息
     */
    private List<CourseSummary> favoriteCourses;

    // ========== 学习计划 ==========
    /**
     * 进行中的学习计划数
     */
    private int activeScheduleCount;

    /**
     * 课程摘要
     */
    @Data
    public static class CourseSummary implements Serializable {

        private static final long serialVersionUID = 1L;

        /**
         * 课程ID
         */
        private Long id;

        /**
         * 课程标题
         */
        private String title;

        /**
         * 课程标签（JSON 数组格式字符串）
         */
        private String tags;

        /**
         * 难度等级：1-入门，2-初级，3-中级，4-高级，5-专家
         */
        private Integer difficulty;
    }
}
