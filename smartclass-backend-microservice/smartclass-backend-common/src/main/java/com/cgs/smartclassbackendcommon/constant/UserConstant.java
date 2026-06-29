package com.cgs.smartclassbackendcommon.constant;

/**
 * 用户常量
*/
public interface UserConstant {

    /**
     * 用户登录态键
     */
    String USER_LOGIN_STATE = "user_login";

    //  region 权限

    /**
     * 默认角色
     */
    String DEFAULT_ROLE = "user";

    /**
     * 管理员角色
     */
    String ADMIN_ROLE = "admin";

    /**
     * 被封号
     */
    String BAN_ROLE = "ban";

    // 原硬编码 SALT="yupi" 已废弃，密码加密迁移至 BCrypt（user 服务 PasswordEncoderConfig）

    // endregion
}
