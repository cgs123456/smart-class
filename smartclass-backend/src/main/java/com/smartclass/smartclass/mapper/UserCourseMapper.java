package com.smartclass.smartclass.mapper;

import com.smartclass.smartclass.model.entity.UserCourse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @description 针对表【user_course(用户课程)】的数据库操作Mapper
 * @Entity com.smartclass.smartclass.model.entity.UserCourse
 */
@Mapper
public interface UserCourseMapper extends BaseMapper<UserCourse> {

}
