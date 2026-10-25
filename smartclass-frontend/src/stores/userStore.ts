import { defineStore } from 'pinia';
import { ref } from 'vue';
import { UserControllerService } from '../services';
import { OpenAPI } from '../services/core/OpenAPI';
import { request as __request } from '../services/core/request';
import type { LoginUserVO } from '../services';

// 默认用户头像路径
export const DEFAULT_USER_AVATAR = '/default.jpg';

export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref<LoginUserVO | null>(null);

  // 初始化用户信息
  try {
    const storedUserInfo = localStorage.getItem('userInfo');
    if (storedUserInfo) {
      userInfo.value = JSON.parse(storedUserInfo);
    }
  } catch (error) {
    console.error('Failed to parse user info from localStorage', error);
  }

  // 获取当前登录用户信息
  const fetchCurrentUser = async () => {
    try {
      const response = await UserControllerService.getLoginUserUsingGet();
      if (response.code === 0 && response.data) {
        // 确保用户头像有默认值
        if (!response.data.userAvatar) {
          response.data.userAvatar = DEFAULT_USER_AVATAR;
        }
        userInfo.value = response.data;
        localStorage.setItem('userInfo', JSON.stringify(response.data));
        return response.data;
      }
      // 如果响应码不为0或没有数据，保持当前状态，不清除用户信息
      // 确保至少有默认值
      if (!userInfo.value) {
        userInfo.value = { userAvatar: DEFAULT_USER_AVATAR } as LoginUserVO;
      }
      return userInfo.value;
    } catch (error) {
      console.error('Failed to fetch current user', error);
      const err = error as { status?: number; response?: { status?: number } };
      const status = err?.status ?? err?.response?.status;
      if (status === 401 || status === 403) {
        userInfo.value = null;
        localStorage.removeItem('userInfo');
        if (window.location.pathname !== '/login') {
          window.location.href = '/login';
        }
        return null;
      }
      // 其他异常时，保留现有用户状态，返回当前的userInfo
      if (!userInfo.value) {
        userInfo.value = { userAvatar: DEFAULT_USER_AVATAR } as LoginUserVO;
      }
      return userInfo.value;
    }
  };

  // 用户名登录
  const login = async (
    userAccount: string,
    userPassword: string,
    captchaUuid?: string,
    captchaCode?: string,
  ) => {
    try {
      const response = await UserControllerService.userLoginUsingPost({
        userAccount,
        userPassword,
        captchaUuid,
        captchaCode,
      });

      if (response.code === 0 && response.data) {
        // 确保用户头像有默认值
        if (!response.data.userAvatar) {
          response.data.userAvatar = DEFAULT_USER_AVATAR;
        }
        userInfo.value = response.data;
        localStorage.setItem('userInfo', JSON.stringify(response.data));
        return { success: true, data: response.data };
      }

      return { success: false, message: response.message || '登录失败' };
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error ? error.message : '登录失败，请检查网络连接';
      return {
        success: false,
        message: errorMessage,
      };
    }
  };

  // 手机号登录
  const loginByPhone = async (
    userPhone: string,
    userPassword: string,
    captchaUuid?: string,
    captchaCode?: string,
  ) => {
    try {
      const response = await UserControllerService.userLoginByPhoneUsingPost({
        userPhone,
        userPassword,
        captchaUuid,
        captchaCode,
      });

      if (response.code === 0 && response.data) {
        // 确保用户头像有默认值
        if (!response.data.userAvatar) {
          response.data.userAvatar = DEFAULT_USER_AVATAR;
        }
        userInfo.value = response.data;
        localStorage.setItem('userInfo', JSON.stringify(response.data));
        return { success: true, data: response.data };
      }

      return { success: false, message: response.message || '登录失败' };
    } catch (error: unknown) {
      const errorMessage =
        error instanceof Error ? error.message : '登录失败，请检查网络连接';
      return {
        success: false,
        message: errorMessage,
      };
    }
  };

  // 获取图形验证码（后端 captcha.enable=false 时返回 enabled=false）
  const fetchCaptcha = async (): Promise<{
    enabled: boolean;
    uuid: string;
    image: string;
  }> => {
    try {
      const response = await __request<{
        code: number;
        data?: { uuid?: string; captchaImage?: string; captchaEnable?: string };
      }>(OpenAPI, {
        method: 'GET',
        url: '/api/captcha',
      });
      if (response?.code === 0 && response.data) {
        if (response.data.captchaEnable === 'false') {
          return { enabled: false, uuid: '', image: '' };
        }
        return {
          enabled: true,
          uuid: response.data.uuid || '',
          image: response.data.captchaImage || '',
        };
      }
      return { enabled: true, uuid: '', image: '' };
    } catch (error) {
      console.error('Failed to fetch captcha', error);
      return { enabled: true, uuid: '', image: '' };
    }
  };

  // 登出
  const logout = async () => {
    try {
      await UserControllerService.userLogoutUsingPost();
    } catch (error) {
      console.error('Logout API call failed', error);
    } finally {
      // 无论API调用是否成功，都清除本地状态
      userInfo.value = null;
      localStorage.removeItem('userInfo');
    }
  };

  // 获取用户头像，确保有默认值
  const getUserAvatar = () => {
    return userInfo.value?.userAvatar || DEFAULT_USER_AVATAR;
  };

  // 登录态 = 存在真实用户 id（游客兜底对象只有默认头像、无 id）
  const isLoggedIn = () => !!userInfo.value?.id;

  return {
    userInfo,
    login,
    loginByPhone,
    fetchCaptcha,
    logout,
    fetchCurrentUser,
    getUserAvatar,
    isLoggedIn,
    DEFAULT_USER_AVATAR,
  };
});
