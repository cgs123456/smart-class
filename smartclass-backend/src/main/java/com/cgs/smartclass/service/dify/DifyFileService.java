package com.cgs.smartclass.service.dify;

/**
 * Dify文件服务接口
 * 负责文件上传到Dify
 */
public interface DifyFileService {

    /**
     * 上传文件到Dify
     *
     * @param userId     用户ID
     * @param baseUrl    Dify API基础URL
     * @param avatarAuth AI分身授权token
     * @param fileName   文件名
     * @param fileData   文件数据
     * @param mimeType   文件MIME类型
     * @return           上传结果
     */
    String uploadFile(Long userId, String baseUrl, String avatarAuth, String fileName, byte[] fileData, String mimeType);
}
