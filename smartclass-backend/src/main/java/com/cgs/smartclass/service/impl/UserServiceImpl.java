package com.cgs.smartclass.service.impl;

import static com.cgs.smartclass.constant.UserConstant.SALT;
import static com.cgs.smartclass.constant.UserConstant.USER_LOGIN_STATE;

import cn.hutool.core.collection.CollUtil;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.cgs.smartclass.common.ErrorCode;
import com.cgs.smartclass.constant.CommonConstant;
import com.cgs.smartclass.exception.BusinessException;
import com.cgs.smartclass.exception.ThrowUtils;
import com.cgs.smartclass.mapper.UserMapper;
import com.cgs.smartclass.model.dto.user.UserAddRequest;
import com.cgs.smartclass.model.dto.user.UserQueryRequest;
import com.cgs.smartclass.model.dto.user.UserUpdateMyRequest;
import com.cgs.smartclass.model.dto.user.UserUpdateRequest;
import com.cgs.smartclass.model.entity.User;
import com.cgs.smartclass.model.enums.UserRoleEnum;
import com.cgs.smartclass.model.vo.LoginUserVO;
import com.cgs.smartclass.model.vo.UserVO;
import com.cgs.smartclass.service.UserService;
import com.cgs.smartclass.utils.JwtUtil;
import com.cgs.smartclass.utils.SqlUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.ThreadLocalRandom;

import lombok.extern.slf4j.Slf4j;
import me.chanjar.weixin.common.bean.WxOAuth2UserInfo;
import me.chanjar.weixin.common.bean.oauth2.WxOAuth2AccessToken;
import me.chanjar.weixin.mp.api.WxMpService;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Value;
import com.cgs.smartclass.config.WxOpenConfig;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import jakarta.annotation.Resource;

/**
 * 用户服务实现
 */
