package com.cgs.smartclass.model.dto.wrongquestion;

import lombok.Data;

import java.io.Serializable;

/**
 * 错题复习请求
 */
@Data
public class WrongQuestionReviewRequest implements Serializable {

    private Long id;

    private Integer masteryLevel;

    private static final long serialVersionUID = 1L;
}
