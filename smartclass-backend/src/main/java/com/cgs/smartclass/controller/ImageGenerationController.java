package com.cgs.smartclass.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationQueryRequest;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.ImageGenerationVO;
import com.cgs.smartclass.service.ImageGenerationService;
import com.cgs.smartclass.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 图片生成接口
 */
@RestController
@RequestMapping("/api/image-generation")
@Slf4j
public class ImageGenerationController {

    @Resource
    private ImageGenerationService imageGenerationService;

    @Resource
    private UserService userService;

    /**
     * 生成图片
     */
    @PostMapping("/generate")
    public BaseResponse<ImageGenerationVO> generateImage(@RequestBody ImageGenerationRequest request,
                                                          HttpServletRequest httpRequest) {
        if (request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        ImageGenerationVO vo = imageGenerationService.generateImage(loginUser.getId(), request);
        return ResultUtils.success(vo);
    }

    /**
     * 分页查询我的图片生成记录
     */
    @GetMapping("/list/my")
    public BaseResponse<Page<ImageGenerationVO>> getMyGenerations(ImageGenerationQueryRequest queryRequest,
                                                                    HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<ImageGenerationVO> page = imageGenerationService.getMyGenerations(loginUser.getId(), queryRequest);
        return ResultUtils.success(page);
    }

    /**
     * 根据ID获取图片生成记录
     */
    @GetMapping("/{id}")
    public BaseResponse<ImageGenerationVO> getGenerationById(@PathVariable("id") Long id,
                                                               HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        ImageGenerationVO vo = imageGenerationService.getGenerationById(loginUser.getId(), id);
        return ResultUtils.success(vo);
    }

    /**
     * 删除图片生成记录
     */
    @PostMapping("/delete")
    public BaseResponse<Boolean> deleteGeneration(@RequestBody Long id, HttpServletRequest httpRequest) {
        if (id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = imageGenerationService.deleteGeneration(loginUser.getId(), id);
        return ResultUtils.success(result);
    }
}
