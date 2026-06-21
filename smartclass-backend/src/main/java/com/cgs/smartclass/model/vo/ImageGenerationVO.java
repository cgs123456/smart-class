package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片生成记录视图
 */
@Data
public class ImageGenerationVO implements Serializable {

    private Long id;

    private Long userId;

    private String prompt;

    private String negativePrompt;

    private String style;

    private String size;

    private String imageUrl;

    private Integer status;

    private String errorMessage;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
