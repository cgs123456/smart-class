package com.smartclass.smartclassbackendlearnschedule.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclassbackendmodel.model.entity.LearnScheduleItem;
import com.smartclass.smartclassbackendlearnschedule.mapper.LearnScheduleItemMapper;
import com.smartclass.smartclassbackendlearnschedule.service.LearnScheduleItemService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 学习计划项服务实现
 */
@Service
@Slf4j
public class LearnScheduleItemServiceImpl extends ServiceImpl<LearnScheduleItemMapper, LearnScheduleItem>
        implements LearnScheduleItemService {

}
