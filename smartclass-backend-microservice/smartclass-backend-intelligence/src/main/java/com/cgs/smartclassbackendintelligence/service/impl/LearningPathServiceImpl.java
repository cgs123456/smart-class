package com.cgs.smartclassbackendintelligence.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclassbackendintelligence.service.LearningPathService;
import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.cgs.smartclassbackendmodel.model.entity.User;
import com.cgs.smartclassbackendserviceclient.service.CourseFeignClient;
import com.cgs.smartclassbackendserviceclient.service.DailyArticleFeignClient;
import com.cgs.smartclassbackendserviceclient.service.DailyWordFeignClient;
import com.cgs.smartclassbackendserviceclient.service.UserFeignClient;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 学习路径推荐服务实现
 * 通过 Feign 调用各微服务聚合用户学习记录，构建用户画像文本供 LLM 使用
 */
@Service
@Slf4j
public class LearningPathServiceImpl implements LearningPathService {

    /**
     * 用户画像文本最大字符数
     */
    private static final int MAX_PROFILE_LENGTH = 1500;

    @Resource
    private UserFeignClient userFeignClient;

    @Resource
    private CourseFeignClient courseFeignClient;

    @Resource
    private DailyWordFeignClient dailyWordFeignClient;

    @Resource
    private DailyArticleFeignClient dailyArticleFeignClient;

    @Value("${rag.enabled:false}")
    private boolean ragEnabled;

    @Override
    public String buildUserProfile(Long userId) {
        if (userId == null) {
            return "";
        }
        // rag.enabled 关闭时不注入画像，与 RAG 上下文行为保持一致
        if (!ragEnabled) {
            return "";
        }

        // 1. 获取用户基本信息
        User user = null;
        try {
            user = userFeignClient.getById(userId);
        } catch (Exception e) {
            log.warn("获取用户信息失败 userId={}: {}", userId, e.getMessage());
        }

        // 2. 获取单词学习摘要
        LearningProfileDTO wordProfile = safeGet(() -> dailyWordFeignClient.getWordLearningSummary(userId), "word", userId);

        // 3. 获取文章学习摘要
        LearningProfileDTO articleProfile = safeGet(() -> dailyArticleFeignClient.getArticleLearningSummary(userId), "article", userId);

        // 4. 获取课程学习摘要
        LearningProfileDTO courseProfile = safeGet(() -> courseFeignClient.getCourseLearningSummary(userId), "course", userId);

        // 5. 合并数据并拼接为格式化文本
        String profile = formatUserProfile(user, wordProfile, articleProfile, courseProfile);

        // 控制在 1500 字符以内
        if (profile.length() > MAX_PROFILE_LENGTH) {
            profile = profile.substring(0, MAX_PROFILE_LENGTH);
        }
        return profile;
    }

    /**
     * 安全调用 Feign 接口，失败时返回 null 并记录警告
     */
    private LearningProfileDTO safeGet(java.util.function.Supplier<LearningProfileDTO> supplier, String tag, Long userId) {
        try {
            return supplier.get();
        } catch (Exception e) {
            log.warn("获取{}学习摘要失败 userId={}: {}", tag, userId, e.getMessage());
            return null;
        }
    }

