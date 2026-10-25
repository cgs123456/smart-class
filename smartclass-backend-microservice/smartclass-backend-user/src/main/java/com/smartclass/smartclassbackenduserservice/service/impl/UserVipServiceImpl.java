package com.smartclass.smartclassbackenduserservice.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.smartclass.smartclassbackendcommon.common.ErrorCode;
import com.smartclass.smartclassbackendcommon.exception.BusinessException;
import com.smartclass.smartclassbackendmodel.model.entity.UserVip;
import com.smartclass.smartclassbackenduserservice.mapper.UserVipMapper;
import com.smartclass.smartclassbackenduserservice.service.UserVipService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.Calendar;
import java.util.Date;

/**
 * 用户会员服务实现
 *
 * 逻辑与单体版 UserVipServiceImpl.activateVip 对齐（新建或续期叠加），
 * 并增加 paymentOrderId 幂等键，保证支付渠道回调重试时不会重复叠加时长。
 */
@Service
public class UserVipServiceImpl extends ServiceImpl<UserVipMapper, UserVip>
        implements UserVipService {

    @Override
    public boolean activateVip(Long userId, String level, int durationDays, Long paymentOrderId) {
        if (userId == null || StringUtils.isBlank(level)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        // 幂等：该订单已激活过（记录存在且未过期），不重复叠加
        if (paymentOrderId != null) {
            QueryWrapper<UserVip> idempotentWrapper = new QueryWrapper<>();
            idempotentWrapper.eq("paymentOrderId", paymentOrderId);
            UserVip existed = this.getOne(idempotentWrapper);
            if (existed != null
                    && (existed.getExpireTime() == null || existed.getExpireTime().after(new Date()))) {
                return true;
            }
        }
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        UserVip userVip = this.getOne(queryWrapper);
        if (userVip == null) {
            // 新建VIP记录
            userVip = new UserVip();
            userVip.setUserId(userId);
            userVip.setLevel(level);
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.DAY_OF_MONTH, durationDays);
            userVip.setExpireTime(calendar.getTime());
            userVip.setAutoRenew(0);
            userVip.setPaymentOrderId(paymentOrderId);
            return this.save(userVip);
        }
        // 如果已有VIP且未过期，在原过期时间上续期
        Date baseTime = userVip.getExpireTime();
        if (baseTime == null || baseTime.before(new Date())) {
            baseTime = new Date();
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTime(baseTime);
        calendar.add(Calendar.DAY_OF_MONTH, durationDays);
        userVip.setLevel(level);
        userVip.setExpireTime(calendar.getTime());
        userVip.setPaymentOrderId(paymentOrderId);
        return this.updateById(userVip);
    }
}
