package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.mapper.UserCourseProgressMapper;
import com.cgs.smartclass.model.entity.UserCourseProgress;
import com.cgs.smartclass.service.UserCourseProgressService;
import org.springframework.stereotype.Service;

/**
 * 用户课程学习进度服务实现
 */
@Service
public class UserCourseProgressServiceImpl extends ServiceImpl<UserCourseProgressMapper, UserCourseProgress>
        implements UserCourseProgressService {

}
