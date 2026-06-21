package com.cgs.smartclass.controller;

import com.cgs.smartclass.common.BaseResponse;
import com.cgs.smartclass.common.ResultUtils;
import com.cgs.smartclass.model.vo.UserVipVO;
import com.cgs.smartclass.model.vo.VipPrivilegeVO;
import com.cgs.smartclass.service.UserVipService;
import com.cgs.smartclass.service.VipPrivilegeService;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.model.entity.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import java.util.List;

/**
 * 用户VIP接口
 */
@RestController
@RequestMapping("/vip")
@Slf4j
public class UserVipController {

    @Resource
    private UserVipService userVipService;

    @Resource
    private VipPrivilegeService vipPrivilegeService;

    @Resource
    private UserService userService;

    /**
     * 获取我的VIP信息
     */
    @GetMapping("/my")
    public BaseResponse<UserVipVO> getMyVip(HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        UserVipVO vo = userVipService.getUserVip(loginUser.getId());
        return ResultUtils.success(vo);
    }

    /**
     * 获取VIP权益列表
     */
    @GetMapping("/privileges")
    public BaseResponse<List<VipPrivilegeVO>> listPrivileges(@RequestParam(required = false) String level) {
        List<VipPrivilegeVO> list;
        if (level != null) {
            list = vipPrivilegeService.listByLevel(level);
        } else {
            list = vipPrivilegeService.listAll();
        }
        return ResultUtils.success(list);
    }

    /**
     * 检查是否有某项权益
     */
    @GetMapping("/check/{featureKey}")
    public BaseResponse<Boolean> checkPrivilege(@PathVariable String featureKey,
                                                 HttpServletRequest request) {
        User loginUser = userService.getLoginUser(request);
        boolean hasPrivilege = userVipService.checkPrivilege(loginUser.getId(), featureKey);
        return ResultUtils.success(hasPrivilege);
    }
}
