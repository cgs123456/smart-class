package com.cgs.smartclass.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclass.model.dto.UserArticleRecord;
import com.cgs.smartclass.model.entity.Course;
import com.cgs.smartclass.model.entity.CourseFavourite;
import com.cgs.smartclass.model.entity.CourseReview;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.entity.UserCourse;
import com.cgs.smartclass.model.entity.UserCourseProgress;
import com.cgs.smartclass.model.entity.UserDailyWord;
import com.cgs.smartclass.model.entity.UserWordBook;
import com.cgs.smartclass.service.CourseFavouriteService;
import com.cgs.smartclass.service.CourseReviewService;
import com.cgs.smartclass.service.CourseService;
import com.cgs.smartclass.service.LearningPathService;
import com.cgs.smartclass.service.UserArticleRecordService;
import com.cgs.smartclass.service.UserCourseProgressService;
import com.cgs.smartclass.service.UserCourseService;
import com.cgs.smartclass.service.UserDailyWordService;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.service.UserWordBookService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 个性化学习路径推荐服务实现
 * 聚合用户学习记录，构建用户画像文本，注入 Dify 对话用于 AI 推荐
 */
@Service
@Slf4j
public class LearningPathServiceImpl implements LearningPathService {

    @Resource
    private UserService userService;

    @Resource
    private UserCourseService userCourseService;

    @Resource
    private UserCourseProgressService userCourseProgressService;

    @Resource
    private CourseService courseService;

    @Resource
    private CourseFavouriteService courseFavouriteService;

    @Resource
    private CourseReviewService courseReviewService;

    @Resource
    private UserArticleRecordService userArticleRecordService;

    @Resource
    private UserDailyWordService userDailyWordService;

    @Resource
    private UserWordBookService userWordBookService;

    @Value("${rag.enabled:false}")
    private boolean ragEnabled;

    /**
     * 用户画像文本最大长度（控制在 1500 字符以内）
     */
    private static final int MAX_PROFILE_LENGTH = 1500;

