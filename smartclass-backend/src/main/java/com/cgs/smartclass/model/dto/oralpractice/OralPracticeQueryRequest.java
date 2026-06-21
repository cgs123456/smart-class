package com.cgs.smartclass.model.dto.oralpractice;

import com.cgs.smartclass.common.PageRequest;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.io.Serializable;

/**
 * 查询口语练习题目请求
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class OralPracticeQueryRequest extends PageRequest implements Serializable {

    private String category;

    private Integer difficulty;

    private static final long serialVersionUID = 1L;
}
