package com.cgs.smartclass.model.dto.ebook;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;

/**
 * 电子书阅读进度更新请求
 */
@Data
public class EbookReadingUpdateRequest implements Serializable {

    private Long ebookId;

    private BigDecimal progress;

    private Integer lastReadPage;

    private static final long serialVersionUID = 1L;
}
