package com.cgs.smartclassbackendpay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclassbackendcommon.annotation.AuthCheck;
import com.cgs.smartclassbackendcommon.common.BaseResponse;
import com.cgs.smartclassbackendcommon.common.ErrorCode;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendcommon.common.ResultUtils;
import com.cgs.smartclassbackendcommon.constant.UserConstant;
import com.cgs.smartclassbackendcommon.exception.BusinessException;
import com.cgs.smartclassbackendmodel.model.dto.payment.PaymentOrderCreateRequest;
import com.cgs.smartclassbackendmodel.model.entity.User;
import com.cgs.smartclassbackendmodel.model.vo.PaymentOrderVO;
import com.cgs.smartclassbackendpay.service.PaymentOrderService;
import com.cgs.smartclassbackendserviceclient.service.UserFeignClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * 支付订单接口
 */
@RestController
@RequestMapping("/payment/order")
@Slf4j
public class PaymentOrderController {

    @Resource
    private PaymentOrderService paymentOrderService;

    @Resource
    private UserFeignClient userService;

    /**
     * 创建订单
     */
    @PostMapping("/create")
    public BaseResponse<PaymentOrderVO> createOrder(@RequestBody PaymentOrderCreateRequest request,
                                                     HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PaymentOrderVO vo = paymentOrderService.createOrder(loginUser.getId(), request);
        return ResultUtils.success(vo);
    }

    /**
     * 支付订单
     */
    @PostMapping("/pay")
    public BaseResponse<PaymentOrderVO> payOrder(@RequestParam String orderNo,
                                                  HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PaymentOrderVO vo = paymentOrderService.payOrder(loginUser.getId(), orderNo);
        return ResultUtils.success(vo);
    }

    /**
     * 取消订单
     */
    @PostMapping("/cancel")
    public BaseResponse<Boolean> cancelOrder(@RequestParam String orderNo,
                                              HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        boolean result = paymentOrderService.cancelOrder(loginUser.getId(), orderNo);
        return ResultUtils.success(result);
    }

    /**
     * 我的订单列表
     */
    @GetMapping("/my")
    public BaseResponse<Page<PaymentOrderVO>> getMyOrders(PageRequest pageRequest,
                                                           HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        Page<PaymentOrderVO> page = paymentOrderService.getMyOrders(loginUser.getId(), pageRequest);
        return ResultUtils.success(page);
    }

    /**
     * 订单详情
     */
    @GetMapping("/{id}")
    public BaseResponse<PaymentOrderVO> getOrderById(@PathVariable Long id,
                                                      HttpServletRequest httpRequest) {
        User loginUser = userService.getLoginUser(httpRequest);
        PaymentOrderVO vo = paymentOrderService.getOrderById(loginUser.getId(), id);
        return ResultUtils.success(vo);
    }

    /**
     * 支付回调（无需登录，但必须通过签名验签）
     *
     * <p>不同支付渠道使用不同的验签策略：</p>
     * <ul>
     *   <li>wechat：使用 {@code WX_PAY_SIGN_KEY} 通过 HMAC-SHA256 校验 {@code X-Wechatpay-Signature} 头</li>
     *   <li>alipay：使用支付宝公钥校验 {@code sign} 字段</li>
     * </ul>
     */
    @PostMapping("/callback/{channel}")
    public BaseResponse<Boolean> handleCallback(@PathVariable String channel,
                                                 @RequestBody Map<String, String> params,
                                                 HttpServletRequest httpRequest) {
        // 提取验签相关参数，由 service 层校验签名
        Map<String, String> headers = extractCallbackHeaders(httpRequest);
        boolean result;
        try {
            result = paymentOrderService.handleCallbackWithVerify(channel, params, headers);
        } catch (BusinessException e) {
            log.warn("支付回调验签失败，channel: {}, reason: {}", channel, e.getMessage());
            // 验签失败返回 false，避免泄露内部细节
            return ResultUtils.success(false);
        }
        if (!result) {
            log.warn("支付回调处理失败，channel: {}, params: {}", channel, params);
        }
        return ResultUtils.success(result);
    }

    /**
     * 提取回调验签所需的请求头。
     */
    private Map<String, String> extractCallbackHeaders(HttpServletRequest request) {
        return Map.of(
                "wechatpay-signature", nullToEmpty(request.getHeader("X-Wechatpay-Signature")),
                "wechatpay-timestamp", nullToEmpty(request.getHeader("X-Wechatpay-Timestamp")),
                "wechatpay-nonce", nullToEmpty(request.getHeader("X-Wechatpay-Nonce")),
                "wechatpay-serial", nullToEmpty(request.getHeader("X-Wechatpay-Serial"))
        );
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
