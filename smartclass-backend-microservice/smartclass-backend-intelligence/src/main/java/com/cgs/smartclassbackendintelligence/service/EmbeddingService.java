package com.cgs.smartclassbackendintelligence.service;

import java.util.List;

/**
 * 文本向量化服务
 * 用于将文本转换为向量，支持 RAG 向量检索
 */
public interface EmbeddingService {

    /**
     * 将单个文本转换为向量
     *
     * @param text 文本
     * @return 向量数组，失败时返回 null
     */
    float[] embed(String text);

    /**
     * 批量将文本转换为向量
     *
     * @param texts 文本列表
     * @return 向量列表，失败时返回 null
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 检查向量化服务是否可用
     *
     * @return 是否可用
     */
    boolean isAvailable();
}
