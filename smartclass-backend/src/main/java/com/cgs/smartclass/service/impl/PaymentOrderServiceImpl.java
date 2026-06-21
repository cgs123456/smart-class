package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.common.PageRequest;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.mapper.PaymentOrderMapper;
import com.cgs.smartclass.model.dto.payment.PaymentOrderCreateRequest;
import com.cgs.smartclass.model.entity.PaymentOrder;
import com.cgs.smartclass.model.entity.Product;
import com.cgs.smartclass.model.vo.PaymentOrderVO;
import com.cgs.smartclass.service.PaymentOrderService;
import com.cgs.smartclass.service.ProductService;
import com.cgs.smartclass.service.UserVipService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 支付订单服务实现
 */
@Service
@Slf4j
public class PaymentOrderServiceImpl extends ServiceImpl<PaymentOrderMapper, PaymentOrder>
        implements PaymentOrderService {

    @Resource
    private ProductService productService;

    @Resource
    private UserVipService userVipService;

    @Override
    public PaymentOrderVO createOrder(Long userId, PaymentOrderCreateRequest request) {
        if (userId == null || request == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        Long productId = request.getProductId();
        String paymentChannel = request.getPaymentChannel();
        if (productId == null || productId <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品ID不合法");
        }
        if (StringUtils.isBlank(paymentChannel)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "支付渠道不能为空");
        }
        // 查询商品
        Product product = productService.getById(productId);
        if (product == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "商品不存在");
        }
        if (product.getStatus() != 1) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "商品已下架");
        }
        // 生成订单号
        String orderNo = System.currentTimeMillis() + "" + (int) ((Math.random() * 9 + 1) * 1000);
        // 创建订单
        PaymentOrder order = new PaymentOrder();
        order.setOrderNo(orderNo);
        order.setUserId(userId);
        order.setProductId(productId);
        order.setProductName(product.getName());
        order.setAmount(product.getPrice());
        order.setPaymentChannel(paymentChannel);
        order.setStatus(0);
        // 设置30分钟过期
        Calendar calendar = Calendar.getInstance();
        calendar.add(Calendar.MINUTE, 30);
        order.setExpireTime(calendar.getTime());
        boolean result = this.save(order);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "创建订单失败");
        }
        return convertToVO(order);
    }

    @Override
    public PaymentOrderVO payOrder(Long userId, String orderNo) {
        if (userId == null || StringUtils.isBlank(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 查询订单
        QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("orderNo", orderNo);
        queryWrapper.eq("userId", userId);
        PaymentOrder order = this.getOne(queryWrapper);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单状态不正确");
        }
        // 检查是否过期
        if (order.getExpireTime() != null && order.getExpireTime().before(new Date())) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "订单已过期");
        }
        // 模拟支付（实际项目中对接支付宝/微信API）
        order.setStatus(1);
        order.setPaymentTime(new Date());
        order.setTradeNo("SIM_" + System.currentTimeMillis());
        boolean result = this.updateById(order);
        if (!result) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "支付失败");
        }
        // 如果是VIP商品，激活会员
        Product product = productService.getById(order.getProductId());
        if (product != null && "vip".equals(product.getType())) {
            String level = StringUtils.isNotBlank(product.getLevel()) ? product.getLevel() : "vip";
            int durationDays = product.getDurationDays() != null ? product.getDurationDays() : 30;
            userVipService.activateVip(userId, level, durationDays, order.getId());
        }
        return convertToVO(order);
    }

    @Override
    public boolean cancelOrder(Long userId, String orderNo) {
        if (userId == null || StringUtils.isBlank(orderNo)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("orderNo", orderNo);
        queryWrapper.eq("userId", userId);
        PaymentOrder order = this.getOne(queryWrapper);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        if (order.getStatus() != 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "只能取消待支付订单");
        }
        order.setStatus(2);
        return this.updateById(order);
    }

    @Override
    public Page<PaymentOrderVO> getMyOrders(Long userId, PageRequest pageRequest) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        long current = pageRequest.getCurrent();
        long size = pageRequest.getPageSize();
        QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        queryWrapper.orderByDesc("createTime");
        Page<PaymentOrder> orderPage = this.page(new Page<>(current, size), queryWrapper);
        Page<PaymentOrderVO> voPage = new Page<>(current, size, orderPage.getTotal());
        List<PaymentOrderVO> voList = orderPage.getRecords().stream()
                .map(this::convertToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return voPage;
    }

    @Override
    public PaymentOrderVO getOrderById(Long userId, Long id) {
        if (userId == null || id == null || id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        PaymentOrder order = this.getById(id);
        if (order == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "订单不存在");
        }
        if (!order.getUserId().equals(userId)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR);
        }
        return convertToVO(order);
    }

    @Override
    public boolean handleCallback(String paymentChannel, Map<String, String> params) {
        // 支付回调处理（预留接口）
        log.info("支付回调处理，channel: {}, params: {}", paymentChannel, params);
        return true;
    }

    /**
     * 转换为VO
     */
    private PaymentOrderVO convertToVO(PaymentOrder order) {
        if (order == null) {
            return null;
        }
        PaymentOrderVO vo = new PaymentOrderVO();
        BeanUtils.copyProperties(order, vo);
        return vo;
    }
}
