package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 口语练习题目视图
 */
@Data
public class OralPracticeVO implements Serializable {

    private Long id;

    private String title;

    private String description;

    private String category;

    private Integer difficulty;

    private String referenceAnswer;

    private String keywords;

    private String audioUrl;

    private String imageUrl;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
