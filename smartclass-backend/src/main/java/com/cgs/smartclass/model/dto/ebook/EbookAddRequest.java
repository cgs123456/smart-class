package com.cgs.smartclass.model.dto.ebook;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加电子书请求
 */
@Data
public class EbookAddRequest implements Serializable {

    private String title;

    private String author;

    private String description;

    private String coverUrl;

    private String fileUrl;

    private Long fileSize;

    private String category;

    private String level;

    private String language;

    private Integer pageCount;

    private Integer isVipOnly;

    private static final long serialVersionUID = 1L;
}
