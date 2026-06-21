package com.cgs.smartclassbackendpay.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.cgs.smartclassbackendcommon.annotation.AuthCheck;
import com.cgs.smartclassbackendcommon.common.BaseResponse;
import com.cgs.smartclassbackendcommon.common.PageRequest;
import com.cgs.smartclassbackendcommon.common.ResultUtils;
import com.cgs.smartclassbackendcommon.constant.UserConstant;
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
     * 支付回调（无需登录）
     */
    @PostMapping("/callback/{channel}")
    public BaseResponse<Boolean> handleCallback(@PathVariable String channel,
                                                 @RequestBody Map<String, String> params) {
        boolean result = paymentOrderService.handleCallback(channel, params);
        return ResultUtils.success(result);
    }
}
