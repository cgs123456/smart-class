package com.cgs.smartclass.model.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单
 * @TableName payment_order
 */
@TableName(value = "payment_order")
@Data
public class PaymentOrder implements Serializable {

    /**
     * 订单ID
     */
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 商品名称
     */
    private String productName;

    /**
     * 支付金额
     */
    private BigDecimal amount;

    /**
     * 支付渠道(alipay/wechat)
     */
    private String paymentChannel;

    /**
     * 订单状态(0-待支付 1-已支付 2-已取消 3-已退款)
     */
    private Integer status;

    /**
     * 支付时间
     */
    private Date paymentTime;

    /**
     * 过期时间
     */
    private Date expireTime;

    /**
     * 第三方交易号
     */
    private String tradeNo;

    /**
     * 是否删除
     */
    @TableLogic
    private Integer isDelete;

    /**
     * 创建时间
     */
    @TableField(fill = FieldFill.INSERT)
    private Date createTime;

    /**
     * 更新时间
     */
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private Date updateTime;

    @TableField(exist = false)
    private static final long serialVersionUID = 1L;
}
