package com.smartclass.smartclass.service;

import com.smartclass.smartclass.exception.BusinessException;
import jakarta.annotation.Resource;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

/**
 * 用户注册参数校验测试：只覆盖不依赖外部状态的校验分支，
 * 用 assertThrows 断言 BusinessException，不再吞异常导致恒通过。
 */
@SpringBootTest
public class UserServiceTest {

    @Resource
    private UserService userService;

    @Test
    void userRegisterRejectsInvalidInput() {
        // 验证码缺失在第一道校验即被拒绝
        Assertions.assertThrows(BusinessException.class, () ->
                userService.userRegister("testuser", null, "", "123456", null, null));
        // 账号过短
        Assertions.assertThrows(BusinessException.class, () ->
                userService.userRegister("te", null, "12345678", "12345678", null, null));
        // 两次密码不一致
        Assertions.assertThrows(BusinessException.class, () ->
                userService.userRegister("testuser2", null, "12345678", "87654321", null, null));
    }
}
