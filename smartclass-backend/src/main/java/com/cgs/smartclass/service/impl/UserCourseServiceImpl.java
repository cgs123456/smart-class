package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.mapper.UserCourseMapper;
import com.cgs.smartclass.model.entity.UserCourse;
import com.cgs.smartclass.service.UserCourseService;
import org.springframework.stereotype.Service;

/**
 * 用户课程服务实现
 */
@Service
public class UserCourseServiceImpl extends ServiceImpl<UserCourseMapper, UserCourse>
        implements UserCourseService {

}
