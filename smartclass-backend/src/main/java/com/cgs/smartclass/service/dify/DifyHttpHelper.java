package com.cgs.smartclass.service.dify;

import com.cgs.smartclass.utils.OkHttpUtils;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.stereotype.Component;

import jakarta.annotation.Resource;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify HTTP调用工具类
 * 封装与Dify API交互的通用HTTP操作
 */
@Component
@Slf4j
public class DifyHttpHelper {

    @Resource
    private OkHttpUtils okHttpUtils;

    /**
     * 构建认证请求头
     *
     * @param avatarAuth AI分身授权token
     * @return 请求头Map
     */
    public Map<String, String> buildAuthHeaders(String avatarAuth) {
        Map<String, String> headers = new HashMap<>();
        headers.put("Content-Type", "application/json");
        headers.put("Authorization", "Bearer " + avatarAuth);
        return headers;
    }

    /**
     * 构建流式认证请求头
     *
     * @param avatarAuth AI分身授权token
     * @return 请求头Map
     */
    public Map<String, String> buildStreamAuthHeaders(String avatarAuth) {
        Map<String, String> headers = buildAuthHeaders(avatarAuth);
        headers.put("Accept", "text/event-stream");
        return headers;
    }

    /**
     * 发送JSON POST请求
     *
     * @param url      请求URL
     * @param jsonBody JSON请求体
     * @param headers  请求头
     * @return Response对象
     */
    public Response postJson(String url, String jsonBody, Map<String, String> headers) {
        return okHttpUtils.postJson(url, jsonBody, headers);
    }

    /**
     * 发送流式JSON POST请求
     *
     * @param url      请求URL
     * @param jsonBody JSON请求体
     * @param headers  请求头
     * @return Response对象
     */
    public Response postJsonStream(String url, String jsonBody, Map<String, String> headers) {
        return okHttpUtils.postJsonStream(url, jsonBody, headers);
    }

    /**
     * 发送DELETE请求
     *
     * @param url      请求URL
     * @param jsonBody JSON请求体
     * @param headers  请求头
     * @return Response对象
     */
    public Response delete(String url, String jsonBody, Map<String, String> headers) {
        return okHttpUtils.delete(url, jsonBody, headers);
    }

    /**
     * 安全读取响应体字符串
     *
     * @param response HTTP响应
     * @return 响应体字符串，读取失败返回空字符串
     */
    public String readResponseBody(Response response) {
        try (ResponseBody body = response.body()) {
            if (body != null) {
                return body.string();
            }
        } catch (IOException e) {
            log.error("读取响应体异常", e);
            throw new RuntimeException("读取响应失败: " + e.getMessage());
        }
        return "";
    }
}
