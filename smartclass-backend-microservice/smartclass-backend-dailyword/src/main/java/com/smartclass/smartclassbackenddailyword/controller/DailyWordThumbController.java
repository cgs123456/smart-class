package com.smartclass.smartclassbackenddailyword.controller;

import com.smartclass.smartclassbackendserviceclient.utils.UserSessionUtils;


import com.smartclass.smartclassbackendcommon.common.BaseResponse;
import com.smartclass.smartclassbackendcommon.common.ErrorCode;
import com.smartclass.smartclassbackendcommon.common.ResultUtils;
import com.smartclass.smartclassbackendcommon.exception.BusinessException;
import com.smartclass.smartclassbackenddailyword.service.DailyWordThumbService;
import com.smartclass.smartclassbackendmodel.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 每日单词点赞接口
 */
@RestController
@RequestMapping("/daily-word-thumbs")
@Slf4j
public class DailyWordThumbController {

    @Resource
    private DailyWordThumbService dailyWordThumbService;



    /**
     * 点赞单词
     *
     * @param wordId 单词ID
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 1-点赞成功；0-操作失败
     */
    @PostMapping("/{wordId}")
    public BaseResponse<Integer> thumbWord(@PathVariable("wordId") long wordId,
                                           HttpServletRequest request) {
        if (wordId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从 Dubbo 获取登录用户
        User loginUser = UserSessionUtils.getLoginUser(request);
        // 执行点赞操作
        int result = dailyWordThumbService.thumbWord(wordId, loginUser);
        return ResultUtils.success(result);
    }
    
    
/**
     * 取消点赞单词
     *
     * @param wordId 单词ID
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 1-取消点赞成功；0-操作失败
     */
    @DeleteMapping("/{wordId}")
    public BaseResponse<Integer> cancelThumbWord(@PathVariable("wordId") long wordId,
                                                HttpServletRequest request) {
        if (wordId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从 Dubbo 获取登录用户
        User loginUser = UserSessionUtils.getLoginUser(request);
        // 执行取消点赞操作
        int result = dailyWordThumbService.cancelThumbWord(wordId, loginUser.getId());
        return ResultUtils.success(result);
    }
    
    /**
     * 查询当前用户是否点赞了单词
     *
     * @param wordId 单词ID
     * @param request HTTP请求对象，用于获取当前登录用户信息
     * @return 是否点赞
     */
    @GetMapping("/{wordId}/status")
    public BaseResponse<Boolean> isThumbWord(@PathVariable("wordId") long wordId,
                                              HttpServletRequest request) {
        if (wordId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 从 Dubbo 获取登录用户
        User loginUser = UserSessionUtils.getLoginUser(request);
        // 查询是否点赞
        boolean result = dailyWordThumbService.isThumbWord(wordId, loginUser.getId());
        return ResultUtils.success(result);
    }
} 