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
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
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

    /**
     * 微信支付回调时间戳容忍窗口（秒），超过该窗口判定为重放攻击
     */
    private static final long WECHAT_CALLBACK_TIMESTAMP_TOLERANCE_SECONDS = 300L;

    /**
     * 微信支付签名密钥（APIv2 风格的 sign-key，用于 HMAC-SHA256 验签）。
     * 生产环境必须通过环境变量 WX_PAY_SIGN_KEY 注入，留空时验签将直接失败。
     */
    @Value("${wx.pay.sign-key:}")
    private String wxPaySignKey;

    /**
     * 支付宝公钥（Base64 编码的 X.509 SubjectPublicKeyInfo），用于 RSA-SHA256 验签。
     * 生产环境必须通过环境变量 ALIPAY_PUBLIC_KEY 注入，留空时 alipay 渠道验签将直接失败。
     */
    @Value("${alipay.public-key:}")
    private String alipayPublicKey;

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
    @Deprecated
    public boolean handleCallback(String paymentChannel, Map<String, String> params) {
        log.warn("调用了未验签的 handleCallback，已拒绝处理，channel: {}", paymentChannel);
        // 旧方法不再执行任何业务逻辑，避免未验签的回调被处理
        return false;
    }

    @Override
    public boolean handleCallbackWithVerify(String channel,
                                            Map<String, String> params,
                                            Map<String, String> headers) {
        if (StringUtils.isBlank(channel) || params == null || headers == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调参数缺失");
        }
        String normalizedChannel = channel.trim().toLowerCase();
        switch (normalizedChannel) {
            case "wechat":
            case "wx":
                verifyWechatCallback(params, headers);
                break;
            case "alipay":
                verifyAlipayCallback(params, headers);
                break;
            default:
                log.warn("不支持的支付渠道：{}", channel);
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "不支持的支付渠道");
        }
        // 验签通过后执行业务逻辑（幂等处理由具体业务保障）
        log.info("支付回调验签通过，channel: {}, orderNo: {}",
                normalizedChannel, params.get("out_trade_no"));
        return doCallbackBusiness(params);
    }

    /**
     * 微信支付回调验签（HMAC-SHA256）。
     *
     * <p>微信支付 v3 回调签名规则简化为：
     * 待签名串 = timestamp + "\n" + nonce + "\n" + body + "\n"，
     * 使用 sign-key 通过 HMAC-SHA256 计算后与 X-Wechatpay-Signature 比较。</p>
     *
     * <p>注意：完整 v3 实现需要使用微信平台证书的 RSA 公钥验签。
     * 此处使用 HMAC-SHA256 + 商户 sign-key，作为未对接平台证书下载接口时的
     * 兼容实现，仍然可以有效防止伪造回调，但强烈建议生产环境对接平台证书后
     * 切换为 RSA-SHA256 验签。</p>
     */
    private void verifyWechatCallback(Map<String, String> params, Map<String, String> headers) {
        if (StringUtils.isBlank(wxPaySignKey)) {
            log.error("微信支付回调验签失败：未配置 wx.pay.sign-key");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "微信支付未配置签名密钥");
        }
        String signature = headers.getOrDefault("signature", "");
        String timestamp = headers.getOrDefault("timestamp", "");
        String nonce = headers.getOrDefault("nonce", "");
        if (StringUtils.isAnyBlank(signature, timestamp, nonce)) {
            log.warn("微信支付回调验签失败：缺少必要请求头");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调请求头缺失");
        }
        // 时间戳防重放
        long callbackTimestamp;
        try {
            callbackTimestamp = Long.parseLong(timestamp);
        } catch (NumberFormatException e) {
            log.warn("微信支付回调时间戳格式非法：{}", timestamp);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调时间戳格式非法");
        }
        long now = System.currentTimeMillis() / 1000L;
        if (Math.abs(now - callbackTimestamp) > WECHAT_CALLBACK_TIMESTAMP_TOLERANCE_SECONDS) {
            log.warn("微信支付回调时间戳超出容忍窗口，回调时间: {}, 服务器时间: {}", callbackTimestamp, now);
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调已过期");
        }
        // 拼接待签名串：timestamp\nnonce\nbody\n
        String body = params.toString();
        String signSource = timestamp + "\n" + nonce + "\n" + body + "\n";
        String expected = hmacSha256(wxPaySignKey, signSource);
        if (!constantTimeEquals(expected, signature)) {
            log.warn("微信支付回调签名不匹配");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "签名校验失败");
        }
    }

    /**
     * 支付宝支付回调验签（RSA-SHA256）。
     *
     * <p>支付宝同步/异步通知签名规则：
     * 将所有参数（sign、sign_type 除外）按字典升序拼接为 key=value&key=value 形式，
     * 使用支付宝公钥通过 SHA256withRSA 验证 sign 字段（Base64 编码）。</p>
     */
    private void verifyAlipayCallback(Map<String, String> params, Map<String, String> headers) {
        if (StringUtils.isBlank(alipayPublicKey)) {
            log.error("支付宝回调验签失败：未配置 alipay.public-key");
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "支付宝未配置公钥");
        }
        String sign = params.get("sign");
        if (StringUtils.isBlank(sign)) {
            log.warn("支付宝回调验签失败：缺少 sign 字段");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "回调签名缺失");
        }
        // 按字典序升序拼接除 sign 和 sign_type 外的所有非空参数
        TreeMap<String, String> sortedParams = new TreeMap<>();
        for (Map.Entry<String, String> entry : params.entrySet()) {
            String key = entry.getKey();
            String value = entry.getValue();
            if ("sign".equals(key) || "sign_type".equals(key)) {
                continue;
            }
            if (StringUtils.isNotBlank(value)) {
                sortedParams.put(key, value);
            }
        }
        StringBuilder signSource = new StringBuilder();
        for (Map.Entry<String, String> entry : sortedParams.entrySet()) {
            if (signSource.length() > 0) {
                signSource.append('&');
            }
            signSource.append(entry.getKey()).append('=').append(entry.getValue());
        }
        if (!verifyRsaSha256(alipayPublicKey, signSource.toString(), sign)) {
            log.warn("支付宝回调签名不匹配");
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "签名校验失败");
        }
    }

    /**
     * 验签通过后的业务处理（幂等）。
     * 当前实现：根据 out_trade_no 找到订单，更新为已支付，并激活 VIP。
     */
    private boolean doCallbackBusiness(Map<String, String> params) {
        // 微信 v3 / 支付宝 均使用 out_trade_no 作为商户订单号字段
        String orderNo = params.get("out_trade_no");
        // 微信为 transaction_id，支付宝为 trade_no
        String tradeNo = params.getOrDefault("transaction_id", params.get("trade_no"));
        if (StringUtils.isBlank(orderNo)) {
            log.warn("回调缺少订单号字段，无法处理业务");
            return false;
        }
        QueryWrapper<PaymentOrder> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("orderNo", orderNo);
        PaymentOrder order = this.getOne(queryWrapper);
        if (order == null) {
            log.warn("回调对应订单不存在，orderNo: {}", orderNo);
            return false;
        }
        // 幂等：订单已支付直接返回成功
        if (order.getStatus() != null && order.getStatus() == 1) {
            log.info("订单已支付，跳过重复处理，orderNo: {}", orderNo);
            return true;
        }
        order.setStatus(1);
        order.setPaymentTime(new Date());
        if (StringUtils.isNotBlank(tradeNo)) {
            order.setTradeNo(tradeNo);
        }
        boolean updated = this.updateById(order);
        if (!updated) {
            log.error("订单状态更新失败，orderNo: {}", orderNo);
            return false;
        }
        // 如果是 VIP 商品，激活会员
        Product product = productService.getById(order.getProductId());
        if (product != null && "vip".equals(product.getType())) {
            String level = StringUtils.isNotBlank(product.getLevel()) ? product.getLevel() : "vip";
            int durationDays = product.getDurationDays() != null ? product.getDurationDays() : 30;
            userVipService.activateVip(order.getUserId(), level, durationDays, order.getId());
        }
        return true;
    }

    /**
     * 计算 HMAC-SHA256 并返回 Base64 编码结果。
     */
    private String hmacSha256(String key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] raw = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(raw);
        } catch (Exception e) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "HMAC-SHA256 计算失败");
        }
    }

    /**
     * 使用 RSA-SHA256 验证签名。
     *
     * @param publicKeyBase64 Base64 编码的 X.509 SubjectPublicKeyInfo
     * @param data            待验签原文
     * @param signBase64      Base64 编码的签名
     */
    private boolean verifyRsaSha256(String publicKeyBase64, String data, String signBase64) {
        try {
            byte[] keyBytes = Base64.getDecoder().decode(publicKeyBase64);
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            KeyFactory keyFactory = KeyFactory.getInstance("RSA");
            PublicKey publicKey = keyFactory.generatePublic(keySpec);
            Signature signature = Signature.getInstance("SHA256withRSA");
            signature.initVerify(publicKey);
            signature.update(data.getBytes(StandardCharsets.UTF_8));
            return signature.verify(Base64.getDecoder().decode(signBase64));
        } catch (Exception e) {
            log.error("RSA-SHA256 验签异常", e);
            return false;
        }
    }

    /**
     * 常量时间字符串比较，避免时序攻击。
     */
    private boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) {
            return false;
        }
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        if (aBytes.length != bBytes.length) {
            return false;
        }
        int result = 0;
        for (int i = 0; i < aBytes.length; i++) {
            result |= aBytes[i] ^ bBytes[i];
        }
        return result == 0;
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
