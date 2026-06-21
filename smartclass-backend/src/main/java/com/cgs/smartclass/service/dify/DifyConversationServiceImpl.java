package com.cgs.smartclass.service.dify;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclass.config.DifyConfig;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import jakarta.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

/**
 * Dify会话管理服务实现
 * 负责会话的创建、获取、删除、总结等操作
 */
@Service
@Slf4j
public class DifyConversationServiceImpl implements DifyConversationService {

    @Resource
    private DifyConfig difyConfig;

    @Resource
    private DifyHttpHelper difyHttpHelper;

    @Override
    public String getSessionSummary(String sessionId, String baseUrl, String avatarAuth) {
        if (!StringUtils.hasLength(sessionId) || !StringUtils.hasLength(baseUrl) || !StringUtils.hasLength(avatarAuth)) {
            throw new RuntimeException("参数错误");
        }

        try {
            // 构建请求URL
            String apiUrl = baseUrl + "/chat-messages/summarize";

            // 创建请求JSON对象
            JSONObject requestBody = new JSONObject();
            requestBody.set("conversation_id", sessionId);
            String jsonBody = requestBody.toString();

            // 添加请求头
            Map<String, String> headers = difyHttpHelper.buildAuthHeaders(avatarAuth);

            // 使用 OkHttp 发送请求
            Response response = difyHttpHelper.postJson(apiUrl, jsonBody, headers);

            // 检查响应状态
            if (!response.isSuccessful()) {
                String responseBody = difyHttpHelper.readResponseBody(response);
                response.close();
                throw new RuntimeException("获取会话总结失败: " + response.code() + " " + responseBody);
            }

            // 解析响应
            String responseBody = difyHttpHelper.readResponseBody(response);

            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

            // 提取总结内容
            String summary = jsonResponse.getStr("summary");
            if (!StringUtils.hasLength(summary)) {
                return "聊天记录总结";
            }

            return summary;
        } catch (Exception e) {
            log.error("获取会话总结失败", e);
            throw new RuntimeException("获取会话总结失败: " + e.getMessage());
        }
    }

    @Override
    public boolean deleteConversation(Long userId, String sessionId, String baseUrl, String avatarAuth) {
        if (!StringUtils.hasLength(sessionId) || !StringUtils.hasLength(baseUrl) || !StringUtils.hasLength(avatarAuth)) {
            throw new RuntimeException("参数错误");
        }

        try {
            // 构建请求URL
            String apiUrl = baseUrl + "/conversations/" + sessionId;

            // 创建请求JSON对象
            JSONObject requestBody = new JSONObject();
            requestBody.set("user", difyConfig.getUserPrefix() + userId);
            String jsonBody = requestBody.toString();

            // 添加请求头
            Map<String, String> headers = difyHttpHelper.buildAuthHeaders(avatarAuth);

            // 使用 OkHttp 发送DELETE请求
            Response response = difyHttpHelper.delete(apiUrl, jsonBody, headers);

            // 检查响应状态
            if (!response.isSuccessful()) {
                // 如果是404错误，则表示会话不存在，也算成功
                if (response.code() == 404) {
                    log.warn("Dify会话不存在，视为删除成功: {}", sessionId);
                    response.close();
                    return true;
                }
                String responseBody = difyHttpHelper.readResponseBody(response);
                log.error("删除Dify会话失败: {}, {}", response.code(), responseBody);
                response.close();
                return false;
            }

            // 解析响应
            String responseBody = difyHttpHelper.readResponseBody(response);

            JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

            // 检查结果
            String result = jsonResponse.getStr("result");
            return "success".equals(result);

        } catch (Exception e) {
            log.error("删除Dify会话异常: {}", e.getMessage(), e);
            return false;
        }
    }

    @Override
    public boolean stopStreamingResponse(Long userId, String taskId, String baseUrl, String avatarAuth) {
        if (userId == null || !StringUtils.hasLength(taskId) ||
                !StringUtils.hasLength(baseUrl) || !StringUtils.hasLength(avatarAuth)) {
            log.error("停止流式响应参数错误: userId={}, taskId={}, baseUrl={}",
                    userId, taskId, baseUrl);
            return false;
        }

        try {
            // 构建请求URL
            String apiUrl = baseUrl + "/chat-messages/" + taskId + "/stop";

            // 构建请求参数
            Map<String, Object> params = new HashMap<>();
            params.put("user", difyConfig.getUserPrefix() + userId);

            // 转换为JSON
            String requestJson = JSONUtil.toJsonStr(params);

            // 添加请求头
            Map<String, String> headers = difyHttpHelper.buildAuthHeaders(avatarAuth);

            // 使用 OkHttp 发送请求
            Response response = difyHttpHelper.postJson(apiUrl, requestJson, headers);

            try {
                if (!response.isSuccessful()) {
                    log.error("停止流式响应失败: {}", response.code());
                    return false;
                }

                // 解析响应体
                String responseBody = response.body().string();
                JSONObject jsonResponse = JSONUtil.parseObj(responseBody);

                // 检查结果
                String result = jsonResponse.getStr("result");
                return "success".equals(result);

            } catch (Exception e) {
                log.error("停止流式响应异常", e);
                return false;
            } finally {
                response.close();
            }

        } catch (Exception e) {
            log.error("停止流式响应异常", e);
            return false;
        }
    }
}
