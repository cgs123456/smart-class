package com.cgs.smartclassbackendcourse.controller.inner;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.cgs.smartclassbackendcourse.service.CourseFavouriteService;
import com.cgs.smartclassbackendcourse.service.CourseReviewService;
import com.cgs.smartclassbackendcourse.service.CourseService;
import com.cgs.smartclassbackendcourse.service.CourseChapterService;
import com.cgs.smartclassbackendcourse.service.CourseSectionService;
import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.cgs.smartclassbackendmodel.model.entity.Course;
import com.cgs.smartclassbackendmodel.model.entity.CourseChapter;
import com.cgs.smartclassbackendmodel.model.entity.CourseFavourite;
import com.cgs.smartclassbackendmodel.model.entity.CourseReview;
import com.cgs.smartclassbackendmodel.model.entity.CourseSection;
import com.cgs.smartclassbackendserviceclient.service.CourseFeignClient;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * 课程服务内部接口
 * 供其他微服务通过 Feign 调用，/inner/** 路径已被网关拦截外部访问
 */
@RestController
@RequestMapping("/inner")
public class CourseInnerController implements CourseFeignClient {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseSectionService courseSectionService;

    @Resource
    private CourseFavouriteService courseFavouriteService;

    @Resource
    private CourseReviewService courseReviewService;

    /**
     * 获取所有未删除的课程
     */
    @Override
    @GetMapping("/course/list")
    public List<Course> listAllCourses() {
        return courseService.list();
    }

    /**
     * 根据ID获取课程
     */
    @Override
    @GetMapping("/course/get/{id}")
    public Course getCourseById(@PathVariable("id") Long id) {
        return courseService.getById(id);
    }

    /**
     * 根据课程ID获取章节列表
     */
    @Override
    @GetMapping("/chapter/list")
    public List<CourseChapter> listChapters(@RequestParam("courseId") Long courseId) {
        return courseChapterService.getChaptersByCourseId(courseId);
    }

    /**
     * 获取所有章节
     */
    @Override
    @GetMapping("/chapter/list/all")
    public List<CourseChapter> listAllChapters() {
        return courseChapterService.list();
    }

    /**
     * 获取所有小节
     */
    @Override
    @GetMapping("/section/list/all")
    public List<CourseSection> listAllSections() {
        return courseSectionService.list();
    }

    /**
     * 根据课程ID获取小节列表
     */
    @Override
    @GetMapping("/section/list")
    public List<CourseSection> listSections(@RequestParam("courseId") Long courseId) {
        return courseSectionService.getSectionsByCourseId(courseId);
    }

    /**
     * 根据ID集合批量获取课程
     */
    @Override
    @PostMapping("/course/listByIds")
    public List<Course> listCoursesByIds(@RequestBody Collection<Long> ids) {
        return courseService.listByIds(ids);
    }

    /**
     * 获取用户课程学习摘要
     * 统计用户收藏课程数、课程评价数及平均评分，并返回收藏课程信息
     *
     * @param userId 用户ID
     * @return 课程学习画像数据
     */
    @Override
    @GetMapping("/course/learning/summary")
    public LearningProfileDTO getCourseLearningSummary(@RequestParam("userId") Long userId) {
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

