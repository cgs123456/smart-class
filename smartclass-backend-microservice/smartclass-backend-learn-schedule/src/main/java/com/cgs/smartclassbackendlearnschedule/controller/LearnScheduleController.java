package com.cgs.smartclassbackendlearnschedule.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclassbackendcommon.common.BaseResponse;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendcommon.common.ResultUtils;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleCreateRequest;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleUpdateRequest;
import com.cgs.smartclassbackendmodel.model.entity.User;
import com.cgs.smartclassbackendmodel.model.vo.LearnScheduleVO;
import com.cgs.smartclassbackendlearnschedule.service.LearnScheduleService;
import com.cgs.smartclassbackendserviceclient.service.UserFeignClient;
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

    @Resource
    private UserFeignClient userService;

    /**
     * 创建学习计划
     */
    @PostMapping("/create")
    public BaseResponse<LearnScheduleVO> createSchedule(@RequestBody LearnScheduleCreateRequest request,
                                                         HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        LearnScheduleVO vo = learnScheduleService.createSchedule(loginUser.getId(), request);
        return ResultUtils.success(vo);
    }

    /**
     * 我的学习计划列表
     */
    @GetMapping("/my")
    public BaseResponse<Page<LearnScheduleVO>> getMySchedules(PageRequest pageRequest,
                                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<LearnScheduleVO> page = learnScheduleService.getMySchedules(loginUser.getId(), pageRequest);
        return ResultUtils.success(page);
    }

    /**
     * 学习计划详情
     */
    @GetMapping("/{id}")
    public BaseResponse<LearnScheduleVO> getScheduleById(@PathVariable Long id,
                                                          HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        LearnScheduleVO vo = learnScheduleService.getScheduleById(loginUser.getId(), id);
        return ResultUtils.success(vo);
    }

    /**
     * 更新学习计划
     */
    @PostMapping("/update")
    public BaseResponse<Boolean> updateSchedule(@RequestBody LearnScheduleUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = learnScheduleService.updateSchedule(loginUser.getId(), request);
        return ResultUtils.success(result);
    }

    /**
     * 删除学习计划
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteSchedule(@RequestBody LearnScheduleUpdateRequest request,
                                                 HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = learnScheduleService.deleteSchedule(loginUser.getId(), request.getId());
        return ResultUtils.success(result);
    }
}
