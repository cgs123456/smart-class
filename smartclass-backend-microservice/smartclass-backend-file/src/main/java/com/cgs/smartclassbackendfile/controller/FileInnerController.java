package com.cgs.smartclassbackendfile.controller;

import com.cgs.smartclassbackendcommon.common.BaseResponse;
import com.cgs.smartclassbackendcommon.common.ErrorCode;
import com.cgs.smartclassbackendcommon.common.ResultUtils;
import com.cgs.smartclassbackendcommon.exception.BusinessException;
import com.cgs.smartclassbackendfile.manager.CosManager;
import com.cgs.smartclassbackendmodel.model.enums.FileUploadBizEnum;
import com.cgs.smartclassbackendserviceclient.service.FileFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.Resource;

/**
 * 文件内部接口（供其他服务调用）
 */
@RestController
@RequestMapping("/inner")
@Slf4j
public class FileInnerController implements FileFeignClient {

    @Resource
    private CosManager cosManager;

    /**
     * 通用文件上传
     *
     * @param file 上传的文件
     * @return 文件访问URL
     */
    @Override
    @PostMapping("/upload")
    public BaseResponse<String> uploadFile(@RequestPart("file") MultipartFile file) {
        // 直接使用通用文件类型
        FileUploadBizEnum fileUploadBizEnum = FileUploadBizEnum.GENERAL;
        
        try {
            String url = cosManager.uploadFile(file, fileUploadBizEnum);
            return ResultUtils.success(url);
        } catch (Exception e) {
            log.error("文件上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传头像
     *
     * @param file 头像文件
     * @return 头像URL
     */
    @Override
    @PostMapping("/upload/avatar")
    public BaseResponse<String> uploadAvatar(@RequestPart("file") MultipartFile file) {
        try {
            String url = cosManager.uploadFile(file, FileUploadBizEnum.USER_AVATAR);
            return ResultUtils.success(url);
        } catch (Exception e) {
            log.error("头像上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传视频
     *
     * @param file 视频文件
     * @return 视频URL
     */
    @Override
    @PostMapping("/upload/video")
    public BaseResponse<String> uploadVideo(@RequestPart("file") MultipartFile file) {
        try {
            String url = cosManager.uploadFile(file, FileUploadBizEnum.VIDEO);
            return ResultUtils.success(url);
        } catch (Exception e) {
            log.error("视频上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传文档
     *
     * @param file 文档文件
     * @return 文档URL
     */
    @Override
    @PostMapping("/upload/document")
    public BaseResponse<String> uploadDocument(@RequestPart("file") MultipartFile file) {
        try {
            String url = cosManager.uploadFile(file, FileUploadBizEnum.DOCUMENT);
            return ResultUtils.success(url);
        } catch (Exception e) {
            log.error("文档上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }
    }

    /**
     * 上传课程资料
     *
     * @param file 课程资料文件
     * @return 资料URL
     */
    @Override
    @PostMapping("/upload/material")
    public BaseResponse<String> uploadMaterial(@RequestPart("file") MultipartFile file) {
        try {
            String url = cosManager.uploadFile(file, FileUploadBizEnum.MATERIAL);
            return ResultUtils.success(url);
        } catch (Exception e) {
            log.error("课程资料上传失败", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "上传失败: " + e.getMessage());
        }
    }
} 