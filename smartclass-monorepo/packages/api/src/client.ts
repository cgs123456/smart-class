import axios, { AxiosError, AxiosInstance, AxiosRequestConfig, AxiosResponse, InternalAxiosRequestConfig } from 'axios';
import { ApiError, normalizeApiError } from './errors';
import type { ApiResponse } from '@smartclass/shared';

const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || '/api';

/**
 * 会话过期事件：后端为 Session Cookie 认证，401 时库本身不感知路由，
 * 由宿主应用订阅此事件决定跳转/弹窗。
 */
export const UNAUTHORIZED_EVENT = 'smartclass:unauthorized';

/**
 * 本地缓存的登录用户 key（仅缓存展示信息，非凭证；凭证是 Http 会话 Cookie）
 */
export const CACHED_USER_KEY = 'smartclass_user_info';

/**
 * 业务错误：HTTP 200 但 BaseResponse.code != 0
 */
export class ApiBusinessError extends ApiError {
  constructor(public readonly bizCode: number, message: string) {
    super(String(bizCode), message);
    this.name = 'ApiBusinessError';
  }
}

/**
 * 创建 Axios 实例
 */
export const createApiClient = (baseURL = API_BASE_URL): AxiosInstance => {
  const instance = axios.create({
    baseURL,
    timeout: 30000,
    headers: { 'Content-Type': 'application/json' },
    // 认证走 Cookie（后端 Session），不使用本地令牌
    withCredentials: true
  });

  // 请求拦截：Trace ID（后端不依赖，仅便于日志关联）
  instance.interceptors.request.use((config: InternalAxiosRequestConfig) => {
    config.headers['X-Trace-Id'] = config.headers['X-Trace-Id'] || crypto.randomUUID().replace(/-/g, '');
    (config as InternalAxiosRequestConfig & { metadata?: { startTime: number } }).metadata = { startTime: Date.now() };
    return config;
  });

  // 响应拦截：错误统一归一化；401 广播会话过期事件
  instance.interceptors.response.use(
    undefined,
    async (error: AxiosError<ApiErrorResponse>) => {
      // 401：会话过期/未登录——清除缓存用户并广播，不做页面跳转（库不感知路由）
      if (error.response?.status === 401) {
        localStorage.removeItem(CACHED_USER_KEY);
        window.dispatchEvent(new CustomEvent(UNAUTHORIZED_EVENT));
      }
      return Promise.reject(normalizeApiError(error));
    }
  );

  return instance;
};

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
 * 解包 BaseResponse：code != 0 视为业务错误抛出，成功返回 data 载荷。
 * 端点方法由此获得与类型声明一致的返回值（T 而非 ApiResponse<T>）。
 */
async function unwrap<T>(p: Promise<AxiosResponse<ApiResponse<T>>>): Promise<T> {
  const body = (await p).data;
  if (!body || body.code !== 0) {
    throw new ApiBusinessError(body?.code ?? -1, body?.message || '接口返回异常');
  }
  return body.data as T;
}

/**
 * 类型安全的请求方法封装（返回业务数据本体，业务错误以异常抛出）
 */
export const request = {
  get: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.get<ApiResponse<T>>(url, config)),

  post: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.post<ApiResponse<T>>(url, data, config)),

  put: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.put<ApiResponse<T>>(url, data, config)),

  patch: <T>(url: string, data?: any, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.patch<ApiResponse<T>>(url, data, config)),

  delete: <T>(url: string, config?: AxiosRequestConfig) =>
    unwrap<T>(apiClient.delete<ApiResponse<T>>(url, config)),

  // 文件上传
  upload: <T>(url: string, file: File, onProgress?: (progress: number) => void) => {
    const formData = new FormData();
    formData.append('file', file);
    return unwrap<T>(apiClient.post<ApiResponse<T>>(url, formData, {
      headers: { 'Content-Type': 'multipart/form-data' },
      onUploadProgress: e => onProgress?.(Math.round((e.loaded * 100) / (e.total || 1)))
    }));
  }
};
