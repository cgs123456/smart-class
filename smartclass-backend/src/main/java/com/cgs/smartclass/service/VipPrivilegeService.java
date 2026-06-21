package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.entity.VipPrivilege;
import com.cgs.smartclass.model.vo.VipPrivilegeVO;

import java.util.List;

/**
 * VIP权益配置服务
 */
public interface VipPrivilegeService extends IService<VipPrivilege> {

    /**
     * 根据等级获取权益列表
     *
     * @param level VIP等级
     * @return 权益列表
     */
    List<VipPrivilegeVO> listByLevel(String level);

    /**
     * 获取所有权益列表
     *
     * @return 权益列表
     */
    List<VipPrivilegeVO> listAll();
}
