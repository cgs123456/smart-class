package com.cgs.smartclass.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.mapper.VipPrivilegeMapper;
import com.cgs.smartclass.model.entity.VipPrivilege;
import com.cgs.smartclass.model.vo.VipPrivilegeVO;
import com.cgs.smartclass.service.VipPrivilegeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * VIP权益配置服务实现
 */
@Service
@Slf4j
public class VipPrivilegeServiceImpl extends ServiceImpl<VipPrivilegeMapper, VipPrivilege>
        implements VipPrivilegeService {

    @Override
    public List<VipPrivilegeVO> listByLevel(String level) {
        QueryWrapper<VipPrivilege> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("level", level);
        List<VipPrivilege> list = this.list(queryWrapper);
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    @Override
    public List<VipPrivilegeVO> listAll() {
        List<VipPrivilege> list = this.list();
        return list.stream().map(this::convertToVO).collect(Collectors.toList());
    }

    /**
     * 转换为VO
     */
    private VipPrivilegeVO convertToVO(VipPrivilege privilege) {
        if (privilege == null) {
            return null;
        }
        VipPrivilegeVO vo = new VipPrivilegeVO();
        BeanUtils.copyProperties(privilege, vo);
        return vo;
    }
}
