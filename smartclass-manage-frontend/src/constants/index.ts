/**
 * 本地后端地址
 */
export const BACKEND_HOST_LOCAL = process.env.BACKEND_HOST_LOCAL || "http://localhost:12345/";

/**
 * 线上后端地址（必须使用 HTTPS，避免会话 Cookie / 凭证在传输中被窃听）
 */
export const BACKEND_HOST_PROD = "https://backend.smartclass.cgs.cn";
