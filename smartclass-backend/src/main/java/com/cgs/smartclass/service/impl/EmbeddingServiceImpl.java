package com.cgs.smartclass.service.impl;

import cn.hutool.core.collection.CollUtil;
import cn.hutool.core.util.StrUtil;
import cn.hutool.json.JSONArray;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.cgs.smartclass.service.EmbeddingService;
import com.cgs.smartclass.utils.OkHttpUtils;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Response;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 向量嵌入服务实现
 * 调用 OpenAI 兼容的 /v1/embeddings API
 */
@Service
@Slf4j
public class EmbeddingServiceImpl implements EmbeddingService {

    @Resource
    private OkHttpUtils okHttpUtils;

    @Value("${rag.embedding-api-url:}")
    private String apiUrl;

    @Value("${rag.embedding-api-key:}")
    private String apiKey;

    @Value("${rag.embedding-model:text-embedding-v3}")
    private String model;

    @Value("${rag.embedding-dims:1024}")
    private int dims;

    @Override
    public float[] embed(String text) {
        if (!isAvailable()) {
            return null;
        }
        if (StrUtil.isBlank(text)) {
            return null;
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", text);
        requestBody.put("encoding_format", "float");
        String jsonBody = JSONUtil.toJsonStr(requestBody);

        try (Response response = okHttpUtils.postJson(apiUrl, jsonBody, buildHeaders())) {
            if (response == null || !response.isSuccessful()) {
                int code = response != null ? response.code() : -1;
                log.warn("Embedding API 调用失败，HTTP状态: {}", code);
                return null;
            }
            String body = response.body() != null ? response.body().string() : null;
            if (StrUtil.isBlank(body)) {
                return null;
            }
            return parseFirstEmbedding(body);
        } catch (Exception e) {
            log.warn("Embedding 调用异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public List<float[]> embedBatch(List<String> texts) {
        if (!isAvailable()) {
            return null;
        }
        if (CollUtil.isEmpty(texts)) {
            return new ArrayList<>();
        }
        Map<String, Object> requestBody = new HashMap<>();
        requestBody.put("model", model);
        requestBody.put("input", texts);
        requestBody.put("encoding_format", "float");
        String jsonBody = JSONUtil.toJsonStr(requestBody);

        try (Response response = okHttpUtils.postJson(apiUrl, jsonBody, buildHeaders())) {
            if (response == null || !response.isSuccessful()) {
                int code = response != null ? response.code() : -1;
                log.warn("Embedding 批量调用失败，HTTP状态: {}", code);
                return null;
            }
            String body = response.body() != null ? response.body().string() : null;
            if (StrUtil.isBlank(body)) {
                return null;
            }
            return parseEmbeddings(body);
        } catch (Exception e) {
            log.warn("Embedding 批量调用异常: {}", e.getMessage());
            return null;
        }
    }

    @Override
    public boolean isAvailable() {
        return StrUtil.isNotBlank(apiUrl) && StrUtil.isNotBlank(apiKey);
    }

    /**
     * 构建请求头
     */
    private Map<String, String> buildHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("Authorization", "Bearer " + apiKey);
        headers.put("Content-Type", "application/json");
        return headers;
    }

    /**
     * 解析响应中第一条 embedding
     */
    private float[] parseFirstEmbedding(String responseBody) {
        try {
            JSONObject json = JSONUtil.parseObj(responseBody);
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return null;
            }
            JSONArray embArr = data.getJSONObject(0).getJSONArray("embedding");
            return toFloatArray(embArr);
        } catch (Exception e) {
            log.warn("解析 embedding 响应异常: {}", e.getMessage());
            return null;
        }
    }

    /**
     * 解析响应中全部 embedding
     */
    private List<float[]> parseEmbeddings(String responseBody) {
        try {
            JSONObject json = JSONUtil.parseObj(responseBody);
            JSONArray data = json.getJSONArray("data");
            if (data == null || data.isEmpty()) {
                return null;
            }
            List<float[]> result = new ArrayList<>(data.size());
            for (int i = 0; i < data.size(); i++) {
                JSONArray embArr = data.getJSONObject(i).getJSONArray("embedding");
                result.add(toFloatArray(embArr));
            }
            return result;
        } catch (Exception e) {
            log.warn("解析 embedding 批量响应异常: {}", e.getMessage());
            return null;
        }
    }

    private float[] toFloatArray(JSONArray embArr) {
        if (embArr == null || embArr.isEmpty()) {
            return null;
        }
        float[] embedding = new float[embArr.size()];
        for (int i = 0; i < embArr.size(); i++) {
            embedding[i] = embArr.getFloat(i);
        }
        return embedding;
    }
}
