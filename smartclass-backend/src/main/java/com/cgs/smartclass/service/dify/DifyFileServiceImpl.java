package com.cgs.smartclass.service.dify;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclass.config.DifyConfig;
import com.cgs.smartclass.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.io.ByteArrayInputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify文件服务实现
 * 负责文件上传到Dify
 */
@Service
@Slf4j
public class DifyFileServiceImpl implements DifyFileService {

    @Resource
    private DifyConfig difyConfig;

    @Resource
    private OkHttpUtils okHttpUtils;

    @Override
    public String uploadFile(Long userId, String baseUrl, String avatarAuth, String fileName, byte[] fileData, String mimeType) {
        try {
            // 构建请求URL
            String apiUrl = baseUrl + "/files/upload";

            // 构建表单数据
            Map<String, String> formData = new HashMap<>();
            formData.put("user", difyConfig.getUserPrefix() + userId);

            // 构建请求头
            Map<String, String> headers = new HashMap<>();
            headers.put("Authorization", "Bearer " + avatarAuth);

            // 发送多部分表单请求
            Response response = okHttpUtils.postMultipart(
                    apiUrl,
                    formData,
                    "file",
                    fileName,
                    new ByteArrayInputStream(fileData),
                    mimeType,
                    headers
            );

            try {
                if (!response.isSuccessful()) {
                    String responseBody = "";
                    if (response.body() != null) {
                        responseBody = response.body().string();
                    }
                    log.error("上传文件到Dify失败: {}, {}", response.code(), responseBody);
                    throw new RuntimeException("上传文件到Dify失败: " + response.code());
                }

                String responseBody = "";
                if (response.body() != null) {
                    responseBody = response.body().string();
                }

                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);
                return jsonResponse.getStr("id");
            } finally {
                response.close();
            }
        } catch (Exception e) {
            log.error("上传文件到Dify异常", e);
            throw new RuntimeException("上传文件到Dify失败: " + e.getMessage());
        }
    }
}
