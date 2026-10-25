package com.smartclass.smartclassbackenduserservice.dubbo;

import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendserviceclient.dubbo.UserDubboService;
import com.smartclass.smartclassbackendmodel.model.vo.UserVO;
import com.smartclass.smartclassbackenduserservice.service.UserService;
import com.smartclass.smartclassbackenduserservice.service.UserVipService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户 Dubbo 服务实现
 * 
 * 替代原 UserInnerController (Feign 内部接口)
 * 协议: Triple (HTTP/2 + Protobuf)
 * 注册中心: Nacos
 */
@DubboService(
    version = "1.0.0",
    protocol = "triple",
    interfaceClass = UserDubboService.class
)
public class UserDubboServiceImpl implements UserDubboService {

    @Autowired
    private UserService userService;

    @Autowired
    private UserVipService userVipService;

    @Override
    public User getById(Long id) {
        return desensitize(userService.getById(id));
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        return userService.getUserVO(userList);
    }

    @Override
    public UserVO getUserVOById(Long userId) {
        return userService.getUserVOById(userId);
    }

    @Override
    public List<User> listByIds(Collection<Long> userIds) {
        return userService.listByIds(userIds).stream()
                .map(this::desensitize)
                .collect(Collectors.toList());
    }

    @Override
    public boolean updateById(User user) {
        // 内部通道禁止改凭证与角色，防消费端经此提权/改密
        user.setUserPassword(null);
        user.setUserRole(null);
        user.setUserAccount(null);
        return userService.updateById(user);
    }

    @Override
    public boolean activateVip(Long userId, String level, int durationDays, Long paymentOrderId) {
        return userVipService.activateVip(userId, level, durationDays, paymentOrderId);
    }

    /**
     * 跨服务返回前抹掉密码哈希；MyBatis-Plus updateById 忽略 null 字段，不影响后续编辑用途
     */
    private User desensitize(User user) {
        if (user != null) {
            user.setUserPassword(null);
        }
        return user;
    }
}