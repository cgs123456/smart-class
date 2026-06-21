package com.cgs.smartclass.model.vo;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 口语练习记录视图
 */
@Data
public class OralPracticeRecordVO implements Serializable {

    private Long id;

    private Long userId;

    private Long practiceId;

    private String userAudioUrl;

    private Integer duration;

    private BigDecimal aiScore;

    private String aiFeedback;

    private Date createTime;

    private static final long serialVersionUID = 1L;
}
