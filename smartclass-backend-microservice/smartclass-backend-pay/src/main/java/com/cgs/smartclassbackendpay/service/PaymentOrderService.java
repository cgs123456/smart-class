package com.cgs.smartclassbackendpay.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendmodel.model.dto.payment.PaymentOrderCreateRequest;
import com.cgs.smartclassbackendmodel.model.entity.PaymentOrder;
import com.cgs.smartclassbackendmodel.model.vo.PaymentOrderVO;

import java.util.Map;

/**
 * 支付订单服务
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    PaymentOrderVO createOrder(Long userId, PaymentOrderCreateRequest request);

    PaymentOrderVO payOrder(Long userId, String orderNo);

    boolean cancelOrder(Long userId, String orderNo);

    Page<PaymentOrderVO> getMyOrders(Long userId, PageRequest pageRequest);

    PaymentOrderVO getOrderById(Long userId, Long id);

    boolean handleCallback(String paymentChannel, Map<String, String> params);
}
