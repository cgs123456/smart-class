import axios, { AxiosError } from 'axios';
import type { ApiErrorResponse } from './client';

/**
 * API 错误类
 */
export class ApiError extends Error {
  constructor(
    public readonly code: string,
    message: string,
    public readonly rawData?: any
  ) {
    super(message);
    this.name = 'ApiError';
    
    // 保持原型链
    Object.setPrototypeOf(this, ApiError.prototype);
  }
  
  /**
   * 判断是否为特定错误码
   */
  is(code: string): boolean {
    return this.code === code;
  }
  
  /**
   * 判断是否为认证错误
   */
  isAuthError(): boolean {
    return ['NOT_LOGIN_ERROR', 'NO_AUTH_ERROR', 'TOKEN_EXPIRED'].includes(this.code);
  }
  
  /**
   * 判断是否为权限错误
   */
  isPermissionError(): boolean {
    return ['FORBIDDEN_ERROR', 'NO_AUTH_ERROR'].includes(this.code);
  }
  
  /**
   * 判断是否为未找到错误
   */
  isNotFound(): boolean {
    return this.code === 'NOT_FOUND_ERROR' || this.code === 'RESOURCE_NOT_FOUND';
  }
  
  /**
   * 判断是否为服务器错误
   */
  isServerError(): boolean {
    return this.code === 'SYSTEM_ERROR' || this.code === 'OPERATION_ERROR';
  }
  
  /**
   * 判断是否为网络错误
   */
  isNetworkError(): boolean {
    return this.code === 'NETWORK_ERROR' || this.code === 'TIMEOUT';
  }
}

/**
 * 标准化 API 错误
 */
export const normalizeApiError = (error: any): ApiError => {
  if (error instanceof ApiError) return error;
  
  if (axios.isAxiosError(error)) {
    const axiosError = error as AxiosError<ApiErrorResponse>;
    return new ApiError(
      axiosError.response?.data?.code?.toString() || 'NETWORK_ERROR',
      axiosError.response?.data?.message || axiosError.message,
      axiosError.response?.data
    );
  }
  
  if (error instanceof Error) {
    return new ApiError('UNKNOWN_ERROR', error.message);
  }
  
  return new ApiError('UNKNOWN_ERROR', String(error));
};

/**
 * 错误码常量
 */
export const ErrorCodes = {
  SUCCESS: '0',
  PARAMS_ERROR: '40000',
  NOT_LOGIN_ERROR: '40100',
  NO_AUTH_ERROR: '40101',
  NOT_FOUND_ERROR: '40400',
  FORBIDDEN_ERROR: '40300',
  SYSTEM_ERROR: '50000',
  OPERATION_ERROR: '50001',
  NETWORK_ERROR: 'NETWORK_ERROR',
  TIMEOUT: 'TIMEOUT',
  UNKNOWN_ERROR: 'UNKNOWN_ERROR'
} as const;

/**
 * 静默错误码 (不弹 Toast)
 */
export const SILENT_ERROR_CODES = new Set([
  'USER_NOT_FOUND',
  'RESOURCE_NOT_FOUND',
  'NOT_LOGIN_ERROR',
  '40400'
]);