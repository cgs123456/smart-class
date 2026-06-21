package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.model.dto.payment.PaymentOrderCreateRequest;
import com.cgs.smartclass.model.entity.PaymentOrder;
import com.cgs.smartclass.model.vo.PaymentOrderVO;

import java.util.Map;

/**
 * 支付订单服务
 */
public interface PaymentOrderService extends IService<PaymentOrder> {

    /**
     * 创建订单
     *
     * @param userId  用户ID
     * @param request 创建请求
     * @return 订单VO
     */
    PaymentOrderVO createOrder(Long userId, PaymentOrderCreateRequest request);

    /**
     * 支付订单
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 订单VO
     */
    PaymentOrderVO payOrder(Long userId, String orderNo);

    /**
     * 取消订单
     *
     * @param userId  用户ID
     * @param orderNo 订单号
     * @return 是否成功
     */
    boolean cancelOrder(Long userId, String orderNo);

    /**
     * 获取我的订单列表
     *
     * @param userId      用户ID
     * @param pageRequest 分页请求
     * @return 订单分页
     */
    Page<PaymentOrderVO> getMyOrders(Long userId, PageRequest pageRequest);

    /**
     * 获取订单详情
     *
     * @param userId 用户ID
     * @param id     订单ID
     * @return 订单VO
     */
    PaymentOrderVO getOrderById(Long userId, Long id);

    /**
     * 支付回调处理
     *
     * @param paymentChannel 支付渠道
     * @param params         回调参数
     * @return 是否成功
     */
    boolean handleCallback(String paymentChannel, Map<String, String> params);
}
