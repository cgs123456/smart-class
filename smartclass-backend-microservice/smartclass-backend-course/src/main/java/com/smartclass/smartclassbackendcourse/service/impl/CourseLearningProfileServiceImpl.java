package com.smartclass.smartclassbackendcourse.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.smartclass.smartclassbackendcourse.service.CourseFavouriteService;
import com.smartclass.smartclassbackendcourse.service.CourseLearningProfileService;
import com.smartclass.smartclassbackendcourse.service.CourseReviewService;
import com.smartclass.smartclassbackendcourse.service.CourseService;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendmodel.model.entity.Course;
import com.smartclass.smartclassbackendmodel.model.entity.CourseFavourite;
import com.smartclass.smartclassbackendmodel.model.entity.CourseReview;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * 课程学习画像服务实现
 * 逻辑自 CourseInnerController 迁移而来，供 Feign 内部接口与 Dubbo 服务共用
 */
@Service
@Slf4j
public class CourseLearningProfileServiceImpl implements CourseLearningProfileService {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseFavouriteService courseFavouriteService;

    @Resource
    private CourseReviewService courseReviewService;

    @Override
    public LearningProfileDTO getCourseLearningSummary(Long userId) {
        LearningProfileDTO profile = new LearningProfileDTO();

        // 收藏课程列表
        List<CourseFavourite> favouriteList = Collections.emptyList();
        try {
            favouriteList = courseFavouriteService.list(
                    new QueryWrapper<CourseFavourite>().eq("userId", userId)
            );
            if (favouriteList == null) {
                favouriteList = Collections.emptyList();
            }
            profile.setCourseFavorited(favouriteList.size());
        } catch (Exception e) {
            favouriteList = Collections.emptyList();
        }

        // 课程评价统计
        try {
            List<CourseReview> reviewList = courseReviewService.list(
                    new QueryWrapper<CourseReview>().eq("userId", userId)
            );
            if (reviewList != null) {
                profile.setCourseReviewed(reviewList.size());
                if (!reviewList.isEmpty()) {
                    double sum = 0.0;
                    int validCount = 0;
                    for (CourseReview review : reviewList) {
                        if (review.getRating() != null) {
                            sum += review.getRating();
                            validCount++;
                        }
                    }
                    if (validCount > 0) {
                        // 保留一位小数
                        double avg = Math.round(sum * 10.0 / validCount) / 10.0;
                        profile.setAvgRating(avg);
                    }
                }
            }
        } catch (Exception e) {
            // 查询失败保持默认值
        }

        // 收藏课程详细信息
        try {
            if (!favouriteList.isEmpty()) {
                List<Long> courseIds = new ArrayList<>(favouriteList.size());
                for (CourseFavourite fav : favouriteList) {
                    if (fav.getCourseId() != null) {
                        courseIds.add(fav.getCourseId());
                    }
                }
                if (!courseIds.isEmpty()) {
                    List<Course> courses = courseService.listByIds(courseIds);
                    List<LearningProfileDTO.CourseSummary> favoriteSummaries = new ArrayList<>();
                    if (courses != null) {
                        for (Course course : courses) {
                            if (course == null) {
                                continue;
                            }
                            LearningProfileDTO.CourseSummary summary = new LearningProfileDTO.CourseSummary();
                            summary.setId(course.getId());
                            summary.setTitle(course.getTitle());
                            summary.setTags(course.getTags());
                            summary.setDifficulty(course.getDifficulty());
                            favoriteSummaries.add(summary);
                        }
                    }
                    profile.setFavoriteCourses(favoriteSummaries);
                    // 由于缺少用户课程进度/已购表，这里将收藏课程同时作为"已学课程"参考
                    profile.setEnrolledCourses(favoriteSummaries);
                    profile.setCourseEnrolled(favoriteSummaries.size());
                }
            }
        } catch (Exception e) {
            // 查询失败保持默认值
        }

        return profile;
    }
}
