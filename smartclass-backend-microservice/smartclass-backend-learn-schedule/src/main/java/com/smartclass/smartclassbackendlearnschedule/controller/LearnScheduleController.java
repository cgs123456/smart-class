package com.smartclass.smartclassbackendlearnschedule.controller;

import org.apache.dubbo.config.annotation.DubboReference;

import com.smartclass.smartclassbackendserviceclient.dubbo.UserDubboService;

import com.smartclass.smartclassbackendserviceclient.utils.UserSessionUtils;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartclass.smartclassbackendcommon.common.BaseResponse;
import com.smartclass.smartclassbackendcommon.common.PageRequest;
import com.smartclass.smartclassbackendcommon.common.ResultUtils;
import com.smartclass.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleCreateRequest;
import com.smartclass.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleUpdateRequest;
import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendmodel.model.vo.LearnScheduleVO;
import com.smartclass.smartclassbackendlearnschedule.service.LearnScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 学习计划接口
 */
@RestController
@RequestMapping("/learn-schedule")
@Slf4j
public class LearnScheduleController {


    @Resource
    private LearnScheduleService learnScheduleService;


    /**
     * 创建学习计划
     */
    @PostMapping("/create")
    public BaseResponse<LearnScheduleVO> createSchedule(@RequestBody LearnScheduleCreateRequest request,
                                                         HttpServletRequest httpRequest) {
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        LearnScheduleVO vo = learnScheduleService.createSchedule(loginUser.getId(), request);
        return ResultUtils.success(vo);
    }

    /**
     * 我的学习计划列表
     */
    @GetMapping("/my")
    public BaseResponse<Page<LearnScheduleVO>> getMySchedules(PageRequest pageRequest,
                                                               HttpServletRequest httpRequest) {
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        Page<LearnScheduleVO> page = learnScheduleService.getMySchedules(loginUser.getId(), pageRequest);
        return ResultUtils.success(page);
    }

    /**
     * 学习计划详情
     */
    @GetMapping("/{id}")
    public BaseResponse<LearnScheduleVO> getScheduleById(@PathVariable Long id,
                                                          HttpServletRequest httpRequest) {
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        LearnScheduleVO vo = learnScheduleService.getScheduleById(loginUser.getId(), id);
        return ResultUtils.success(vo);
    }

    /**
     * 更新学习计划
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSchedule(@RequestBody LearnScheduleUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        boolean result = learnScheduleService.updateSchedule(loginUser.getId(), request);
        return ResultUtils.success(result);
    }

    /**
     * 删除学习计划
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSchedule(@RequestBody LearnScheduleUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        boolean result = learnScheduleService.deleteSchedule(loginUser.getId(), request.getId());
        return ResultUtils.success(result);
    }
}
