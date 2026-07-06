package com.cgs.smartclass.controller;

import com.cgs.smartclass.annotation.AuthCheck;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.constant.UserConstant;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.model.dto.exercise.ExerciseGenerateRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.entity.WrongQuestion;
import com.cgs.smartclass.model.vo.WrongQuestionVO;
import com.cgs.smartclass.service.ExerciseGenerationService;
import com.cgs.smartclass.service.UserService;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * AI 习题生成接口
 */
@RestController
@RequestMapping("/api/exercise")
@Slf4j
public class ExerciseGenerationController {

    @Resource
    private ExerciseGenerationService exerciseGenerationService;

    @Resource
    private UserService userService;

    /**
     * 管理员生成课程习题
     */
    @PostMapping("/generate")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Integer> generateExercises(@RequestBody ExerciseGenerateRequest request,
                                                   HttpServletRequest httpRequest) {
        if (request == null || request.getCourseId() == null || request.getCourseId() <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        int questionCount = request.getQuestionCount() != null && request.getQuestionCount() > 0
                ? request.getQuestionCount() : 5;
        ThrowUtils.throwIf(questionCount > 20, ErrorCode.PARAMS_ERROR, "单次生成题目数量不能超过20");

        User loginUser = userService.getLoginUser(httpRequest);
        int count = exerciseGenerationService.generateExercises(
                request.getCourseId(), request.getChapterId(), questionCount, loginUser.getId());
        return ResultUtils.success(count);
    }

    /**
     * 获取课程的 AI 习题列表
     */
    @GetMapping("/list")
    public BaseResponse<List<WrongQuestionVO>> getCourseExercises(@RequestParam Long courseId,
                                                                  @RequestParam(required = false) Long chapterId) {
        if (courseId == null || courseId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "课程ID不能为空");
        }
        List<WrongQuestion> list = exerciseGenerationService.getCourseExercises(courseId, chapterId);
        List<WrongQuestionVO> voList = list.stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        return ResultUtils.success(voList);
    }

    /**
     * 实体转 VO
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
