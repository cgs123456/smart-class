import { defineStore } from 'pinia';
import { ref, computed } from 'vue';
import type { User } from '@smartclass/shared';
import { userApi } from '@smartclass/api';

function getInitialUser(): User | null {
  try {
    const stored = localStorage.getItem('smartclass_user_info');
    return stored ? JSON.parse(stored) : null;
  } catch {
    return null;
  }
}

export const useUserStore = defineStore('user', () => {
  // 状态
  const token = ref<string | null>(localStorage.getItem('smartclass_token'));
  const refreshToken = ref<string | null>(localStorage.getItem('smartclass_refresh_token'));
  const userInfo = ref<User | null>(getInitialUser());
  const tenantId = ref<string>(localStorage.getItem('smartclass_tenant_id') || 'default');
  const loading = ref(false);
  const error = ref<string | null>(null);

  // 计算属性
  const isLoggedIn = computed(() => !!token.value && !!userInfo.value);
  const isAdmin = computed(() => userInfo.value?.userRole === 'admin');
  const userRole = computed(() => userInfo.value?.userRole || 'student');
  const userName = computed(() => userInfo.value?.userName || userInfo.value?.userAccount || '未登录');
  const userAvatar = computed(() => userInfo.value?.userAvatar || '');

  // Actions
  /**
   * 设置认证信息
   */
  function setAuth(newToken: string, newRefreshToken: string, user: User) {
    token.value = newToken;
    refreshToken.value = newRefreshToken;
    userInfo.value = user;
    localStorage.setItem('smartclass_token', newToken);
    localStorage.setItem('smartclass_refresh_token', newRefreshToken);
    localStorage.setItem('smartclass_user_info', JSON.stringify(user));
    error.value = null;
  }

  /**
   * 清除认证信息
   */
  function clearAuth() {
    token.value = null;
    refreshToken.value = null;
    userInfo.value = null;
    localStorage.removeItem('smartclass_token');
    localStorage.removeItem('smartclass_refresh_token');
    localStorage.removeItem('smartclass_user_info');
  }

  /**
   * 登录
   */
  async function login(credentials: { userAccount: string; userPassword: string }) {
    loading.value = true;
    error.value = null;
    try {
      const response = await userApi.login(credentials);
      if (response.code === 0 && response.data) {
        setAuth(response.data.token, response.data.refreshToken, response.data.user);
        return { success: true };
      }
      error.value = response.message || '登录失败';
      return { success: false, message: error.value };
    } catch (err: any) {
      error.value = err.message || '登录失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 注册
   */
  async function register(data: { userAccount: string; userPassword: string; checkPassword: string }) {
    loading.value = true;
    error.value = null;
    try {
      const response = await userApi.register(data);
      if (response.code === 0 && response.data) {
        setAuth(response.data.token, response.data.refreshToken, response.data.user);
        return { success: true };
      }
      error.value = response.message || '注册失败';
      return { success: false, message: error.value };
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
  async function phoneLogin(phone: string, code: string) {
    loading.value = true;
    error.value = null;
    try {
      const response = await userApi.phoneLogin({ phone, code });
      if (response.code === 0 && response.data) {
        setAuth(response.data.token, response.data.refreshToken, response.data.user);
        return { success: true };
      }
      error.value = response.message || '登录失败';
      return { success: false, message: error.value };
    } catch (err: any) {
      error.value = err.message || '登录失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 登出
   */
  async function logout() {
    try {
      await userApi.logout();
    } catch {
      // 忽略登出接口错误
    } finally {
      clearAuth();
    }
  }

  /**
   * 刷新用户信息
   */
  async function refreshUserInfo() {
    if (!isLoggedIn.value) return;
    try {
      const response = await userApi.getLoginUser();
      if (response.code === 0 && response.data) {
        userInfo.value = response.data;
        localStorage.setItem('smartclass_user_info', JSON.stringify(response.data));
      }
    } catch (err) {
      console.error('刷新用户信息失败:', err);
    }
  }

  /**
   * 更新用户资料
   */
  async function updateProfile(data: Partial<User>) {
    if (!userInfo.value) return { success: false, message: '未登录' };
    loading.value = true;
    error.value = null;
    try {
      const payload: Partial<User> & { id: number } = { ...data, id: userInfo.value.id as number };
      const response = await userApi.updateById(payload);
      if (response) {
        await refreshUserInfo();
        return { success: true };
      }
      error.value = '更新失败';
      return { success: false, message: '更新失败' };
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
      const response = await userApi.uploadAvatar(file);
      if (response.code === 0 && response.data) {
        if (userInfo.value) {
          userInfo.value.userAvatar = response.data;
          localStorage.setItem('smartclass_user_info', JSON.stringify(userInfo.value));
        }
        return { success: true, url: response.data };
      }
      error.value = response.message || '上传失败';
      return { success: false, message: error.value };
    } catch (err: any) {
      error.value = err.message || '上传失败';
      return { success: false, message: error.value };
    } finally {
      loading.value = false;
    }
  }

  /**
   * 设置租户 ID
   */
  function setTenantId(newTenantId: string) {
    tenantId.value = newTenantId;
    localStorage.setItem('smartclass_tenant_id', newTenantId);
  }

  return {
    // 状态
    token,
    refreshToken,
    userInfo,
    tenantId,
    loading,
    error,
    // 计算属性
    isLoggedIn,
    isAdmin,
    userRole,
    userName,
    userAvatar,
    // Actions
    setAuth,
    clearAuth,
    login,
    register,
    phoneLogin,
    logout,
    refreshUserInfo,
    updateProfile,
    uploadAvatar,
    setTenantId
  };
});