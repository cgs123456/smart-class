package com.cgs.smartclass.model.dto.oralpractice;

import lombok.Data;

import java.io.Serializable;

/**
 * 提交口语练习请求
 */
@Data
public class OralPracticeSubmitRequest implements Serializable {

    private Long practiceId;

    private String userAudioUrl;

    private Integer duration;

    private static final long serialVersionUID = 1L;
}
