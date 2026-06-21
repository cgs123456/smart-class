package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 图片生成记录
 * @TableName image_generation
 */
@TableName(value = "image_generation")
@Data
public class ImageGeneration implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private String prompt;

    private String negativePrompt;

    private String style;

    private String size;

    private String imageUrl;

    private Integer status;

    private String errorMessage;

    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
