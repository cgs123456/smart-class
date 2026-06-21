package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.io.Serializable;
import java.util.Date;
import lombok.Data;

/**
 * 电子书
 * @TableName ebook
 */
@TableName(value = "ebook")
@Data
public class Ebook implements Serializable {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private String author;

    private String description;

    private String coverUrl;

    private String fileUrl;

    private Long fileSize;

    private String category;

    private String level;

    private String language;

    private Integer pageCount;

    private Integer downloadCount;

    private Integer isVipOnly;

    private Integer status;

    private Integer isDelete;

    private Date createTime;

    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
