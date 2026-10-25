package com.smartclass.smartclassbackenduserservice.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.smartclass.smartclassbackendmodel.model.entity.UserVip;

/**
 * 用户会员服务
 */
public interface UserVipService extends IService<UserVip> {

    /**
     * 激活/续期会员
     *
     * 幂等说明：以 paymentOrderId 作为激活幂等键——若该订单已激活过（记录仍在且未过期），
     * 直接返回成功，不叠加时长。这是跨服务支付回调重试的安全前提：
     * pay 服务可能在订单已支付后重放激活请求（渠道回调重试 / doCallbackBusiness 重入）。
     * 已知边界：行上仅保存最近一次 paymentOrderId，若用户先完成订单 B 续费、再重放更早的
     * 订单 A 的激活，幂等键不复存在会重复叠加——需触发条件罕见（A 首次激活失败且其后
     * 完成另一笔订单且渠道重试 A），彻底解法为独立激活流水表（见评审报告 P1-13）。
     *
     * @param userId         用户ID
     * @param level          会员等级
     * @param durationDays   时长（天）
     * @param paymentOrderId 关联支付订单ID（幂等键）
     * @return 是否成功
     */
    boolean activateVip(Long userId, String level, int durationDays, Long paymentOrderId);
}
