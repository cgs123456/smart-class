package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 电子书阅读记录视图
 */
@Data
public class EbookReadingRecordVO implements Serializable {

    private Long id;

    private Long userId;

    private Long ebookId;

    private BigDecimal progress;

    private Integer lastReadPage;

    private Date lastReadTime;

    private Integer isFavorite;

    private Date createTime;

    /**
     * 电子书标题
     */
    private String ebookTitle;

    /**
     * 电子书封面URL
     */
    private String ebookCoverUrl;

    /**
     * 电子书作者
     */
    private String ebookAuthor;

    private static final long serialVersionUID = 1L;
}
