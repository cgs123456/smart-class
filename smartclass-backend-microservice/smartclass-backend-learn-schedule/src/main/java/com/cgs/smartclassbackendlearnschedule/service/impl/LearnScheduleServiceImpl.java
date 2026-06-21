package com.cgs.smartclassbackendlearnschedule.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclassbackendcommon.common.ErrorCode;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendcommon.exception.BusinessException;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleCreateRequest;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleItemDTO;
import com.cgs.smartclassbackendmodel.model.dto.learnschedule.LearnScheduleUpdateRequest;
import com.cgs.smartclassbackendmodel.model.entity.LearnSchedule;
import com.cgs.smartclassbackendmodel.model.entity.LearnScheduleItem;
import com.cgs.smartclassbackendmodel.model.vo.LearnScheduleItemVO;
import com.cgs.smartclassbackendmodel.model.vo.LearnScheduleVO;
import com.cgs.smartclassbackendlearnschedule.mapper.LearnScheduleMapper;
import com.cgs.smartclassbackendlearnschedule.service.LearnScheduleItemService;
import com.cgs.smartclassbackendlearnschedule.service.LearnScheduleService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import jakarta.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学习计划服务实现
 */
@Service
@Slf4j
public class LearnScheduleServiceImpl extends ServiceImpl<LearnScheduleMapper, LearnSchedule>
        implements LearnScheduleService {

    @Resource
    private LearnScheduleItemService learnScheduleItemService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public LearnScheduleVO createSchedule(Long userId, LearnScheduleCreateRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(request.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "计划标题不能为空");
        }
        if (request.getStartDate() == null || request.getEndDate() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "开始和结束日期不能为空");
        }
        // 创建计划
        LearnSchedule schedule = new LearnSchedule();
        schedule.setUserId(userId);
        schedule.setTitle(request.getTitle());
        schedule.setDescription(request.getDescription());
        schedule.setStartDate(request.getStartDate());
        schedule.setEndDate(request.getEndDate());
        schedule.setStatus(0);
        schedule.setDailyMinutes(request.getDailyMinutes() != null ? request.getDailyMinutes() : 30);
        boolean result = this.save(schedule);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建学习计划失败");
        }
        // 创建计划项
        if (CollectionUtils.isNotEmpty(request.getItems())) {
            List<LearnScheduleItem> items = new ArrayList<>();
            for (LearnScheduleItemDTO itemDTO : request.getItems()) {
                LearnScheduleItem item = new LearnScheduleItem();
                item.setScheduleId(schedule.getId());
                item.setCourseId(itemDTO.getCourseId());
                item.setDailyWordCount(itemDTO.getDailyWordCount() != null ? itemDTO.getDailyWordCount() : 0);
                item.setDailyArticleCount(itemDTO.getDailyArticleCount() != null ? itemDTO.getDailyArticleCount() : 0);
                item.setDayOfWeek(itemDTO.getDayOfWeek());
                item.setTimeSlot(itemDTO.getTimeSlot());
                items.add(item);
            }
            learnScheduleItemService.saveBatch(items);
        }
        return convertToVO(schedule);
    }

    @Override
    public Page<LearnScheduleVO> getMySchedules(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        QueryWrapper<LearnSchedule> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<LearnSchedule> schedulePage = this.page(new Page<>(current, size), queryWrapper);
        Page<LearnScheduleVO> voPage = new Page<>(current, size, schedulePage.getTotal());
        List<LearnScheduleVO> voList = schedulePage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public LearnScheduleVO getScheduleById(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LearnSchedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习计划不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return convertToVO(schedule);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean updateSchedule(Long userId, LearnScheduleUpdateRequest request) {
        if (userId == null || request == null || request.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LearnSchedule schedule = this.getById(request.getId());
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习计划不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 更新计划
        if (StringUtils.isNotBlank(request.getTitle())) {
            schedule.setTitle(request.getTitle());
        }
        if (request.getDescription() != null) {
            schedule.setDescription(request.getDescription());
        }
        if (request.getStartDate() != null) {
            schedule.setStartDate(request.getStartDate());
        }
        if (request.getEndDate() != null) {
            schedule.setEndDate(request.getEndDate());
        }
        if (request.getStatus() != null) {
            schedule.setStatus(request.getStatus());
        }
        if (request.getDailyMinutes() != null) {
            schedule.setDailyMinutes(request.getDailyMinutes());
        }
        boolean result = this.updateById(schedule);
        // 更新计划项（先删除旧的再添加新的）
        if (CollectionUtils.isNotEmpty(request.getItems())) {
            QueryWrapper<LearnScheduleItem> deleteWrapper = new QueryWrapper<>();
            deleteWrapper.eq("scheduleId", request.getId());
            learnScheduleItemService.remove(deleteWrapper);
            List<LearnScheduleItem> items = new ArrayList<>();
            for (LearnScheduleItemDTO itemDTO : request.getItems()) {
                LearnScheduleItem item = new LearnScheduleItem();
                item.setScheduleId(request.getId());
                item.setCourseId(itemDTO.getCourseId());
                item.setDailyWordCount(itemDTO.getDailyWordCount() != null ? itemDTO.getDailyWordCount() : 0);
                item.setDailyArticleCount(itemDTO.getDailyArticleCount() != null ? itemDTO.getDailyArticleCount() : 0);
                item.setDayOfWeek(itemDTO.getDayOfWeek());
                item.setTimeSlot(itemDTO.getTimeSlot());
                items.add(item);
            }
            learnScheduleItemService.saveBatch(items);
        }
        return result;
    }

    @Override
    public boolean deleteSchedule(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        LearnSchedule schedule = this.getById(id);
        if (schedule == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "学习计划不存在");
        }
        if (!schedule.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        // 删除计划项
        QueryWrapper<LearnScheduleItem> deleteWrapper = new QueryWrapper<>();
        deleteWrapper.eq("scheduleId", id);
        learnScheduleItemService.remove(deleteWrapper);
        return this.removeById(id);
    }

    /**
     * 转换为VO
     */
    private LearnScheduleVO convertToVO(LearnSchedule schedule) {
        if (schedule == null) {
            return null;
        }
        LearnScheduleVO vo = new LearnScheduleVO();
        BeanUtils.copyProperties(schedule, vo);
        // 查询计划项
        QueryWrapper<LearnScheduleItem> itemQueryWrapper = new QueryWrapper<>();
        itemQueryWrapper.eq("scheduleId", schedule.getId());
        List<LearnScheduleItem> items = learnScheduleItemService.list(itemQueryWrapper);
        List<LearnScheduleItemVO> itemVOs = items.stream().map(item -> {
            LearnScheduleItemVO itemVO = new LearnScheduleItemVO();
            BeanUtils.copyProperties(item, itemVO);
            return itemVO;
        }).collect(Collectors.toList());
        vo.setItems(itemVOs);
        return vo;
    }
}