@Service
@Slf4j
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

    @Resource
    private JwtUtil jwtUtil;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private WxOpenConfig wxOpenConfig;

    @Value("${captcha.enable:true}")
    private boolean captchaEnable;

    // 新增一个用于注册时加锁的 map
    private final ConcurrentHashMap<String, Object> registerLockMap = new ConcurrentHashMap<>();

    @Override
    public long userRegister(String userAccount, String userPassword, String checkPassword, String captchaUuid, String captchaCode) {
        // 0. 验证码校验
        validateCaptcha(captchaUuid, captchaCode);
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户账号过短");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 获取或创建锁对象
        Object lock = registerLockMap.computeIfAbsent(userAccount, k -> new Object());
        synchronized (lock) {
            try {
                // 账户不能重复
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userAccount", userAccount);
                long count = this.baseMapper.selectCount(queryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号重复");
                }

                // 2. 加密
                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

                // 3. 插入数据
                User user = new User();
                user.setUserAccount(userAccount);
                user.setUserPassword(encryptPassword);
                boolean saveResult = this.save(user);
                if (!saveResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
                }
                return user.getId();
            } finally {
                // 注册完成后移除锁对象，避免内存泄漏
                registerLockMap.remove(userAccount);
            }
        }
    }

    // 在类中定义锁 map
    private final Map<String, Object> phoneRegisterLockMap = new ConcurrentHashMap<>();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public long userRegisterByPhone(String userPhone, String userPassword, String checkPassword, String captchaUuid, String captchaCode) {
        // 0. 验证码校验
        validateCaptcha(captchaUuid, captchaCode);
        // 1. 校验
        if (StringUtils.isAnyBlank(userPhone, userPassword, checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!userPhone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }
        if (userPassword.length() < 8 || checkPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户密码过短");
        }
        if (!userPassword.equals(checkPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "两次输入的密码不一致");
        }

        // 加锁注册流程
        Object lock = phoneRegisterLockMap.computeIfAbsent(userPhone, k -> new Object());
        synchronized (lock) {
            try {
                // 手机号不能重复
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("userPhone", userPhone);
                long count = this.baseMapper.selectCount(queryWrapper);
                if (count > 0) {
                    throw new BusinessException(ErrorCode.PARAMS_ERROR, "该手机号已被注册");
                }

                // 随机生成账号（最多尝试10次）
                String userAccount = null;
                int maxAttempts = 10;
                boolean foundUnique = false;
                QueryWrapper<User> accountQueryWrapper = new QueryWrapper<>();

                for (int i = 0; i < maxAttempts; i++) {
                    userAccount = generateRandomAccount();
                    accountQueryWrapper.clear();
                    accountQueryWrapper.eq("userAccount", userAccount);
                    long accountCount = this.baseMapper.selectCount(accountQueryWrapper);
                    if (accountCount == 0) {
                        foundUnique = true;
                        break;
                    }
                }

                if (!foundUnique) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "无法生成唯一账号，请稍后重试");
                }

                // 加密密码
                String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());

                // 插入数据
                User user = new User();
                user.setUserAccount(userAccount);
                user.setUserPhone(userPhone);
                user.setUserPassword(encryptPassword);
                boolean saveResult = this.save(user);
                if (!saveResult) {
                    throw new BusinessException(ErrorCode.SYSTEM_ERROR, "注册失败，数据库错误");
                }
                return user.getId();

            } finally {
                phoneRegisterLockMap.remove(userPhone);
            }
        }
    }


    /**
     * 生成指定长度的随机字符串作为账号
     *
     * @return 随机生成的账号
     */
    private String generateRandomAccount() {
        String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder(10);
        for (int i = 0; i < 10; i++) {

            int index = ThreadLocalRandom.current().nextInt(characters.length());

            sb.append(characters.charAt(index));
        }
        return sb.toString();
    }

    @Override
    public LoginUserVO userLogin(String userAccount, String userPassword, String captchaUuid, String captchaCode, HttpServletRequest request) {
        // 0. 验证码校验
        validateCaptcha(captchaUuid, captchaCode);
        // 1. 校验
        if (StringUtils.isAnyBlank(userAccount, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (userAccount.length() < 4) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "账号错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userAccount", userAccount);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userAccount cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 生成 JWT token 并设置到 response header
        setJwtTokenToResponse(request, user);
        return this.getLoginUserVO(user);
    }

    @Override
    public LoginUserVO userLoginByPhone(String userPhone, String userPassword, String captchaUuid, String captchaCode, HttpServletRequest request) {
        // 0. 验证码校验
        validateCaptcha(captchaUuid, captchaCode);
        // 1. 校验
        if (StringUtils.isAnyBlank(userPhone, userPassword)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "参数为空");
        }
        if (!userPhone.matches("^1[3-9]\\d{9}$")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "手机号格式错误");
        }
        if (userPassword.length() < 8) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "密码错误");
        }
        // 2. 加密
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + userPassword).getBytes());
        // 查询用户是否存在
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userPhone", userPhone);
        queryWrapper.eq("userPassword", encryptPassword);
        User user = this.baseMapper.selectOne(queryWrapper);
        // 用户不存在
        if (user == null) {
            log.info("user login failed, userPhone cannot match userPassword");
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "用户不存在或密码错误");
        }
        // 3. 记录用户的登录态
        request.getSession().setAttribute(USER_LOGIN_STATE, user);
        // 4. 生成 JWT token 并设置到 response header
        setJwtTokenToResponse(request, user);
        return this.getLoginUserVO(user);
    }

    // 在类中定义锁 map
    private final Map<String, Object> wxLoginLockMap = new ConcurrentHashMap<>();

    @Transactional(rollbackFor = Exception.class)
    @Override
    public LoginUserVO userLoginByMpOpen(WxOAuth2UserInfo wxOAuth2UserInfo, HttpServletRequest request) {
        String unionId = wxOAuth2UserInfo.getUnionId();
        String mpOpenId = wxOAuth2UserInfo.getOpenid();

        if (StringUtils.isBlank(unionId)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "微信用户信息为空");
        }

        // 加锁注册流程
        Object lock = wxLoginLockMap.computeIfAbsent(unionId, k -> new Object());
        synchronized (lock) {
            try {
                // 查询用户是否已存在
                QueryWrapper<User> queryWrapper = new QueryWrapper<>();
                queryWrapper.eq("unionId", unionId);
                User user = this.getOne(queryWrapper);

                // 被封号，禁止登录
                if (user != null && UserRoleEnum.BAN.getValue().equals(user.getUserRole())) {
                    throw new BusinessException(ErrorCode.FORBIDDEN_ERROR, "该用户已被封，禁止登录");
                }

                // 用户不存在则创建
                if (user == null) {
                    user = new User();
                    user.setUnionId(unionId);
                    user.setMpOpenId(mpOpenId);
                    user.setUserAvatar(wxOAuth2UserInfo.getHeadImgUrl());
                    user.setUserName(wxOAuth2UserInfo.getNickname());

                    boolean result = this.save(user);
                    if (!result) {
                        throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，用户创建失败");
                    }
                }

                // 记录用户的登录态
                request.getSession().setAttribute(USER_LOGIN_STATE, user);
                return getLoginUserVO(user);

            } finally {
                wxLoginLockMap.remove(unionId);
            }
        }
    }


    /**
     * 验证码校验
     */
    private void validateCaptcha(String captchaUuid, String captchaCode) {
        if (!captchaEnable) {
            return;
        }
        if (StringUtils.isAnyBlank(captchaUuid, captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码不能为空");
        }
        String redisKey = "captcha:" + captchaUuid;
        String cachedCode = stringRedisTemplate.opsForValue().get(redisKey);
        if (cachedCode == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码已过期，请重新获取");
        }
        if (!cachedCode.equalsIgnoreCase(captchaCode)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "验证码错误");
        }
        // 校验通过后删除 Redis 中的验证码
        stringRedisTemplate.delete(redisKey);
    }

    /**
     * 生成 JWT token 并设置到 response header
     */
    private void setJwtTokenToResponse(HttpServletRequest request, User user) {
        try {
            String token = jwtUtil.generateToken(user.getId(), user.getUserRole());
            // 通过 RequestContextHolder 获取 response
            org.springframework.web.context.request.ServletRequestAttributes attributes =
                    (org.springframework.web.context.request.ServletRequestAttributes) org.springframework.web.context.request.RequestContextHolder.getRequestAttributes();
            if (attributes != null) {
                attributes.getResponse().setHeader("Authorization", "Bearer " + token);
            }
        } catch (Exception e) {
            log.warn("生成 JWT token 失败", e);
        }
    }


    /**
     * 获取当前登录用户
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUser(HttpServletRequest request) {
        // 1. 先从 session 获取
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        if (userObj != null) {
            User currentUser = (User) userObj;
            if (currentUser.getId() != null) {
                currentUser = this.getById(currentUser.getId());
                if (currentUser != null) {
                    return currentUser;
                }
            }
        }
        // 2. 尝试从 JWT token 获取
        String token = request.getHeader("Authorization");
        if (StringUtils.isNotBlank(token) && token.startsWith("Bearer ")) {
            token = token.substring(7);
            try {
                if (jwtUtil.validateToken(token)) {
                    Long userId = jwtUtil.getUserIdFromToken(token);
                    User user = this.getById(userId);
                    if (user != null) {
                        // 同步到 session，保持兼容
                        request.getSession().setAttribute(USER_LOGIN_STATE, user);
                        return user;
                    }
                }
            } catch (Exception e) {
                log.warn("JWT token 验证失败", e);
            }
        }
        throw new BusinessException(ErrorCode.NOT_LOGIN_ERROR);
    }

    /**
     * 获取当前登录用户（允许未登录）
     *
     * @param request
     * @return
     */
    @Override
    public User getLoginUserPermitNull(HttpServletRequest request) {
        // 先判断是否已登录
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User currentUser = (User) userObj;
        if (currentUser == null || currentUser.getId() == null) {
            return null;
        }
        // 从数据库查询（追求性能的话可以注释，直接走缓存）
        long userId = currentUser.getId();
        return this.getById(userId);
    }

    /**
     * 是否为管理员
     *
     * @param request
     * @return
     */
    @Override
    public boolean isAdmin(HttpServletRequest request) {
        // 仅管理员可查询
        Object userObj = request.getSession().getAttribute(USER_LOGIN_STATE);
        User user = (User) userObj;
        return isAdmin(user);
    }

    @Override
    public boolean isAdmin(User user) {
        return user != null && UserRoleEnum.ADMIN.getValue().equals(user.getUserRole());
    }

    /**
     * 用户注销
     *
     * @param request
     */
    @Override
    public boolean userLogout(HttpServletRequest request) {
        if (request.getSession().getAttribute(USER_LOGIN_STATE) == null) {
            throw new BusinessException(ErrorCode.OPERATION_ERROR, "未登录");
        }
        // 移除登录态
        request.getSession().removeAttribute(USER_LOGIN_STATE);
        return true;
    }

    @Override
    public LoginUserVO getLoginUserVO(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    @Override
    public UserVO getUserVO(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        BeanUtils.copyProperties(user, userVO);
        if (user.getBirthday() != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(user.getBirthday());
            int birthYear = calendar.get(Calendar.YEAR);
            userVO.setBirthdayYear(birthYear);
        } else {
            userVO.setBirthdayYear(0); // 或者按需设为默认值
        }
        return userVO;
    }

    @Override
    public List<UserVO> getUserVO(List<User> userList) {
        if (CollUtil.isEmpty(userList)) {
            return new ArrayList<>();
        }
        return userList.stream().map(this::getUserVO).collect(Collectors.toList());
    }

    @Override
    public UserVO getUserVOById(Long userId) {
        if (userId == null) {
            return null;
        }
        User user = this.getById(userId);
        return getUserVO(user);
    }

    @Override
    public QueryWrapper<User> getQueryWrapper(UserQueryRequest userQueryRequest) {
        if (userQueryRequest == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "请求参数为空");
        }

        Long id = userQueryRequest.getId();
        String userAccount = userQueryRequest.getUserAccount();
        Integer userGender = userQueryRequest.getUserGender();
        String userPhone = userQueryRequest.getUserPhone();
        String userName = userQueryRequest.getUserName();
        String userRole = userQueryRequest.getUserRole();
        String userEmail = userQueryRequest.getUserEmail();
        String wechatId = userQueryRequest.getWechatId();
        int birthdayYear = userQueryRequest.getBirthdayYear();
        String sortField = userQueryRequest.getSortField();
        String sortOrder = userQueryRequest.getSortOrder();
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq(id != null, "id", id);
        queryWrapper.eq(StringUtils.isNotBlank(userRole), "userRole", userRole);
        queryWrapper.eq(StringUtils.isNotBlank(userAccount), "userAccount", userAccount);
        queryWrapper.eq(userGender != null, "userGender", userGender);
        queryWrapper.eq(StringUtils.isNotBlank(userPhone), "userPhone", userPhone);
        queryWrapper.eq(StringUtils.isNotBlank(userEmail), "userEmail", userEmail);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.like(StringUtils.isNotBlank(wechatId), "wechatId", wechatId);
        queryWrapper.like(StringUtils.isNotBlank(userName), "userName", userName);
        queryWrapper.apply(birthdayYear > 0, "YEAR(birthday) = {0}", birthdayYear);
        queryWrapper.orderBy(SqlUtils.validSortField(sortField), sortOrder.equals(CommonConstant.SORT_ORDER_ASC),
                sortField);
        return queryWrapper;
    }

    @Override
    public List<User> getAllAdmins() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("userRole", UserRoleEnum.ADMIN.getValue());
        queryWrapper.eq("isDelete", 0); // 未删除的用户
        return this.list(queryWrapper);
    }

    @Override
    public long addUser(UserAddRequest userAddRequest) {
        User user = new User();
        BeanUtils.copyProperties(userAddRequest, user);

        // 设置默认密码并加密存储
        String defaultPassword = "12345678";
        String encryptPassword = DigestUtils.md5DigestAsHex((SALT + defaultPassword).getBytes());
        user.setUserPassword(encryptPassword);

        boolean result = this.save(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return user.getId();
    }

    @Override
    public boolean updateUser(UserUpdateRequest userUpdateRequest) {
        User user = new User();
        BeanUtils.copyProperties(userUpdateRequest, user);
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public User getUserByIdWithAuthCheck(long id, HttpServletRequest request) {
        if (id <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR);
        }

        // 获取当前登录用户
        User loginUser = this.getLoginUser(request);

        // 如果不是管理员，则只能查看自己的信息
        if (!this.isAdmin(loginUser) && !loginUser.getId().equals(id)) {
            throw new BusinessException(ErrorCode.NO_AUTH_ERROR, "无权限查看其他用户信息");
        }

        User user = this.getById(id);
        ThrowUtils.throwIf(user == null, ErrorCode.NOT_FOUND_ERROR);
        return user;
    }

    @Override
    public Page<UserVO> listUserVOByPage(UserQueryRequest userQueryRequest) {
        long current = userQueryRequest.getCurrent();
        long size = userQueryRequest.getPageSize();

        // 防止数据泄露或爬虫攻击，限制单页最大记录数
        ThrowUtils.throwIf(size > 20, ErrorCode.PARAMS_ERROR);

        Page<User> userPage = this.page(new Page<>(current, size),
                this.getQueryWrapper(userQueryRequest));

        Page<UserVO> userVOPage = new Page<>(current, size, userPage.getTotal());
        List<UserVO> userVO = this.getUserVO(userPage.getRecords());
        userVOPage.setRecords(userVO);

        return userVOPage;
    }

    @Override
    public boolean updateMyUser(UserUpdateMyRequest userUpdateMyRequest, HttpServletRequest request) {
        User loginUser = this.getLoginUser(request);
        User user = new User();
        BeanUtils.copyProperties(userUpdateMyRequest, user);
        user.setId(loginUser.getId());
        boolean result = this.updateById(user);
        ThrowUtils.throwIf(!result, ErrorCode.OPERATION_ERROR);
        return true;
    }

    @Override
    public LoginUserVO userLoginByWxOpen(String code, HttpServletRequest request) {
        WxOAuth2AccessToken accessToken;
        try {
            WxMpService wxService = wxOpenConfig.getWxMpService();
            accessToken = wxService.getOAuth2Service().getAccessToken(code);
            WxOAuth2UserInfo userInfo = wxService.getOAuth2Service().getUserInfo(accessToken, code);
            String unionId = userInfo.getUnionId();
            String mpOpenId = userInfo.getOpenid();
            if (StringUtils.isAnyBlank(unionId, mpOpenId)) {
                throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
            }
            return this.userLoginByMpOpen(userInfo, request);
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("userLoginByWxOpen error", e);
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "登录失败，系统错误");
        }
    }
}
