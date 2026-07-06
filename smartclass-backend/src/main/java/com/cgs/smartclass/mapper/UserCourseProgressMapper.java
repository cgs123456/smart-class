package com.cgs.smartclass.mapper;

import com.cgs.smartclass.model.entity.UserCourseProgress;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author cgs
 * @description 针对表【user_course_progress(用户课程学习进度)】的数据库操作Mapper
 * @Entity com.cgs.smartclass.model.entity.UserCourseProgress
 */
@Mapper
public interface UserCourseProgressMapper extends BaseMapper<UserCourseProgress> {

}
