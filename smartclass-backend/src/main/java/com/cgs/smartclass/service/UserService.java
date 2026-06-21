package com.cgs.smartclass.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.cgs.smartclass.model.dto.user.UserAddRequest;
import com.cgs.smartclass.model.dto.user.UserQueryRequest;
import com.cgs.smartclass.model.dto.user.UserUpdateMyRequest;
import com.cgs.smartclass.model.dto.user.UserUpdateRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.vo.LoginUserVO;
import com.cgs.smartclass.model.vo.UserVO;
import java.util.List;
import jakarta.servlet.http.HttpServletRequest;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;

/**
 * 用户服务
*/
public interface UserService extends IService<User> {

    /**
     * 用户注册
     *
     * @param userAccount   用户账户
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param captchaUuid   验证码UUID
     * @param captchaCode   验证码
     * @return 新用户 id
     */
    long userRegister(String userAccount, String userPassword, String checkPassword, String captchaUuid, String captchaCode);

    /**
     * 用户手机号注册
     *
     * @param userPhone     用户手机号
     * @param userPassword  用户密码
     * @param checkPassword 校验密码
     * @param captchaUuid   验证码UUID
     * @param captchaCode   验证码
     * @return 新用户 id
     */
    long userRegisterByPhone(String userPhone, String userPassword, String checkPassword, String captchaUuid, String captchaCode);

    /**
     * 用户登录
     *
     * @param userAccount  用户账户
     * @param userPassword 用户密码
     * @param captchaUuid  验证码UUID
     * @param captchaCode  验证码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLogin(String userAccount, String userPassword, String captchaUuid, String captchaCode, HttpServletRequest request);

    /**
     * 用户手机号登录
     *
     * @param userPhone  用户手机号
     * @param userPassword 用户密码
     * @param captchaUuid  验证码UUID
     * @param captchaCode  验证码
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLoginByPhone(String userPhone, String userPassword, String captchaUuid, String captchaCode, HttpServletRequest request);

    /**
     * 用户登录（微信开放平台）
     *
     * @param wxOAuth2UserInfo 从微信获取的用户信息
     * @param request
     * @return 脱敏后的用户信息
     */
    LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request);

    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    User getLoginUser(HttpServletRequest request);

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    User getLoginUserPermitNull(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    boolean isAdmin(HttpServletRequest request);

    /**
     * 是否为管理员
     *
     * @param user
     * @return
     */
    boolean isAdmin(User user);

    /**
     * 用户注销
     *
     * @param request
     * @return
     */
    boolean userLogout(HttpServletRequest request);

    /**
     * 获取脱敏的已登录用户信息
     *
     * @return
     */
    LoginUserVO getLoginUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param user
     * @return
     */
    UserVO getUserVO(User user);

    /**
     * 获取脱敏的用户信息
     *
     * @param userList
     * @return
     */
    List<UserVO> getUserVO(List<User> userList);
    
    /**
     * 根据用户ID获取脱敏的用户信息
     *
     * @param userId 用户ID
     * @return 脱敏后的用户信息
     */
    UserVO getUserVOById(Long userId);

    /**
     * 获取查询条件
     *
     * @param userQueryRequest
     * @return
     */
    QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest);

    /**
     * 获取所有管理员用户
     *
     * @return 管理员用户列表
     */
    List<User> getAllAdmins();

    /**
     * 添加用户（管理员操作）
     * 包含DTO转Entity、默认密码加密等业务逻辑
     *
     * @param userAddRequest 用户添加请求
     * @return 新用户ID
     */
    long addUser(UserAddRequest userAddRequest);

    /**
     * 更新用户信息（管理员操作）
     * 包含DTO转Entity逻辑
     *
     * @param userUpdateRequest 用户更新请求
     * @return 是否更新成功
     */
    boolean updateUser(UserUpdateRequest userUpdateRequest);

    /**
     * 根据ID获取用户信息（含权限校验）
     * 非管理员只能查看自己的信息
     *
     * @param id      用户ID
     * @param request HTTP请求
     * @return 用户信息
     */
    User getUserByIdWithAuthCheck(long id, HttpServletRequest request);

    /**
     * 分页获取用户VO列表
     * 包含分页大小限制、VO转换逻辑
     *
     * @param userQueryRequest 查询请求
     * @return 用户VO分页
     */
    Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest);

    /**
     * 更新当前登录用户个人信息
     * 包含DTO转Entity、设置当前用户ID逻辑
     *
     * @param userUpdateMyRequest 更新请求
     * @param request             HTTP请求
     * @return 是否更新成功
     */
    boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request);

    /**
     * 微信开放平台登录
     * 包含OAuth交互逻辑
     *
     * @param code    微信授权码
     * @param request HTTP请求
     * @return 登录用户信息
     */
    LoginUserVO userLoginByWxOpen(String code, HttpServletRequest request);

}
