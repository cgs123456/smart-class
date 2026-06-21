package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.entity.UserVip;
import com.cgs.smartclass.model.vo.UserVipVO;

/**
 * 用户VIP服务
 */
public interface UserVipService extends IService<UserVip> {

    /**
     * 获取用户VIP信息
     *
     * @param userId 用户ID
     * @return VIP视图
     */
    UserVipVO getUserVip(Long userId);

    /**
     * 激活或续费VIP
     *
     * @param userId          用户ID
     * @param level           VIP等级
     * @param durationDays    有效天数
     * @param paymentOrderId  关联订单ID
     * @return 是否成功
     */
    boolean activateVip(Long userId, String level, int durationDays, Long paymentOrderId);

    /**
     * 检查用户是否有某项权益
     *
     * @param userId     用户ID
     * @param featureKey 功能标识
     * @return 是否有权益
     */
    boolean checkPrivilege(Long userId, String featureKey);

    /**
     * 获取用户某项权益的限制次数
     *
     * @param userId     用户ID
     * @param featureKey 功能标识
     * @return 限制次数
     */
    int getPrivilegeLimit(Long userId, String featureKey);

    /**
     * 取消VIP
     *
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deactivateVip(Long userId);
}
