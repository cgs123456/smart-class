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

    /**
     * 支付回调处理（无验签，已过时，仅保留兼容性）
     *
     * @deprecated 请使用 {@link #handleCallbackWithVerify(String, Map, Map)}
     * 在网关层或调用方完成签名校验后再调用本方法处理业务
     */
    @Deprecated
    boolean handleCallback(String paymentChannel, Map<String, String> params);

    /**
     * 支付回调处理（带验签）
     *
     * <p>根据不同支付渠道使用对应算法校验签名，验签通过后才执行业务逻辑：</p>
     * <ul>
     *   <li>wechat：使用 {@code wx.pay.sign-key} 通过 HMAC-SHA256 校验
     *       {@code X-Wechatpay-Signature} 头（兼容微信支付 v3 回调风格）</li>
     *   <li>alipay：使用 {@code alipay.public-key} 通过 RSA-SHA256 校验
     *       参数中的 {@code sign} 字段（支付宝开放平台规则）</li>
     * </ul>
     *
     * @param channel 支付渠道（wechat / alipay）
     * @param params  回调请求体参数
     * @param headers 回调请求头（包含 wechatpay-signature / wechatpay-timestamp / wechatpay-nonce 等）
     * @return 业务处理是否成功
     * @throws com.cgs.smartclassbackendcommon.exception.BusinessException 验签失败或参数缺失时抛出
     */
    boolean handleCallbackWithVerify(String channel,
                                     Map<String, String> params,
                                     Map<String, String> headers);
}
