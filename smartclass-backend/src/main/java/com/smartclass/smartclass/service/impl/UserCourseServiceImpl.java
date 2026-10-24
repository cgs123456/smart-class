package com.smartclass.smartclass.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclass.mapper.UserCourseMapper;
import com.smartclass.smartclass.model.entity.UserCourse;
import com.smartclass.smartclass.service.UserCourseService;
import org.springframework.stereotype.Service;

/**
 * 用户课程服务实现
 */
@Service
public class UserCourseServiceImpl extends ServiceImpl<UserCourseMapper, UserCourse>
        implements UserCourseService {

}
