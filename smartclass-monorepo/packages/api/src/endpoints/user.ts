import { request } from '../client';
import { CACHED_USER_KEY } from '../client';
import type { User, UserVO, LoginUserVO, PageResult } from '@smartclass/shared';

/**
 * 用户相关 API —— 与单体后端 UserController/CaptchaController 实际契约对齐：
 * 认证为 Session Cookie（无 token/refreshToken），登录/注册在验证码开关开启时
 * 需先调 getCaptcha 并提交 captchaUuid + captchaCode。
 */
export const userApi = {
  /**
   * 获取图形验证码（后端 captcha.enable=true 时登录/注册必填）
   * 返回 { uuid, captchaImage(base64) }；开关关闭时返回 { captchaEnable: "false" }
   */
  getCaptcha: () =>
    request.get<Record<string, string>>('/captcha'),

  /**
   * 用户登录（返回 LoginUserVO，会话 Cookie 由后端 Set-Cookie 下发）
   */
  login: (data: { userAccount: string; userPassword: string; captchaUuid?: string; captchaCode?: string }) =>
    request.post<LoginUserVO>('/user/login', data),

  /**
   * 用户注册（返回新用户 id）
   */
  register: (data: { userAccount: string; userPassword: string; checkPassword: string; captchaUuid?: string; captchaCode?: string }) =>
    request.post<number>('/user/register', data),

  /**
   * 手机号登录
   */
  phoneLogin: (data: { userPhone: string; userPassword: string; captchaUuid?: string; captchaCode?: string }) =>
    request.post<LoginUserVO>('/user/login/phone', data),

  /**
   * 手机号注册（返回新用户 id）
   */
  phoneRegister: (data: { userPhone: string; userPassword: string; checkPassword: string; captchaUuid?: string; captchaCode?: string }) =>
    request.post<number>('/user/register/phone', data),

  /**
   * 登出（后端销毁会话）
   */
  logout: () =>
    request.post<boolean>('/user/logout'),

  /**
   * 获取当前登录用户（会话失效时后端返回 40100）
   */
  getLoginUser: () =>
    request.get<LoginUserVO>('/user/get/login'),

  /**
   * 根据 ID 获取用户（管理端）
   */
  getById: (id: number) =>
    request.get<User>('/user/get', { params: { id } }),

  /**
   * 获取用户 VO
   */
  getUserVOById: (userId: number) =>
    request.get<UserVO>('/user/get/vo', { params: { userId } }),

  /**
   * 批量获取用户 VO
   */
  getUserVOList: (userList: User[]) =>
    request.post<UserVO[]>('/user/getUserVOList', userList),

  /**
   * 批量获取用户信息
   */
  listByIds: (userIds: number[]) =>
    request.post<User[]>('/user/listByIds', userIds),

  /**
   * 分页查询用户（VO）
   */
  pageQuery: (params: {
    current?: number;
    pageSize?: number;
    userAccount?: string;
    userName?: string;
    userRole?: string;
    province?: string;
    city?: string;
  }) =>
    request.post<PageResult<UserVO>>('/user/list/page/vo', params),

  /**
   * 更新当前用户资料
   */
  updateMy: (user: Partial<Pick<User, 'userName' | 'userAvatar' | 'userProfile'>>) =>
    request.post<boolean>('/user/update/my', user),

  /**
   * 上传头像（FileController /file/upload/avatar）
   */
  uploadAvatar: (file: File, onProgress?: (progress: number) => void) =>
    request.upload<string>('/file/upload/avatar', file, onProgress)
};

/**
 * 登录态辅助：凭证在后端 Session（Cookie），本地仅缓存展示用用户信息。
 * 真实登录态判断请以 userApi.getLoginUser() / 401 事件为准。
 */
export const userState = {
  /** 登录成功后缓存用户展示信息 */
  cacheUser: (user: LoginUserVO) => {
    localStorage.setItem(CACHED_USER_KEY, JSON.stringify(user));
  },

  /** 登出/会话失效时清除缓存 */
  clearCache: () => {
    localStorage.removeItem(CACHED_USER_KEY);
  },

  /** 读取缓存的用户信息（可能过期，仅供首屏占位） */
  getCachedUser: (): LoginUserVO | null => {
    try {
      const userStr = localStorage.getItem(CACHED_USER_KEY);
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },

  /** 判断是否为管理员（基于缓存，权威判断走接口） */
  isAdmin: (user?: LoginUserVO | null): boolean => {
    const currentUser = user || userState.getCachedUser();
    return currentUser?.userRole === 'admin';
  }
};
