package com.cgs.smartclass.model.dto.wrongquestion;

import lombok.Data;

import java.io.Serializable;

/**
 * 添加错题请求
 */
@Data
public class WrongQuestionAddRequest implements Serializable {

    private String questionType;

    private String questionContent;

    private String correctAnswer;

    private String userAnswer;

    private String analysis;

    private String sourceType;

    private Long sourceId;

    private static final long serialVersionUID = 1L;
}