    @Override
    public String buildUserProfile(Long userId) {
        if (!ragEnabled) {
            return "";
        }
        if (userId == null || userId <= 0) {
            return "";
        }

        // 1. 用户基本信息
        String userName = "";
        String userProfile = "";
        String province = "";
        String city = "";
        String userRole = "";
        try {
            User user = userService.getById(userId);
            if (user != null) {
                userName = StrUtil.isBlank(user.getUserName()) ? "" : user.getUserName();
                userProfile = StrUtil.isBlank(user.getUserProfile()) ? "" : user.getUserProfile();
                province = StrUtil.isBlank(user.getProvince()) ? "" : user.getProvince();
                city = StrUtil.isBlank(user.getCity()) ? "" : user.getCity();
                userRole = StrUtil.isBlank(user.getUserRole()) ? "" : user.getUserRole();
            }
        } catch (Exception e) {
            log.warn("获取用户基本信息失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 2. 文章学习统计
        int articleTotal = 0;
        int articleFinished = 0;
        int articleReading = 0;
        try {
            List<UserArticleRecord> articleRecords = userArticleRecordService.list(
                    new QueryWrapper<UserArticleRecord>().eq("userId", userId));
            if (CollUtil.isNotEmpty(articleRecords)) {
                articleTotal = articleRecords.size();
                for (UserArticleRecord r : articleRecords) {
                    Integer rs = r.getReadStatus();
                    if (rs != null && rs == 2) {
                        articleFinished++;
                    } else if (rs != null && rs == 1) {
                        articleReading++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取文章学习统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 3. 单词学习统计
        int wordStudied = 0;
        int wordMastered = 0;
        int wordFamiliar = 0;
        int wordNew = 0;
        try {
            List<UserDailyWord> dailyWords = userDailyWordService.list(
                    new QueryWrapper<UserDailyWord>().eq("userId", userId));
            if (CollUtil.isNotEmpty(dailyWords)) {
                for (UserDailyWord w : dailyWords) {
                    if (w.getIsStudied() != null && w.getIsStudied() == 1) {
                        wordStudied++;
                    }
                    Integer ml = w.getMasteryLevel();
                    if (ml != null) {
                        if (ml == 3) {
                            wordMastered++;
                        } else if (ml == 2) {
                            wordFamiliar++;
                        } else if (ml == 1) {
                            wordNew++;
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取单词学习统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 4. 生词本统计
        int wordBookTotal = 0;
        int wordBookMastered = 0;
        int wordBookLearned = 0;
        try {
            List<UserWordBook> wordBooks = userWordBookService.list(
                    new QueryWrapper<UserWordBook>().eq("userId", userId).eq("isDeleted", 0));
            if (CollUtil.isNotEmpty(wordBooks)) {
                wordBookTotal = wordBooks.size();
                for (UserWordBook wb : wordBooks) {
                    Integer ls = wb.getLearningStatus();
                    if (ls != null && ls == 2) {
                        wordBookMastered++;
                    } else if (ls != null && ls == 1) {
                        wordBookLearned++;
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取生词本统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 5. 课程学习统计 + 收集标签/难度偏好
        int purchasedCount = 0;
        int completedCount = 0;
        int avgProgress = 0;
        List<String> purchasedCourseTitles = new ArrayList<>();
        Map<String, Integer> tagFreq = new HashMap<>();
        Map<Integer, Integer> difficultyFreq = new HashMap<>();
        try {
            List<UserCourse> userCourses = userCourseService.list(
                    new QueryWrapper<UserCourse>().eq("userId", userId).eq("isDelete", 0).eq("status", 1));
            if (CollUtil.isNotEmpty(userCourses)) {
                purchasedCount = userCourses.size();
                int progressSum = 0;
                int courseCountForProgress = 0;
                for (UserCourse uc : userCourses) {
                    Long courseId = uc.getCourseId();
                    if (courseId == null) {
                        continue;
                    }
                    // 进度统计
                    try {
                        List<UserCourseProgress> progressList = userCourseProgressService.list(
                                new QueryWrapper<UserCourseProgress>().eq("userId", userId).eq("courseId", courseId));
                        if (CollUtil.isNotEmpty(progressList)) {
                            boolean allCompleted = true;
                            int progSum = 0;
                            for (UserCourseProgress p : progressList) {
                                if (p.getIsCompleted() == null || p.getIsCompleted() != 1) {
                                    allCompleted = false;
                                }
                                if (p.getProgress() != null) {
                                    progSum += p.getProgress();
                                }
                            }
                            if (allCompleted) {
                                completedCount++;
                            }
                            int courseProgress = progSum / progressList.size();
                            progressSum += courseProgress;
                            courseCountForProgress++;
                        }
                    } catch (Exception e) {
                        log.warn("获取课程进度失败, userId={}, courseId={}, msg={}", userId, courseId, e.getMessage());
                    }
                    // 课程信息（标题、标签、难度）
                    try {
                        Course course = courseService.getById(courseId);
                        if (course != null) {
                            if (StrUtil.isNotBlank(course.getTitle())) {
                                purchasedCourseTitles.add(course.getTitle());
                            }
                            collectTags(course.getTags(), tagFreq);
                            if (course.getDifficulty() != null) {
                                difficultyFreq.merge(course.getDifficulty(), 1, Integer::sum);
                            }
                        }
                    } catch (Exception e) {
                        log.warn("获取课程信息失败, courseId={}, msg={}", courseId, e.getMessage());
                    }
                }
                if (courseCountForProgress > 0) {
                    avgProgress = progressSum / courseCountForProgress;
                }
            }
        } catch (Exception e) {
            log.warn("获取课程学习统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 6. 课程收藏统计（同步补充标签偏好）
        int favouriteCount = 0;
        try {
            List<CourseFavourite> favourites = courseFavouriteService.list(
                    new QueryWrapper<CourseFavourite>().eq("userId", userId));
            if (CollUtil.isNotEmpty(favourites)) {
                favouriteCount = favourites.size();
                for (CourseFavourite cf : favourites) {
                    Long courseId = cf.getCourseId();
                    if (courseId == null) {
                        continue;
                    }
                    try {
                        Course course = courseService.getById(courseId);
                        if (course != null) {
                            collectTags(course.getTags(), tagFreq);
                        }
                    } catch (Exception e) {
                        log.warn("获取收藏课程信息失败, courseId={}, msg={}", courseId, e.getMessage());
                    }
                }
            }
        } catch (Exception e) {
            log.warn("获取课程收藏统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 7. 课程评价统计
        int reviewCount = 0;
        double avgRating = 0.0;
        try {
            List<CourseReview> reviews = courseReviewService.list(
                    new QueryWrapper<CourseReview>().eq("userId", userId).eq("isDelete", 0));
            if (CollUtil.isNotEmpty(reviews)) {
                reviewCount = reviews.size();
                int ratingSum = 0;
                int ratedCount = 0;
                for (CourseReview r : reviews) {
                    if (r.getRating() != null) {
                        ratingSum += r.getRating();
                        ratedCount++;
                    }
                }
                if (ratedCount > 0) {
                    avgRating = (double) ratingSum / ratedCount;
                }
            }
        } catch (Exception e) {
            log.warn("获取课程评价统计失败, userId={}, msg={}", userId, e.getMessage());
        }

        // 8. 学习偏好分析
        String topTags = analyzeTopTags(tagFreq);
        String difficultyPreference = analyzeDifficulty(difficultyFreq);
        String learnedCourses = joinCourseTitles(purchasedCourseTitles, 3);

        // 拼接为格式化文本
        StringBuilder sb = new StringBuilder();
        sb.append("用户画像：\n");
        sb.append("- 姓名：").append(userName).append("\n");
        sb.append("- 角色：").append(userRole).append("\n");
        sb.append("- 地区：").append(province).append(city).append("\n");
        sb.append("- 简介：").append(userProfile).append("\n");
        sb.append("\n学习记录摘要：\n");
        sb.append("- 已阅读文章 ").append(articleTotal).append(" 篇（精读 ")
                .append(articleFinished).append(" 篇，阅读中 ").append(articleReading).append(" 篇）\n");
        sb.append("- 已学习单词 ").append(wordStudied).append(" 个（掌握 ")
                .append(wordMastered).append(" 个，熟悉 ").append(wordFamiliar)
                .append(" 个，生词 ").append(wordNew).append(" 个）\n");
        sb.append("- 生词本收录 ").append(wordBookTotal).append(" 个（已掌握 ")
                .append(wordBookMastered).append(" 个）\n");
        sb.append("- 已报名课程 ").append(purchasedCount).append(" 门（已完成 ")
                .append(completedCount).append(" 门，平均进度 ").append(avgProgress).append("%）\n");
        sb.append("- 收藏课程 ").append(favouriteCount).append(" 门\n");
        sb.append("- 课程评价 ").append(reviewCount).append(" 条（平均评分 ")
                .append(String.format("%.1f", avgRating)).append(" 分）\n");
        sb.append("\n学习偏好：\n");
        sb.append("- 常学课程类型：").append(topTags).append("\n");
        sb.append("- 难度偏好：").append(difficultyPreference).append("\n");
        sb.append("- 已学课程：").append(learnedCourses);

        String result = sb.toString();
        if (result.length() > MAX_PROFILE_LENGTH) {
            result = result.substring(0, MAX_PROFILE_LENGTH);
        }
        return result;
    }

    /**
     * 解析课程标签 JSON 数组并累计标签频次
     */
    private void collectTags(String tagsJson, Map<String, Integer> tagFreq) {
        if (StrUtil.isBlank(tagsJson)) {
            return;
        }
        try {
            JSONArray arr = JSONUtil.parseArray(tagsJson);
            for (Object o : arr) {
                if (o == null) {
                    continue;
                }
                String tag = o.toString().trim();
                if (StrUtil.isNotBlank(tag)) {
                    tagFreq.merge(tag, 1, Integer::sum);
                }
            }
        } catch (Exception e) {
            log.warn("解析课程标签失败, tags={}, msg={}", tagsJson, e.getMessage());
        }
    }

    /**
     * 取频次最高的前 5 个标签
     */
    private String analyzeTopTags(Map<String, Integer> tagFreq) {
        if (tagFreq.isEmpty()) {
            return "暂无数据";
        }
        return tagFreq.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.joining("、"));
    }

    /**
     * 取频次最高的难度等级作为偏好
     */
    private String analyzeDifficulty(Map<Integer, Integer> difficultyFreq) {
        if (difficultyFreq.isEmpty()) {
            return "暂无数据";
        }
        Integer top = difficultyFreq.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        return difficultyLabel(top);
    }

    /**
     * 难度等级文本：1-入门，2-初级，3-中级，4-高级，5-专家
     */
    private String difficultyLabel(Integer difficulty) {
        if (difficulty == null) {
            return "暂无数据";
        }
        switch (difficulty) {
            case 1:
                return "入门";
            case 2:
                return "初级";
            case 3:
                return "中级";
            case 4:
                return "高级";
            case 5:
                return "专家";
            default:
                return "未知";
        }
    }

    /**
     * 拼接课程标题（限制数量）
     */
    private String joinCourseTitles(List<String> titles, int limit) {
        if (CollUtil.isEmpty(titles)) {
            return "暂无";
        }
        return titles.stream().limit(limit).collect(Collectors.joining("、"));
    }
}
