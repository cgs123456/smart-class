import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, InternalAxiosRequestConfig } from 'axios';
import { normalizeApiError } from './errors';
import type { ApiResponse } from '@smartclass/shared';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * 创建 Axios 实例
 */
export const createApiClient = (baseURL = API_BASE_URL): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' },
    withCredentials: true
  });

  // 请求拦截：注入 Token、租户 ID、Trace ID
  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    // 从 localStorage 获取 token
    const token = localStorage.getItem('smartclass_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    
    // 租户 ID
    const tenantId = localStorage.getItem('smartclass_tenant_id') || 'default';
    if (tenantId) {
      config.headers['X-Tenant-Id'] = tenantId;
    }
    
    // Trace ID
    config.headers['X-Trace-Id'] = config.headers['X-Trace-Id'] || crypto.randomUUID().replace(/-/g, '');
    (config as InternalAxiosRequestConfig & { metadata?: { startTime: number } }).metadata = { startTime: Date.now() };
    
    return config;
  });

  // 响应拦截：统一解包、错误码映射、Token 刷新
  instance.interceptors.response.use(
    response => response.data, // 直接返回 ApiResponse.data
    async (error: AxiosError<ApiErrorResponse>) => {
      const originalRequest = error.config as InternalAxiosRequestConfig & { _retry?: boolean };
      
      // 401: 尝试刷新 Token
      if (error.response?.status === 401 && !originalRequest._retry) {
        originalRequest._retry = true;
        try {
          await refreshToken();
          return instance(originalRequest);
        } catch {
          // 刷新失败，清除登录态
          localStorage.removeItem('smartclass_token');
          localStorage.removeItem('smartclass_refresh_token');
          localStorage.removeItem('smartclass_user_info');
          window.location.href = '/login';
        }
      }
      
      // 业务错误码映射
      const bizCode = error.response?.data?.code;
      const message = error.response?.data?.message || error.message;
      
      // 静默错误码不弹 Toast（对齐后端 ErrorCode：40100 未登录 / 40400 数据不存在）
      const silentCodes = [40100, 40400];
      if (bizCode !== undefined && silentCodes.includes(bizCode)) {
        // 这里可以集成 Toast 组件，暂时用 console.warn
        console.warn('[API Error]', message);
      }
      
      return Promise.reject(normalizeApiError(error));
    }
  );

  return instance;
};

/**
 * 刷新 Token
 */
async function refreshToken(): Promise<void> {
  const refreshToken = localStorage.getItem('smartclass_refresh_token');
  if (!refreshToken) throw new Error('No refresh token');
  
  const response = await axios.post<ApiResponse<{ token: string; refreshToken: string }>>(
    `${API_BASE_URL}/auth/refresh`,
    { refreshToken },
    { headers: { 'Content-Type': 'application/json' } }
  );
  
  if (response.data.code === 0 && response.data.data) {
    localStorage.setItem('smartclass_token', response.data.data.token);
    localStorage.setItem('smartclass_refresh_token', response.data.data.refreshToken);
  } else {
    throw new Error('Token refresh failed');
  }
}

/**
 * API 错误响应类型
 */
export interface ApiErrorResponse {
  code: number;
  message: string;
  data?: any;
}

/**
 * 单例导出
 */
export const apiClient = createApiClient();

/**
 * 类型安全的请求方法封装
 */
export const request = {
  get: <T>(url: string, config?: AxiosRequestConfig) => 
    apiClient.get<ApiResponse<T>>(url, config).then(r => r.data),
  
  post: <T>(url: string, data?: any, config?: AxiosRequestConfig) => 
    apiClient.post<ApiResponse<T>>(url, data, config).then(r => r.data),
  
  put: <T>(url: string, data?: any, config?: AxiosRequestConfig) => 
    apiClient.put<ApiResponse<T>>(url, data, config).then(r => r.data),
  
  patch: <T>(url: string, data?: any, config?: AxiosRequestConfig) => 
    apiClient.patch<ApiResponse<T>>(url, data, config).then(r => r.data),
  
  delete: <T>(url: string, config?: AxiosRequestConfig) => 
    apiClient.delete<ApiResponse<T>>(url, config).then(r => r.data),
  
  // 文件上传
  upload: <T>(url: string, file: File, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    return apiClient.post<ApiResponse<T>>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / (e.total || 1)))
    }).then(r => r.data);
  }
};