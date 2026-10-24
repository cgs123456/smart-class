package com.smartclass.smartclass.model.dto.imagegeneration;

import com.smartclass.smartclass.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 图片生成查询请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ImageGenerationQueryRequest extends PageRequest implements Serializable {

    private Integer status;

    private static final long serialVersionUID = 1L;
}
