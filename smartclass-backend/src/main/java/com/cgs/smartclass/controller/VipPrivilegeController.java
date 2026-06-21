package com.cgs.smartclass.controller;

import com.cgs.smartclass.annotation.AuthCheck;
import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.constant.UserConstant;
import com.cgs.smartclass.model.entity.VipPrivilege;
import com.cgs.smartclass.model.vo.VipPrivilegeVO;
import com.cgs.smartclass.service.VipPrivilegeService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import java.util.List;

/**
 * VIP权益管理接口（管理端）
 */
@RestController
@RequestMapping("/vip/privilege")
@Slf4j
public class VipPrivilegeController {

    @Resource
    private VipPrivilegeService vipPrivilegeService;

    /**
     * 权益列表
     */
    @GetMapping("/list")
    public BaseResponse<List<VipPrivilegeVO>> listPrivileges() {
        List<VipPrivilegeVO> list = vipPrivilegeService.listAll();
        return ResultUtils.success(list);
    }

    /**
     * 添加权益
     */
    @PostMapping("/add")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Long> addPrivilege(@RequestBody VipPrivilege privilege) {
        boolean result = vipPrivilegeService.save(privilege);
        return ResultUtils.success(privilege.getId());
    }

    /**
     * 更新权益
     */
    @PostMapping("/update")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> updatePrivilege(@RequestBody VipPrivilege privilege) {
        boolean result = vipPrivilegeService.updateById(privilege);
        return ResultUtils.success(result);
    }

    /**
     * 删除权益
     */
    @PostMapping("/delete")
    @AuthCheck(mustRole = UserConstant.ADMIN_ROLE)
    public BaseResponse<Boolean> deletePrivilege(@RequestBody VipPrivilege privilege) {
        boolean result = vipPrivilegeService.removeById(privilege.getId());
        return ResultUtils.success(result);
    }
}