    /**
     * 将用户信息与各维度学习摘要拼接为格式化文本
     */
    private String formatUserProfile(User user,
                                     LearningProfileDTO wordProfile,
                                     LearningProfileDTO articleProfile,
                                     LearningProfileDTO courseProfile) {
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像：\n");

        // 用户基本信息
        if (user != null) {
            String userName = StrUtil.isBlank(user.getUserName()) ? "未设置" : user.getUserName();
            String userRole = StrUtil.isBlank(user.getUserRole()) ? "student" : user.getUserRole();
            String province = StrUtil.nullToEmpty(user.getProvince());
            String city = StrUtil.nullToEmpty(user.getCity());
            String region = (province + city).isEmpty() ? "未设置" : province + city;
            String userProfile = StrUtil.isBlank(user.getUserProfile()) ? "无" : user.getUserProfile();
            sb.append("- 姓名：").append(userName).append("\n");
            sb.append("- 角色：").append(userRole).append("\n");
            sb.append("- 地区：").append(region).append("\n");
            sb.append("- 简介：").append(userProfile).append("\n");
        }

        sb.append("\n学习记录摘要：\n");

        // 文章统计
        int articleTotal = articleProfile != null ? articleProfile.getArticleTotal() : 0;
        int articleCompleted = articleProfile != null ? articleProfile.getArticleCompleted() : 0;
        int articleInProgress = articleProfile != null ? articleProfile.getArticleInProgress() : 0;
        sb.append("- 已阅读文章 ").append(articleTotal).append(" 篇（精读 ")
                .append(articleCompleted).append(" 篇，阅读中 ").append(articleInProgress).append(" 篇）\n");

        // 单词统计
        int wordStudied = wordProfile != null ? wordProfile.getWordStudied() : 0;
        int wordMastered = wordProfile != null ? wordProfile.getWordMastered() : 0;
        int wordFamiliar = wordProfile != null ? wordProfile.getWordFamiliar() : 0;
        int wordNew = wordProfile != null ? wordProfile.getWordNew() : 0;
        sb.append("- 已学习单词 ").append(wordStudied).append(" 个（掌握 ")
                .append(wordMastered).append(" 个，熟悉 ").append(wordFamiliar)
                .append(" 个，生词 ").append(wordNew).append(" 个）\n");

        // 生词本统计
        int wordBookTotal = wordProfile != null ? wordProfile.getWordBookTotal() : 0;
        int wordBookMastered = wordProfile != null ? wordProfile.getWordBookMastered() : 0;
        sb.append("- 生词本收录 ").append(wordBookTotal).append(" 个（已掌握 ")
                .append(wordBookMastered).append(" 个）\n");

        // 课程统计
        int courseEnrolled = courseProfile != null ? courseProfile.getCourseEnrolled() : 0;
        int courseCompleted = courseProfile != null ? courseProfile.getCourseCompleted() : 0;
        int courseFavorited = courseProfile != null ? courseProfile.getCourseFavorited() : 0;
        int courseReviewed = courseProfile != null ? courseProfile.getCourseReviewed() : 0;
        double avgRating = courseProfile != null ? courseProfile.getAvgRating() : 0.0;
        sb.append("- 已报名课程 ").append(courseEnrolled).append(" 门（已完成 ")
                .append(courseCompleted).append(" 门）\n");
        sb.append("- 收藏课程 ").append(courseFavorited).append(" 门\n");
        sb.append("- 课程评价 ").append(courseReviewed).append(" 条（平均评分 ")
                .append(avgRating).append(" 分）\n");

        sb.append("\n学习偏好：\n");

        // 常学课程类型（从已购/收藏课程 tags 聚合）
        String tagsText = aggregateCourseTags(courseProfile);
        sb.append("- 常学课程类型：").append(StrUtil.isBlank(tagsText) ? "暂无" : tagsText).append("\n");

        // 已学课程列表（取已购课程，最多 3 个）
        String enrolledCoursesText = formatEnrolledCourses(courseProfile);
        sb.append("- 已学课程：").append(StrUtil.isBlank(enrolledCoursesText) ? "暂无" : enrolledCoursesText).append("\n");

        return sb.toString();
    }

    /**
     * 从已购/收藏课程的 tags JSON 数组中聚合出现频次最高的标签
     */
    private String aggregateCourseTags(LearningProfileDTO courseProfile) {
        if (courseProfile == null) {
            return "";
        }
        List<LearningProfileDTO.CourseSummary> courses = courseProfile.getFavoriteCourses();
        if (courses == null || courses.isEmpty()) {
            // 已购与收藏使用同一份数据，这里优先用收藏
            courses = courseProfile.getEnrolledCourses();
        }
        if (courses == null || courses.isEmpty()) {
            return "";
        }

        Map<String, Integer> tagCount = new HashMap<>();
        for (LearningProfileDTO.CourseSummary course : courses) {
            String tags = course.getTags();
            if (StrUtil.isBlank(tags)) {
                continue;
            }
            try {
                List<String> tagList = JSONUtil.parseArray(tags).toList(String.class);
                for (String tag : tagList) {
                    if (StrUtil.isBlank(tag)) {
                        continue;
                    }
                    tagCount.merge(tag.trim(), 1, Integer::sum);
                }
            } catch (Exception e) {
                // tags 不是 JSON 数组格式，跳过
            }
        }
        if (tagCount.isEmpty()) {
            return "";
        }
        // 按频次降序取前 5 个
        List<String> topTags = new ArrayList<>(tagCount.keySet());
        topTags.sort((a, b) -> tagCount.get(b) - tagCount.get(a));
        int limit = Math.min(5, topTags.size());
        return String.join("、", topTags.subList(0, limit));
    }

    /**
     * 格式化已学课程列表，最多取 3 个标题
     */
    private String formatEnrolledCourses(LearningProfileDTO courseProfile) {
        if (courseProfile == null) {
            return "";
        }
        List<LearningProfileDTO.CourseSummary> courses = courseProfile.getEnrolledCourses();
        if (courses == null || courses.isEmpty()) {
            return "";
        }
        List<String> titles = new ArrayList<>();
        for (int i = 0; i < courses.size() && titles.size() < 3; i++) {
            LearningProfileDTO.CourseSummary course = courses.get(i);
            if (course != null && StrUtil.isNotBlank(course.getTitle())) {
                titles.add(course.getTitle());
            }
        }
        return String.join("、", titles);
    }
}
