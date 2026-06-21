package com.cgs.smartclassbackendmodel.model.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

/**
 * 支付订单视图
 */
@Data
public class PaymentOrderVO implements Serializable {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long productId;
    private String productName;
    private BigDecimal amount;
    private String paymentChannel;
    private Integer status;
    private Date paymentTime;
    private Date expireTime;
    private String tradeNo;
    private Date createTime;

    private static final long serialVersionUID = 1L;
}
