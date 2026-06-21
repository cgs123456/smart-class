package com.cgs.smartclass.model.dto.imagegeneration;

import lombok.Data;

import java.io.Serializable;

/**
 * 图片生成请求
 */
@Data
public class ImageGenerationRequest implements Serializable {

    private String prompt;

    private String negativePrompt;

    private String style;

    private String size;

    private static final long serialVersionUID = 1L;
}
