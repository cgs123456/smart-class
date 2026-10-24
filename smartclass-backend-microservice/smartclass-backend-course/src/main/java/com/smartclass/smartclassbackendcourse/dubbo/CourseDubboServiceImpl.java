package com.smartclass.smartclassbackendcourse.dubbo;

import com.smartclass.smartclassbackendcourse.service.CourseChapterService;
import com.smartclass.smartclassbackendcourse.service.CourseLearningProfileService;
import com.smartclass.smartclassbackendcourse.service.CourseSectionService;
import com.smartclass.smartclassbackendcourse.service.CourseService;
import com.smartclass.smartclassbackendmodel.model.dto.profile.LearningProfileDTO;
import com.smartclass.smartclassbackendmodel.model.entity.Course;
import com.smartclass.smartclassbackendmodel.model.entity.CourseChapter;
import com.smartclass.smartclassbackendmodel.model.entity.CourseSection;
import com.smartclass.smartclassbackendserviceclient.dubbo.CourseDubboService;
import jakarta.annotation.Resource;
import org.apache.dubbo.config.annotation.DubboService;

import java.util.Collection;
import java.util.List;

/**
 * 课程 Dubbo 服务实现
 *
 * 替代原 CourseInnerController (Feign 内部接口)
 * 协议: Triple (HTTP/2 + Protobuf)
 * 注册中心: Nacos
 */
@DubboService(
    version = "1.0.0",
    protocol = "triple",
    interfaceClass = CourseDubboService.class
)
public class CourseDubboServiceImpl implements CourseDubboService {

    @Resource
    private CourseService courseService;

    @Resource
    private CourseChapterService courseChapterService;

    @Resource
    private CourseSectionService courseSectionService;

    @Resource
    private CourseLearningProfileService courseLearningProfileService;

    @Override
    public List<Course> listAllCourses() {
        return courseService.list();
    }

    @Override
    public Course getCourseById(Long id) {
        return courseService.getById(id);
    }

    @Override
    public List<CourseChapter> listChapters(Long courseId) {
        return courseChapterService.getChaptersByCourseId(courseId);
    }

    @Override
    public List<CourseChapter> listAllChapters() {
        return courseChapterService.list();
    }

    @Override
    public List<CourseSection> listAllSections() {
        return courseSectionService.list();
    }

    @Override
    public List<CourseSection> listSections(Long courseId) {
        return courseSectionService.getSectionsByCourseId(courseId);
    }

    @Override
    public List<Course> listCoursesByIds(Collection<Long> ids) {
        return courseService.listByIds(ids);
    }

    @Override
    public LearningProfileDTO getCourseLearningSummary(Long userId) {
        return courseLearningProfileService.getCourseLearningSummary(userId);
    }
}
