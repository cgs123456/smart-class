package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 错题视图
 */
@Data
public class WrongQuestionVO implements Serializable {

    private Long id;

    private Long userId;

    private String questionType;

    private String questionContent;

    private String correctAnswer;

    private String userAnswer;

    private String analysis;

    private String sourceType;

    private Long sourceId;

    private Integer masteryLevel;

    private Integer reviewCount;

    private Date lastReviewTime;

    private Date nextReviewTime;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
