package com.cgs.smartclass.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单视图
 */
@Data
public class PaymentOrderVO implements Serializable {

    /**
     * 订单ID
     */
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
     * 支付渠道
     */
    private String paymentChannel;

    /**
     * 订单状态
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
     * 创建时间
     */
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
