package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 电子书视图
 */
@Data
public class EbookVO implements Serializable {

    private Long id;

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

    private Integer downloadCount;

    private Integer isVipOnly;

    private Integer status;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
