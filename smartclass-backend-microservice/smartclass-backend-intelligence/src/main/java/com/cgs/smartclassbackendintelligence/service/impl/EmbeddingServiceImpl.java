package com.cgs.smartclassbackendintelligence.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclassbackendintelligence.service.EmbeddingService;
import com.cgs.smartclassbackendintelligence.utils.OkHttpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 文本向量化服务实现
 * 调用 OpenAI 兼容的 /v1/embeddings API 将文本转换为向量
 */
@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    @Value("${rag.embedding-api-url:}")
    private String apiUrl;

    @Value("${rag.embedding-api-key:}")
    private String apiKey;

    @Value("${rag.embedding-model:text-embedding-v3}")
    private String model;

    @Resource
    private OkHttpUtils okHttpUtils;

    @Override
    public float[] embed(String text) {
        if (!isAvailable() || StrUtil.isBlank(text)) {
            return null;
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", text);
            requestBody.put("encoding_format", "float");
            String jsonBody = JSONUtil.toJsonStr(requestBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + apiKey);

            Response response = okHttpUtils.postJson(apiUrl, jsonBody, headers);
            try (ResponseBody body = response.body()) {
                if (!response.isSuccessful() || body == null) {
                    log.warn("Embedding API 返回错误状态码: {}", response.code());
                    return null;
                }
                String responseBody = body.string();
                JSONObject json = JSONUtil.parseObj(responseBody);
                JSONArray data = json.getJSONArray("data");
                if (data == null || data.isEmpty()) {
                    return null;
                }
                return parseEmbedding(data.getJSONObject(0).getJSONArray("embedding"));
            }
        } catch (Exception e) {
            log.warn("Embedding 调用失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (!isAvailable() || texts == null || texts.isEmpty()) {
            return null;
        }
        try {
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("model", model);
            requestBody.put("input", texts);
            requestBody.put("encoding_format", "float");
            String jsonBody = JSONUtil.toJsonStr(requestBody);

            Map<String, String> headers = new HashMap<>();
            headers.put("Content-Type", "application/json");
            headers.put("Authorization", "Bearer " + apiKey);

            Response response = okHttpUtils.postJson(apiUrl, jsonBody, headers);
            try (ResponseBody body = response.body()) {
                if (!response.isSuccessful() || body == null) {
                    log.warn("Embedding 批量 API 返回错误状态码: {}", response.code());
                    return null;
                }
                String responseBody = body.string();
                JSONObject json = JSONUtil.parseObj(responseBody);
                JSONArray data = json.getJSONArray("data");
                if (data == null || data.isEmpty()) {
                    return null;
                }
                List<float[]> result = new ArrayList<>();
                for (int i = 0; i < data.size(); i++) {
                    JSONArray embeddingArray = data.getJSONObject(i).getJSONArray("embedding");
                    result.add(parseEmbedding(embeddingArray));
                }
                return result;
            }
        } catch (Exception e) {
            log.warn("Embedding 批量调用失败: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return StrUtil.isNotBlank(apiUrl) && StrUtil.isNotBlank(apiKey);
    }

    /**
     * 解析嵌入向量 JSON 数组
     */
    private float[] parseEmbedding(JSONArray embeddingArray) {
        if (embeddingArray == null || embeddingArray.isEmpty()) {
            return null;
        }
        float[] embedding = new float[embeddingArray.size()];
        for (int i = 0; i < embeddingArray.size(); i++) {
            embedding[i] = embeddingArray.getFloat(i);
        }
        return embedding;
    }
}
