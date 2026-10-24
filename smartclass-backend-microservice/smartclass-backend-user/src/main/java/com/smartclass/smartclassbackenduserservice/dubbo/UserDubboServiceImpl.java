package com.smartclass.smartclassbackenduserservice.dubbo;

import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendserviceclient.dubbo.UserDubboService;
import com.smartclass.smartclassbackendmodel.model.vo.UserVO;
import com.smartclass.smartclassbackenduserservice.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collection;
import java.util.List;

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

    @Override
    public User getById(Long id) {
        return userService.getById(id);
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
        return userService.listByIds(userIds);
    }

    @Override
    public boolean updateById(User user) {
        return userService.updateById(user);
    }
}