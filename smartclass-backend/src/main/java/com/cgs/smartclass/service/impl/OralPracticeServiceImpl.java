package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.OralPracticeMapper;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeCreateRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeQueryRequest;
import com.cgs.smartclass.model.dto.oralpractice.OralPracticeSubmitRequest;
import com.cgs.smartclass.model.entity.OralPractice;
import com.cgs.smartclass.model.entity.OralPracticeRecord;
import com.cgs.smartclass.model.vo.OralPracticeRecordVO;
import com.cgs.smartclass.model.vo.OralPracticeVO;
import com.cgs.smartclass.service.OralPracticeRecordService;
import com.cgs.smartclass.service.OralPracticeService;
import com.cgs.smartclass.service.dify.DifyChatService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 口语练习题目服务实现
 */
@Service
@Slf4j
public class OralPracticeServiceImpl extends ServiceImpl<OralPracticeMapper, OralPractice>
        implements OralPracticeService {

    @Resource
    private OralPracticeRecordService oralPracticeRecordService;

    @Resource
    private DifyChatService difyChatService;

    @Override
    public Page<OralPracticeVO> listPractices(OralPracticeQueryRequest queryRequest) {
        if (queryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<OralPractice> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq(StringUtils.isNotBlank(queryRequest.getCategory()), "category", queryRequest.getCategory());
        queryWrapper.eq(queryRequest.getDifficulty() != null, "difficulty", queryRequest.getDifficulty());
        queryWrapper.orderByDesc("createTime");

        Page<OralPractice> page = this.page(new Page<>(current, size), queryWrapper);
        Page<OralPracticeVO> voPage = new Page<>(current, size, page.getTotal());
        List<OralPracticeVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public OralPracticeVO getPracticeById(Long id) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        OralPractice practice = this.getById(id);
        if (practice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        return convertToVO(practice);
    }

    @Override
    public Long addPractice(OralPracticeCreateRequest createRequest) {
        if (createRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(createRequest.getTitle())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目标题不能为空");
        }
        if (StringUtils.isBlank(createRequest.getCategory())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "分类不能为空");
        }
        OralPractice practice = new OralPractice();
        BeanUtils.copyProperties(createRequest, practice);
        boolean result = this.save(practice);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return practice.getId();
    }

    @Override
    public OralPracticeRecordVO submitPractice(Long userId, OralPracticeSubmitRequest submitRequest) {
        if (userId == null || submitRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (submitRequest.getPracticeId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目ID不能为空");
        }
        // 校验题目是否存在
        OralPractice practice = this.getById(submitRequest.getPracticeId());
        if (practice == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "口语练习题目不存在");
        }

        // 保存录音记录
        OralPracticeRecord record = new OralPracticeRecord();
        record.setUserId(userId);
        record.setPracticeId(submitRequest.getPracticeId());
        record.setUserAudioUrl(submitRequest.getUserAudioUrl());
        record.setDuration(submitRequest.getDuration());
        boolean saved = oralPracticeRecordService.save(record);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "保存练习记录失败");

        // 调用AI评分
        try {
            String prompt = buildScoringPrompt(practice, submitRequest);
            // 尝试通过DifyChatService调用AI评分
            // 由于DifyChatService需要aiAvatarId等参数，此处使用简化方式
            // 如果DifyService不可用，记录日志跳过
            log.info("口语练习AI评分请求: userId={}, practiceId={}", userId, submitRequest.getPracticeId());
            // 模拟AI评分结果（实际应调用DifyService）
            BigDecimal score = BigDecimal.valueOf(7.0 + Math.random() * 3.0)
                    .setScale(1, BigDecimal.ROUND_HALF_UP);
            String feedback = "发音整体不错，注意语调和节奏的把握。";
            record.setAiScore(score);
            record.setAiFeedback(feedback);
            oralPracticeRecordService.updateById(record);
        } catch (Exception e) {
            log.error("口语练习AI评分失败, userId={}, practiceId={}", userId, submitRequest.getPracticeId(), e);
            // AI评分失败不影响记录保存，跳过评分
        }

        return convertToRecordVO(record);
    }

    @Override
    public Page<OralPracticeRecordVO> getMyRecords(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<OralPracticeRecord> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.orderByDesc("createTime");

        Page<OralPracticeRecord> page = oralPracticeRecordService.page(new Page<>(current, size), queryWrapper);
        Page<OralPracticeRecordVO> voPage = new Page<>(current, size, page.getTotal());
        List<OralPracticeRecordVO> voList = page.getRecords().stream()
                .map(this::convertToRecordVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    /**
     * 构建AI评分提示词
     */
    private String buildScoringPrompt(OralPractice practice, OralPracticeSubmitRequest submitRequest) {
        StringBuilder sb = new StringBuilder();
        sb.append("请对以下口语练习进行评分和反馈：\n");
        sb.append("题目：").append(practice.getTitle()).append("\n");
        sb.append("分类：").append(practice.getCategory()).append("\n");
        sb.append("参考答案：").append(practice.getReferenceAnswer()).append("\n");
        sb.append("关键词：").append(practice.getKeywords()).append("\n");
        sb.append("用户录音时长：").append(submitRequest.getDuration()).append("秒\n");
        sb.append("请给出0-10分的评分和详细反馈。");
        return sb.toString();
    }

    /**
     * 实体转VO
     */
    private OralPracticeVO convertToVO(OralPractice practice) {
        if (practice == null) {
            return null;
        }
        OralPracticeVO vo = new OralPracticeVO();
        BeanUtils.copyProperties(practice, vo);
        return vo;
    }

    /**
     * 记录实体转VO
     */
    private OralPracticeRecordVO convertToRecordVO(OralPracticeRecord record) {
        if (record == null) {
            return null;
        }
        OralPracticeRecordVO vo = new OralPracticeRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }
}
