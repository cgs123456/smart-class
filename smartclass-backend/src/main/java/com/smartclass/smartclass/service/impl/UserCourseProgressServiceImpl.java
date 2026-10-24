package com.smartclass.smartclass.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclass.mapper.UserCourseProgressMapper;
import com.smartclass.smartclass.model.entity.UserCourseProgress;
import com.smartclass.smartclass.service.UserCourseProgressService;
import org.springframework.stereotype.Service;

/**
 * 用户课程学习进度服务实现
 */
@Service
public class UserCourseProgressServiceImpl extends ServiceImpl<UserCourseProgressMapper, UserCourseProgress>
        implements UserCourseProgressService {

}
