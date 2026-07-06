package com.cgs.smartclass.mapper;

import com.cgs.smartclass.model.entity.UserCourse;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * @author cgs
 * @description 针对表【user_course(用户课程)】的数据库操作Mapper
 * @Entity com.cgs.smartclass.model.entity.UserCourse
 */
@Mapper
public interface UserCourseMapper extends BaseMapper<UserCourse> {

}
