package com.cgs.smartclassbackendlearnschedule.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleCreateRequest;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleUpdateRequest;
import com.cgs.smartclassbackendmodel.model.entity.LearnSchedule;
import com.cgs.smartclassbackendmodel.model.vo.LearnScheduleVO;

/**
 * 学习计划服务
 */
public interface LearnScheduleService extends IService<LearnSchedule> {

    /**
     * 创建学习计划
     */
    LearnScheduleVO createSchedule(Long userId, LearnScheduleCreateRequest request);

    /**
     * 获取我的学习计划列表
     */
    Page<LearnScheduleVO> getMySchedules(Long userId, PageRequest pageRequest);

    /**
     * 获取学习计划详情
     */
    LearnScheduleVO getScheduleById(Long userId, Long id);

    /**
     * 更新学习计划
     */
    boolean updateSchedule(Long userId, LearnScheduleUpdateRequest request);

    /**
     * 删除学习计划
     */
    boolean deleteSchedule(Long userId, Long id);
}
