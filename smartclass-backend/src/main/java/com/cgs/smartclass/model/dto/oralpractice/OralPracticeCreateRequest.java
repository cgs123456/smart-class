package com.cgs.smartclass.model.dto.oralpractice;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建口语练习题目请求
 */
@Data
public class OralPracticeCreateRequest implements Serializable {

    private String title;

    private String description;

    private String category;

    private Integer difficulty;

    private String referenceAnswer;

    private String keywords;

    private static final long serialVersionUID = 1L;
}
