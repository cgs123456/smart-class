package com.smartclass.smartclassbackendintelligence.controller;

import org.apache.dubbo.config.annotation.DubboReference;

import com.smartclass.smartclassbackendserviceclient.dubbo.UserDubboService;

import com.smartclass.smartclassbackendserviceclient.utils.UserSessionUtils;

import com.smartclass.smartclassbackendcommon.annotation.AuthCheck;
import com.smartclass.smartclassbackendcommon.common.BaseResponse;
import com.smartclass.smartclassbackendcommon.common.ErrorCode;
import com.smartclass.smartclassbackendcommon.common.ResultUtils;
import com.smartclass.smartclassbackendintelligence.service.ExerciseGenerationService;
import com.smartclass.smartclassbackendmodel.model.dto.exercise.ExerciseGenerateRequest;
import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendmodel.model.entity.WrongQuestion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

import static com.smartclass.smartclassbackendcommon.constant.UserConstant.ADMIN_ROLE;

/**
 * AI 习题生成接口
 */
@RestController
@RequestMapping("/api/exercise")
@Slf4j
public class ExerciseGenerationController {


    @Resource
    private ExerciseGenerationService exerciseGenerationService;


    /**
     * 生成 AI 习题（仅管理员）
     *
     * @param request     请求体
     * @param httpRequest HTTP 请求
     * @return 生成的题目数量
     */
    @PostMapping("/generate")
    @AuthCheck(mustRole = ADMIN_ROLE)
    public BaseResponse<Integer> generateExercises(@RequestBody ExerciseGenerateRequest request,
                                                   HttpServletRequest httpRequest) {
        if (request == null || request.getCourseId() == null || request.getCourseId() <= 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = UserSessionUtils.getLoginUser(httpRequest);
        int questionCount = request.getQuestionCount() == null ? 0 : request.getQuestionCount();
        int count = exerciseGenerationService.generateExercises(
                request.getCourseId(), request.getChapterId(), questionCount, loginUser.getId());
        return ResultUtils.success(count);
    }

    /**
     * 获取课程（或章节）下的 AI 习题列表
     *
     * @param courseId  课程ID
     * @param chapterId 章节ID（可选）
     * @return AI 习题列表
     */
    @GetMapping("/list")
    public BaseResponse<List<WrongQuestion>> getCourseExercises(@RequestParam Long courseId,
                                                                @RequestParam(required = false) Long chapterId) {
        if (courseId == null || courseId <= 0) {
            return ResultUtils.error(ErrorCode.PARAMS_ERROR);
        }
        List<WrongQuestion> exercises = exerciseGenerationService.getCourseExercises(courseId, chapterId);
        return ResultUtils.success(exercises);
    }
}
