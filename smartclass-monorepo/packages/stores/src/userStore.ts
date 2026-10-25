import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { LoginUserVO } from '@smartclass/shared';
import { userApi } from '@smartclass/api';

const CACHED_USER_KEY = 'smartclass_user_info';

function getInitialUser(): LoginUserVO | null {
  try {
    const stored = localStorage.getItem(CACHED_USER_KEY);
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

/**
 * 用户状态 store —— 与后端 Session Cookie 认证对齐：
 * 凭证只存在于服务端会话（Cookie 由 axios withCredentials 自动携带），
 * 本地仅缓存展示信息；真实登录态以 fetchSession() / 401 事件为准。
 */
export const useUserStore = defineStore('user', () => {
  // 状态
  const userInfo = ref<LoginUserVO | null>(getInitialUser());
  const sessionChecked = ref(false);
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 计算属性（基于缓存，仅供 UI 占位；权威判断走后端接口）
  const isLoggedIn = computed(() => !!userInfo.value);
  const isAdmin = computed(() => userInfo.value?.userRole === 'admin');
  const userRole = computed(() => userInfo.value?.userRole || 'student');
  const userName = computed(() => userInfo.value?.userName || userInfo.value?.userAccount || '未登录');
  const userAvatar = computed(() => userInfo.value?.userAvatar || '');

  function cacheUser(user: LoginUserVO | null) {
    userInfo.value = user;
    if (user) {
      localStorage.setItem(CACHED_USER_KEY, JSON.stringify(user));
    } else {
      localStorage.removeItem(CACHED_USER_KEY);
    }
  }

  /**
   * 账号密码登录（验证码开关开启时需先 userApi.getCaptcha() 并携带 captchaUuid/captchaCode）
   */
  async function login(credentials: {
    userAccount: string;
    userPassword: string;
    captchaUuid?: string;
    captchaCode?: string;
  }) {
    loading.value = true;
    error.value = null;
    try {
      const user = await userApi.login(credentials);
      cacheUser(user);
      return { success: true };
    } catch (err: any) {
      error.value = err.message || '登录失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 注册（成功后需用户手动登录；注册本身不建立会话）
   */
  async function register(data: {
    userAccount: string;
    userPassword: string;
    checkPassword: string;
    captchaUuid?: string;
    captchaCode?: string;
  }) {
    loading.value = true;
    error.value = null;
    try {
      await userApi.register(data);
      return { success: true };
    } catch (err: any) {
      error.value = err.message || '注册失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 手机号登录
   */
  async function phoneLogin(credentials: {
    userPhone: string;
    userPassword: string;
    captchaUuid?: string;
    captchaCode?: string;
  }) {
    loading.value = true;
    error.value = null;
    try {
      const user = await userApi.phoneLogin(credentials);
      cacheUser(user);
      return { success: true };
    } catch (err: any) {
      error.value = err.message || '登录失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 登出（后端销毁会话）
   */
  async function logout() {
    try {
      await userApi.logout();
    } catch {
      // 忽略登出接口错误
    } finally {
      cacheUser(null);
    }
  }

  /**
   * 向后端校验并刷新登录态（应用启动/路由守卫调用）
   */
  async function fetchSession() {
    try {
      const user = await userApi.getLoginUser();
      cacheUser(user);
      sessionChecked.value = true;
    } catch {
      cacheUser(null);
      sessionChecked.value = true;
    }
  }

  /**
   * 更新当前用户资料
   */
  async function updateProfile(data: Partial<Pick<LoginUserVO, 'userName' | 'userAvatar' | 'userProfile'>>) {
    loading.value = true;
    error.value = null;
    try {
      await userApi.updateMy(data);
      await fetchSession();
      return { success: true };
    } catch (err: any) {
      error.value = err.message || '更新失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 上传头像
   */
  async function uploadAvatar(file: File) {
    loading.value = true;
    error.value = null;
    try {
      const url = await userApi.uploadAvatar(file);
      await fetchSession();
      return { success: true, url };
    } catch (err: any) {
      error.value = err.message || '上传失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  return {
    // 状态
    userInfo,
    sessionChecked,
    loading,
    error,
    // 计算属性
    isLoggedIn,
    isAdmin,
    userRole,
    userName,
    userAvatar,
    // Actions
    login,
    register,
    phoneLogin,
    logout,
    fetchSession,
    updateProfile,
    uploadAvatar
  };
});
