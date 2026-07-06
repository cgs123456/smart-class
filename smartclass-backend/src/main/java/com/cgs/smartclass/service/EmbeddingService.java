package com.cgs.smartclass.service;

import java.util.List;

/**
 * 向量嵌入服务
 */
public interface EmbeddingService {

    /**
     * 生成文本的向量嵌入
     *
     * @param text 文本内容
     * @return 向量数组，如果服务不可用返回 null
     */
    float[] embed(String text);

    /**
     * 批量生成向量嵌入
     *
     * @param texts 文本列表
     * @return 向量列表，如果服务不可用返回 null
     */
    List<float[]> embedBatch(List<String> texts);

    /**
     * 检查嵌入服务是否可用
     */
    boolean isAvailable();
}
