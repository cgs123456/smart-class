import { request } from '../client';
import type { User, UserVO, PageResult, PageRequest } from '@smartclass/shared';

/**
 * 用户相关 API
 */
export const userApi = {
  /**
   * 用户登录
   */
  login: (data: { userAccount: string; userPassword: string }) =>
    request.post<{ token: string; refreshToken: string; user: User }>('/user/login', data),
  
  /**
   * 用户注册
   */
  register: (data: { userAccount: string; userPassword: string; checkPassword: string }) =>
    request.post<{ token: string; refreshToken: string; user: User }>('/user/register', data),
  
  /**
   * 手机号登录
   */
  phoneLogin: (data: { phone: string; code: string }) =>
    request.post<{ token: string; refreshToken: string; user: User }>('/user/login/phone', data),
  
  /**
   * 手机号注册
   */
  phoneRegister: (data: { phone: string; code: string; password: string; checkPassword: string }) =>
    request.post<{ token: string; refreshToken: string; user: User }>('/user/register/phone', data),
  
  /**
   * 刷新 Token
   */
  refreshToken: (refreshToken: string) =>
    request.post<{ token: string; refreshToken: string }>('/auth/refresh', { refreshToken }),
  
  /**
   * 登出
   */
  logout: () =>
    request.post('/user/logout'),
  
  /**
   * 获取当前登录用户
   */
  getLoginUser: () =>
    request.get<User>('/user/get/login'),
  
  /**
   * 获取当前登录用户 (允许未登录)
   */
  getLoginUserPermitNull: () =>
    request.get<User | null>('/user/get/login/permitNull'),
  
  /**
   * 根据 ID 获取用户
   */
  getById: (id: number) =>
    request.get<User>(`/user/get/${id}`),
  
  /**
   * 获取用户 VO
   */
  getUserVOById: (userId: number) =>
    request.get<UserVO>(`/user/getUserVO/${userId}`),
  
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
   * 更新用户信息
   */
  updateById: (user: Partial<User> & { id: number }) =>
    request.put<boolean>('/user/updateById', user),
  
  /**
   * 分页查询用户
   */
  pageQuery: (params: PageRequest & {
    userAccount?: string;
    userName?: string;
    userRole?: string;
    province?: string;
    city?: string;
  }) =>
    request.get<PageResult<UserVO>>('/user/page', { params }),
  
  /**
   * 上传头像
   */
  uploadAvatar: (file: File, onProgress?: (progress: number) => void) =>
    request.upload<string>('/user/upload/avatar', file, onProgress),
  
  /**
   * 发送验证码
   */
  sendCaptcha: (email: string) =>
    request.post('/captcha/send', { email }),
  
  /**
   * 校验验证码
   */
  verifyCaptcha: (email: string, code: string) =>
    request.post('/captcha/verify', { email, code })
};

/**
 * 用户状态管理相关
 */
export const userState = {
  /**
   * 设置登录态
   */
  setAuth: (token: string, refreshToken: string, user: User) => {
    localStorage.setItem('smartclass_token', token);
    localStorage.setItem('smartclass_refresh_token', refreshToken);
    localStorage.setItem('smartclass_user_info', JSON.stringify(user));
  },
  
  /**
   * 清除登录态
   */
  clearAuth: () => {
    localStorage.removeItem('smartclass_token');
    localStorage.removeItem('smartclass_refresh_token');
    localStorage.removeItem('smartclass_user_info');
  },
  
  /**
   * 获取当前用户
   */
  getCurrentUser: (): User | null => {
    try {
      const userStr = localStorage.getItem('smartclass_user_info');
      return userStr ? JSON.parse(userStr) : null;
    } catch {
      return null;
    }
  },
  
  /**
   * 获取 Token
   */
  getToken: (): string | null => {
    return localStorage.getItem('smartclass_token');
  },
  
  /**
   * 判断是否登录
   */
  isLoggedIn: (): boolean => {
    return !!localStorage.getItem('smartclass_token');
  },
  
  /**
   * 判断是否为管理员
   */
  isAdmin: (user?: User | null): boolean => {
    const currentUser = user || userState.getCurrentUser();
    return currentUser?.userRole === 'admin';
  }
};