package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * 学习计划视图
 */
@Data
public class LearnScheduleVO implements Serializable {

    private Long id;
    private Long userId;
    private String title;
    private String description;
    private Date startDate;
    private Date endDate;
    private Integer status;
    private Integer dailyMinutes;
    private Date createTime;
    private Date updateTime;
    private List<LearnScheduleItemVO> items;

    private static final long serialVersionUID = 1L;
}
