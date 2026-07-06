package com.cgs.smartclassbackendserviceclient.service;

import com.cgs.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.cgs.smartclassbackendmodel.model.entity.Course;
import com.cgs.smartclassbackendmodel.model.entity.CourseChapter;
import com.cgs.smartclassbackendmodel.model.entity.CourseSection;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.Collection;
import java.util.List;

/**
 * 课程服务 Feign 客户端
 *
 * <p>注意：name 必须与 course 服务在 Nacos 注册的服务名一致，即 {@code smartclass-backend-course}。
 * path 指向 course 服务内部接口前缀 {@code /inner}。</p>
 */
@FeignClient(name = "smartclass-backend-course", path = "/inner")
public interface CourseFeignClient {

    /**
     * 获取所有未删除的课程
     *
     * @return 课程列表
     */
    @GetMapping("/course/list")
    List<Course> listAllCourses();

    /**
     * 根据ID获取课程
     *
     * @param id 课程ID
     * @return 课程信息
     */
    @GetMapping("/course/get/{id}")
    Course getCourseById(@PathVariable("id") Long id);

    /**
     * 根据课程ID获取章节列表
     *
     * @param courseId 课程ID
     * @return 章节列表
     */
    @GetMapping("/chapter/list")
    List<CourseChapter> listChapters(@RequestParam("courseId") Long courseId);

    /**
     * 获取所有章节
     *
     * @return 章节列表
     */
    @GetMapping("/chapter/list/all")
    List<CourseChapter> listAllChapters();

    /**
     * 获取所有小节
     *
     * @return 小节列表
     */
    @GetMapping("/section/list/all")
    List<CourseSection> listAllSections();

    /**
     * 根据课程ID获取小节列表
     *
     * @param courseId 课程ID
     * @return 小节列表
     */
    @GetMapping("/section/list")
    List<CourseSection> listSections(@RequestParam("courseId") Long courseId);

    /**
     * 根据ID集合批量获取课程
     *
     * @param ids 课程ID集合
     * @return 课程列表
     */
    @PostMapping("/course/listByIds")
    List<Course> listCoursesByIds(@RequestBody Collection<Long> ids);

    /**
     * 获取用户课程学习摘要（收藏、评价、已购课程等）
     *
     * @param userId 用户ID
     * @return 课程学习画像数据
     */
    @GetMapping("/course/learning/summary")
    LearningProfileDTO getCourseLearningSummary(@RequestParam("userId") Long userId);
}
