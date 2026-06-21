package com.cgs.smartclass.model.dto.wrongquestion;

import com.cgs.smartclass.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询错题请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class WrongQuestionQueryRequest extends PageRequest implements Serializable {

    private String questionType;

    private Integer masteryLevel;

    private static final long serialVersionUID = 1L;
}
