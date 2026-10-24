package com.smartclass.smartclass.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.smartclass.smartclass.common.BaseResponse;
import com.smartclass.smartclass.common.ErrorCode;
import com.smartclass.smartclass.common.PageRequest;
import com.smartclass.smartclass.common.ResultUtils;
import com.smartclass.smartclass.exception.BusinessException;
import com.smartclass.smartclass.model.dto.ebook.EbookReadingUpdateRequest;
import com.smartclass.smartclass.model.entity.User;
import com.smartclass.smartclass.model.vo.EbookReadingRecordVO;
import com.smartclass.smartclass.service.EbookReadingRecordService;
import com.smartclass.smartclass.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 电子书阅读记录接口
 */
@RestController
@RequestMapping("/api/ebook/reading")
@Slf4j
public class EbookReadingController {

    @Resource
    private EbookReadingRecordService ebookReadingRecordService;

    @Resource
    private UserService userService;

    /**
     * 获取我的电子书阅读记录
     */
    @GetMapping("/my")
    public BaseResponse<Page<EbookReadingRecordVO>> getMyBooks(PageRequest pageRequest,
                                                                HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        Page<EbookReadingRecordVO> page = ebookReadingRecordService.getMyBooks(loginUser.getId(), pageRequest);
        return ResultUtils.success(page);
    }

    /**
     * 更新阅读进度
     */
    @PostMapping("/progress")
    public BaseResponse<Boolean> updateProgress(@RequestBody EbookReadingUpdateRequest updateRequest,
                                                  HttpServletRequest request) {
        if (updateRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = ebookReadingRecordService.updateProgress(loginUser.getId(), updateRequest);
        return ResultUtils.success(result);
    }

    /**
     * 切换收藏状态
     */
    @PostMapping("/favorite/{ebookId}")
    public BaseResponse<Boolean> toggleFavorite(@PathVariable("ebookId") Long ebookId,
                                                  HttpServletRequest request) {
        if (ebookId == null || ebookId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        boolean result = ebookReadingRecordService.toggleFavorite(loginUser.getId(), ebookId);
        return ResultUtils.success(result);
    }

    /**
     * 获取阅读记录
     */
    @GetMapping("/{ebookId}")
    public BaseResponse<EbookReadingRecordVO> getReadingRecord(@PathVariable("ebookId") Long ebookId,
                                                                 HttpServletRequest request) {
        if (ebookId == null || ebookId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(request);
        EbookReadingRecordVO vo = ebookReadingRecordService.getReadingRecord(loginUser.getId(), ebookId);
        return ResultUtils.success(vo);
    }
}
