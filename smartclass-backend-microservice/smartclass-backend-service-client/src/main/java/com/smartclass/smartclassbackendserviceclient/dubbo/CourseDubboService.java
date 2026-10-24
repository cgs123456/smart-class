package com.smartclass.smartclassbackendserviceclient.dubbo;

import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendmodel.model.entity.Course;
import com.smartclass.smartclassbackendmodel.model.entity.CourseChapter;
import com.smartclass.smartclassbackendmodel.model.entity.CourseSection;

import java.util.Collection;
import java.util.List;

/**
 * 课程 Dubbo 服务接口
 *
 * 定义内部 RPC 方法，替代原 Feign CourseInnerController
 *
 * 迁移对照:
 * - CourseFeignClient.listAllCourses           -> listAllCourses
 * - CourseFeignClient.getCourseById            -> getCourseById
 * - CourseFeignClient.listChapters             -> listChapters
 * - CourseFeignClient.listAllChapters          -> listAllChapters
 * - CourseFeignClient.listSections             -> listSections
 * - CourseFeignClient.listAllSections          -> listAllSections
 * - CourseFeignClient.listCoursesByIds         -> listCoursesByIds
 * - CourseFeignClient.getCourseLearningSummary -> getCourseLearningSummary
 */
public interface CourseDubboService {

    /**
     * 获取所有未删除的课程
     *
     * @return 课程列表
     */
    List<Course> listAllCourses();

    /**
     * 根据ID获取课程
     *
     * @param id 课程ID
     * @return 课程信息
     */
    Course getCourseById(Long id);

    /**
     * 根据课程ID获取章节列表
     *
     * @param courseId 课程ID
     * @return 章节列表
     */
    List<CourseChapter> listChapters(Long courseId);

    /**
     * 获取所有章节
     *
     * @return 章节列表
     */
    List<CourseChapter> listAllChapters();

    /**
     * 获取所有小节
     *
     * @return 小节列表
     */
    List<CourseSection> listAllSections();

    /**
     * 根据课程ID获取小节列表
     *
     * @param courseId 课程ID
     * @return 小节列表
     */
    List<CourseSection> listSections(Long courseId);

    /**
     * 根据ID集合批量获取课程
     *
     * @param ids 课程ID集合
     * @return 课程列表
     */
    List<Course> listCoursesByIds(Collection<Long> ids);

    /**
     * 获取用户课程学习摘要（收藏、评价、已购课程等）
     *
     * @param userId 用户ID
     * @return 课程学习画像数据
     */
    LearningProfileDTO getCourseLearningSummary(Long userId);
}
