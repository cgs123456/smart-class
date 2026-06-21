package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationQueryRequest;
import com.cgs.smartclass.model.dto.imagegeneration.ImageGenerationRequest;
import com.cgs.smartclass.model.entity.ImageGeneration;
import com.cgs.smartclass.model.vo.ImageGenerationVO;

/**
 * 图片生成服务
 */
public interface ImageGenerationService extends IService<ImageGeneration> {

    /**
     * 生成图片
     *
     * @param userId  用户ID
     * @param request 生成请求
     * @return 生成记录视图
     */
    ImageGenerationVO generateImage(Long userId, ImageGenerationRequest request);

    /**
     * 分页查询我的图片生成记录
     *
     * @param userId       用户ID
     * @param queryRequest 查询请求
     * @return 分页结果
     */
    Page<ImageGenerationVO> getMyGenerations(Long userId, ImageGenerationQueryRequest queryRequest);

    /**
     * 根据ID获取图片生成记录
     *
     * @param userId 用户ID
     * @param id     记录ID
     * @return 生成记录视图
     */
    ImageGenerationVO getGenerationById(Long userId, Long id);

    /**
     * 删除图片生成记录
     *
     * @param userId 用户ID
     * @param id     记录ID
     * @return 是否成功
     */
    boolean deleteGeneration(Long userId, Long id);
}
