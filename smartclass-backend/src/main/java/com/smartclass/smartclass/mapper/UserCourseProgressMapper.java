package com.smartclass.smartclass.mapper;

import com.smartclass.smartclass.model.entity.UserCourseProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【user_course_progress(用户课程学习进度)】的数据库操作Mapper
 * @Entity com.smartclass.smartclass.model.entity.UserCourseProgress
 */
@Mapper
public interface UserCourseProgressMapper extends BaseMapper<UserCourseProgress> {

}
