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
 * 用户课程
 * @TableName user_course
 */
@TableName(value ="user_course")
@Data
public class UserCourse implements Serializable {
    /**
     * id
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 用户id
     */
    private Long userId;

    /**
     * 课程id
     */
    private Long courseId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 价格
     */
    private BigDecimal price;

    /**
     * 支付方式
     */
    private String paymentMethod;

    /**
     * 支付时间
     */
    private Date paymentTime;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 状态：0-未支付，1-已支付，2-已过期，3-已退款
     */
    private Integer status;

    /**
     * 创建时间
     */
    private Date createTime;

    /**
     * 更新时间
     */
    private Date updateTime;

    /**
     * 是否删除
     */
    private Integer isDelete;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
