package com.smartclass.smartclassbackendserviceclient.utils;

import com.smartclass.smartclassbackendcommon.common.ErrorCode;
import com.smartclass.smartclassbackendcommon.constant.UserConstant;
import com.smartclass.smartclassbackendcommon.exception.BusinessException;
import com.smartclass.smartclassbackendmodel.model.entity.User;
import com.smartclass.smartclassbackendmodel.model.enums.UserRoleEnum;
import jakarta.servlet.http.HttpServletRequest;

/**
 * 会话登录用户工具类
 *
 * <p>用户登录后，user 服务会将完整的 User 实体写入 Session（由 Redis 分布式 Session 共享），
 * 各业务服务可直接从 Session 读取，无需额外的 RPC 调用。</p>
 *
 * <p>原 UserFeignClient.getLoginUser(request) 的"会话取 ID 再远程刷新"逻辑：
 * 如需最新数据，可使用 {@link #getLoginUserId(HttpServletRequest)} 后自行调用
 * UserDubboService.getById 刷新。</p>
 */
public final class UserSessionUtils {

    private UserSessionUtils() {
    }

    /**
     * 获取当前登录用户（未登录时抛出 NOT_LOGIN_ERROR）
     */
    public static User getLoginUser(HttpServletRequest request) {
        User user = getLoginUserPermitNull(request);
        if (user == null) {
            throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
        }
        return user;
    }

    /**
     * 获取当前登录用户（允许未登录，返回 null）
     */
    public static User getLoginUserPermitNull(HttpServletRequest request) {
        Object userObj = request.getSession().getAttribute(UserConstant.USER_LOGIN_STATE);
        return userObj instanceof User ? (User) userObj : null;
    }

    /**
     * 获取当前登录用户 ID（未登录时抛出 NOT_LOGIN_ERROR）
     */
    public static Long getLoginUserId(HttpServletRequest request) {
        return getLoginUser(request).getId();
    }

    /**
     * 获取当前登录用户 ID（允许未登录，返回 null）
     */
    public static Long getLoginUserIdPermitNull(HttpServletRequest request) {
        User user = getLoginUserPermitNull(request);
        return user == null ? null : user.getId();
    }

    /**
     * 判断指定用户是否为管理员
     */
    public static boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 判断当前会话用户是否为管理员
     */
    public static boolean isAdmin(HttpServletRequest request) {
        return isAdmin(getLoginUserPermitNull(request));
    }
}
