package com.cgs.smartclass.model.dto.payment;

import lombok.Data;

import java.io.Serializable;

/**
 * 创建支付订单请求
 */
@Data
public class PaymentOrderCreateRequest implements Serializable {

    /**
     * 商品ID
     */
    private Long productId;

    /**
     * 支付渠道(alipay/wechat)
     */
    private String paymentChannel;

    private static final long serialVersionUID = 1L;
}
