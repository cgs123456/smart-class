package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;
import lombok.Data;

/**
 * 口语练习记录
 * @TableName oral_practice_record
 */
@TableName(value = "oral_practice_record")
@Data
public class OralPracticeRecord implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long practiceId;

    private String userAudioUrl;

    private Integer duration;

    private BigDecimal aiScore;

    private String aiFeedback;

    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
