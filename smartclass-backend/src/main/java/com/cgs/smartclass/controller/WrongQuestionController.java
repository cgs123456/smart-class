package com.cgs.smartclass.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionAddRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionQueryRequest;
import com.cgs.smartclass.model.dto.wrongquestion.WrongQuestionReviewRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.WrongQuestionVO;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.service.WrongQuestionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 错题本接口
 */
@RestController
@RequestMapping("/api/wrong-question")
@Slf4j
public class WrongQuestionController {

    @Resource
    private WrongQuestionService wrongQuestionService;

    @Resource
    private UserService userService;

    /**
     * 添加错题
     */
    @PostMapping("/add")
    public BaseResponse<Long> addWrongQuestion(@RequestBody WrongQuestionAddRequest addRequest,
                                                HttpServletRequest request) {
        if (addRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        Long id = wrongQuestionService.addWrongQuestion(loginUser.getId(), addRequest);
        return ResultUtils.success(id);
    }

    /**
     * 分页查询我的错题
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<WrongQuestionVO>> listMyWrongQuestions(WrongQuestionQueryRequest queryRequest,
                                                                     HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<WrongQuestionVO> page = wrongQuestionService.listMyWrongQuestions(loginUser.getId(), queryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 根据ID获取错题
     */
    @GetMapping("/{id}")
    public BaseResponse<WrongQuestionVO> getWrongQuestionById(@PathVariable("id") Long id,
                                                               HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        WrongQuestionVO vo = wrongQuestionService.getWrongQuestionById(loginUser.getId(), id);
        return ResultUtils.success(vo);
    }

    /**
     * 复习错题
     */
    @PostMapping("/review")
    public BaseResponse<Boolean> reviewWrongQuestion(@RequestBody WrongQuestionReviewRequest reviewRequest,
                                                      HttpServletRequest request) {
        if (reviewRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = wrongQuestionService.reviewWrongQuestion(loginUser.getId(), reviewRequest);
        return ResultUtils.success(result);
    }

    /**
     * 获取需要复习的错题列表
     */
    @GetMapping("/review-list")
    public BaseResponse<List<WrongQuestionVO>> getReviewList(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        List<WrongQuestionVO> list = wrongQuestionService.getReviewList(loginUser.getId());
        return ResultUtils.success(list);
    }

    /**
     * 删除错题
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteWrongQuestion(@RequestBody Long id, HttpServletRequest request) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = wrongQuestionService.deleteWrongQuestion(loginUser.getId(), id);
        return ResultUtils.success(result);
    }
}
