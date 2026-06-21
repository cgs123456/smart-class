package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.WrongQuestionMapper;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionAddRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionQueryRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionReviewRequest;
import com.cgs.smartclass.model.entity.WrongQuestion;
import com.cgs.smartclass.model.vo.WrongQuestionVO;
import com.cgs.smartclass.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 错题本服务实现
 */
@Service
@Slf4j
public class WrongQuestionServiceImpl extends ServiceImpl<WrongQuestionMapper, WrongQuestion>
        implements WrongQuestionService {

    /**
     * 艾宾浩斯遗忘曲线复习间隔（天）：1天→2天→4天→7天→15天→30天
     */
    private static final int[] REVIEW_INTERVALS = {1, 2, 4, 7, 15, 30};

    @Override
    public Long addWrongQuestion(Long userId, WrongQuestionAddRequest addRequest) {
        if (userId == null || addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(addRequest.getQuestionType())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目类型不能为空");
        }
        if (StringUtils.isBlank(addRequest.getQuestionContent())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "题目内容不能为空");
        }
        if (StringUtils.isBlank(addRequest.getCorrectAnswer())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "正确答案不能为空");
        }

        WrongQuestion wrongQuestion = new WrongQuestion();
        BeanUtils.copyProperties(addRequest, wrongQuestion);
        wrongQuestion.setUserId(userId);
        wrongQuestion.setMasteryLevel(0);
        wrongQuestion.setReviewCount(0);
        // 设置首次复习时间为1天后
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, REVIEW_INTERVALS[0]);
        wrongQuestion.setNextReviewTime(calendar.getTime());

        boolean result = this.save(wrongQuestion);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return wrongQuestion.getId();
    }

    @Override
    public Page<WrongQuestionVO> listMyWrongQuestions(Long userId, WrongQuestionQueryRequest queryRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq(StringUtils.isNotBlank(queryRequest.getQuestionType()),
                "questionType", queryRequest.getQuestionType());
        queryWrapper.eq(queryRequest.getMasteryLevel() != null,
                "masteryLevel", queryRequest.getMasteryLevel());
        queryWrapper.orderByDesc("createTime");

        Page<WrongQuestion> page = this.page(new Page<>(current, size), queryWrapper);
        Page<WrongQuestionVO> voPage = new Page<>(current, size, page.getTotal());
        List<WrongQuestionVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public WrongQuestionVO getWrongQuestionById(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WrongQuestion wrongQuestion = this.getById(id);
        if (wrongQuestion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!wrongQuestion.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return convertToVO(wrongQuestion);
    }

    @Override
    public boolean reviewWrongQuestion(Long userId, WrongQuestionReviewRequest reviewRequest) {
        if (userId == null || reviewRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (reviewRequest.getId() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "错题ID不能为空");
        }
        if (reviewRequest.getMasteryLevel() == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "掌握程度不能为空");
        }

        WrongQuestion wrongQuestion = this.getById(reviewRequest.getId());
        if (wrongQuestion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "错题不存在");
        }
        if (!wrongQuestion.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }

        // 更新掌握程度和复习次数
        wrongQuestion.setMasteryLevel(reviewRequest.getMasteryLevel());
        wrongQuestion.setReviewCount(wrongQuestion.getReviewCount() + 1);
        wrongQuestion.setLastReviewTime(new Date());

        // 基于艾宾浩斯遗忘曲线计算下次复习时间
        int reviewCount = wrongQuestion.getReviewCount();
        int intervalIndex = Math.min(reviewCount - 1, REVIEW_INTERVALS.length - 1);
        int intervalDays = REVIEW_INTERVALS[intervalIndex];
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.DAY_OF_MONTH, intervalDays);
        wrongQuestion.setNextReviewTime(calendar.getTime());

        // 如果已掌握，不再设置下次复习时间
        if (reviewRequest.getMasteryLevel() == 2) {
            wrongQuestion.setNextReviewTime(null);
        }

        return this.updateById(wrongQuestion);
    }

    @Override
    public List<WrongQuestionVO> getReviewList(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<WrongQuestion> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.le("nextReviewTime", new Date());
        queryWrapper.ne("masteryLevel", 2);
        queryWrapper.orderByAsc("nextReviewTime");

        List<WrongQuestion> list = this.list(queryWrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public boolean deleteWrongQuestion(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        WrongQuestion wrongQuestion = this.getById(id);
        if (wrongQuestion == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "错题不存在");
        }
        if (!wrongQuestion.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return this.removeById(id);
    }

    /**
     * 实体转VO
     */
    private WrongQuestionVO convertToVO(WrongQuestion wrongQuestion) {
        if (wrongQuestion == null) {
            return null;
        }
        WrongQuestionVO vo = new WrongQuestionVO();
        BeanUtils.copyProperties(wrongQuestion, vo);
        return vo;
    }
}
