package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.ImageGenerationMapper;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationQueryRequest;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationRequest;
import com.cgs.smartclass.model.entity.ImageGeneration;
import com.cgs.smartclass.model.vo.ImageGenerationVO;
import com.cgs.smartclass.service.ImageGenerationService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 图片生成服务实现
 */
@Service
@Slf4j
public class ImageGenerationServiceImpl extends ServiceImpl<ImageGenerationMapper, ImageGeneration>
        implements ImageGenerationService {

    @Override
    public ImageGenerationVO generateImage(Long userId, ImageGenerationRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        if (StringUtils.isBlank(request.getPrompt())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "提示词不能为空");
        }

        // 创建记录（状态=生成中）
        ImageGeneration imageGeneration = new ImageGeneration();
        imageGeneration.setUserId(userId);
        imageGeneration.setPrompt(request.getPrompt());
        imageGeneration.setNegativePrompt(request.getNegativePrompt());
        imageGeneration.setStyle(request.getStyle());
        imageGeneration.setSize(StringUtils.isBlank(request.getSize()) ? "512x512" : request.getSize());
        imageGeneration.setStatus(0);
        boolean saved = this.save(imageGeneration);
        ThrowUtils.throwIf(!saved, ErrorCode.OPERATION_ERROR, "创建图片生成记录失败");

        // 调用图片生成API（目前模拟实现，返回占位URL）
        try {
            log.info("图片生成请求: userId={}, prompt={}", userId, request.getPrompt());
            // 模拟生成过程
            String placeholderUrl = "https://placeholder.com/512x512?text=" + UUID.randomUUID().toString().substring(0, 8);
            imageGeneration.setImageUrl(placeholderUrl);
            imageGeneration.setStatus(1);
            this.updateById(imageGeneration);
        } catch (Exception e) {
            log.error("图片生成失败, userId={}", userId, e);
            imageGeneration.setStatus(2);
            imageGeneration.setErrorMessage("图片生成失败: " + e.getMessage());
            this.updateById(imageGeneration);
        }

        return convertToVO(imageGeneration);
    }

    @Override
    public Page<ImageGenerationVO> getMyGenerations(Long userId, ImageGenerationQueryRequest queryRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        QueryWrapper<ImageGeneration> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.eq("isDelete", 0);
        queryWrapper.eq(queryRequest.getStatus() != null, "status", queryRequest.getStatus());
        queryWrapper.orderByDesc("createTime");

        Page<ImageGeneration> page = this.page(new Page<>(current, size), queryWrapper);
        Page<ImageGenerationVO> voPage = new Page<>(current, size, page.getTotal());
        List<ImageGenerationVO> voList = page.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public ImageGenerationVO getGenerationById(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ImageGeneration imageGeneration = this.getById(id);
        if (imageGeneration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR);
        }
        if (!imageGeneration.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return convertToVO(imageGeneration);
    }

    @Override
    public boolean deleteGeneration(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        ImageGeneration imageGeneration = this.getById(id);
        if (imageGeneration == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "图片生成记录不存在");
        }
        if (!imageGeneration.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return this.removeById(id);
    }

    /**
     * 实体转VO
     */
    private ImageGenerationVO convertToVO(ImageGeneration imageGeneration) {
        if (imageGeneration == null) {
            return null;
        }
        ImageGenerationVO vo = new ImageGenerationVO();
        BeanUtils.copyProperties(imageGeneration, vo);
        return vo;
    }
}
