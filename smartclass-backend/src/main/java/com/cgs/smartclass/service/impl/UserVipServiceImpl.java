package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.mapper.UserVipMapper;
import com.cgs.smartclass.model.entity.UserVip;
import com.cgs.smartclass.model.entity.VipPrivilege;
import com.cgs.smartclass.model.vo.UserVipVO;
import com.cgs.smartclass.model.vo.VipPrivilegeVO;
import com.cgs.smartclass.service.UserVipService;
import com.cgs.smartclass.service.VipPrivilegeService;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import jakarta.annotation.Resource;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户VIP服务实现
 */
@Service
@Slf4j
public class UserVipServiceImpl extends ServiceImpl<UserVipMapper, UserVip>
        implements UserVipService {

    @Resource
    private VipPrivilegeService vipPrivilegeService;

    @Override
    public UserVipVO getUserVip(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        UserVip userVip = this.getOne(queryWrapper);
        UserVipVO vo = new UserVipVO();
        if (userVip == null) {
            // 默认为free用户
            vo.setUserId(userId);
            vo.setLevel("free");
            vo.setIsActive(true);
            List<VipPrivilegeVO> privileges = vipPrivilegeService.listByLevel("free");
            vo.setPrivileges(privileges);
            return vo;
        }
        BeanUtils.copyProperties(userVip, vo);
        // 检查是否过期
        boolean isActive = userVip.getExpireTime() == null || userVip.getExpireTime().after(new Date());
        vo.setIsActive(isActive);
        // 如果过期，降级为free
        String level = isActive ? userVip.getLevel() : "free";
        List<VipPrivilegeVO> privileges = vipPrivilegeService.listByLevel(level);
        vo.setPrivileges(privileges);
        return vo;
    }

    @Override
    public boolean activateVip(Long userId, String level, int durationDays, Long paymentOrderId) {
        if (userId == null || StringUtils.isBlank(level)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
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

    @Override
    public boolean checkPrivilege(Long userId, String featureKey) {
        int limit = getPrivilegeLimit(userId, featureKey);
        return limit != 0;
    }

    @Override
    public int getPrivilegeLimit(Long userId, String featureKey) {
        if (userId == null || StringUtils.isBlank(featureKey)) {
            return 0;
        }
        // 获取用户当前等级
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        UserVip userVip = this.getOne(queryWrapper);
        String level = "free";
        if (userVip != null) {
            boolean isActive = userVip.getExpireTime() == null || userVip.getExpireTime().after(new Date());
            if (isActive) {
                level = userVip.getLevel();
            }
        }
        // 查询权益限制
        QueryWrapper<VipPrivilege> privilegeQueryWrapper = new QueryWrapper<>();
        privilegeQueryWrapper.eq("level", level);
        privilegeQueryWrapper.eq("featureKey", featureKey);
        VipPrivilege privilege = vipPrivilegeService.getOne(privilegeQueryWrapper);
        if (privilege == null) {
            return 0;
        }
        return privilege.getLimitCount() != null ? privilege.getLimitCount() : -1;
    }

    @Override
    public boolean deactivateVip(Long userId) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }
        QueryWrapper<UserVip> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userId", userId);
        UserVip userVip = this.getOne(queryWrapper);
        if (userVip == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND_ERROR, "用户VIP信息不存在");
        }
        userVip.setLevel("free");
        userVip.setExpireTime(null);
        userVip.setAutoRenew(0);
        return this.updateById(userVip);
    }
}
