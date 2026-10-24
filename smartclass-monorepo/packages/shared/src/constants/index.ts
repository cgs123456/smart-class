// API 错误码
export const ErrorCode = {
  SUCCESS: 0,
  PARAMS_ERROR: 40000,
  NOT_LOGIN_ERROR: 40100,
  NO_AUTH_ERROR: 40101,
  NOT_FOUND_ERROR: 40400,
  FORBIDDEN_ERROR: 40300,
  SYSTEM_ERROR: 50000,
  OPERATION_ERROR: 50001
} as const;

export type ErrorCode = typeof ErrorCode[keyof typeof ErrorCode];

// 用户角色
export const UserRole = {
  STUDENT: 'student',
  TEACHER: 'teacher',
  ADMIN: 'admin',
  BAN: 'ban'
} as const;

export type UserRole = typeof UserRole[keyof typeof UserRole];

// 用户性别
export const UserGender = {
  MALE: 0,
  FEMALE: 1,
  SECRET: 2
} as const;

export type UserGender = typeof UserGender[keyof typeof UserGender];

// 帖子类型
export const PostType = {
  STUDY: 'study',
  LIFE: 'life',
  SKILL: 'skill',
  QUESTION: 'question'
} as const;

export type PostType = typeof PostType[keyof typeof PostType];

// 课程难度
export const CourseLevel = {
  BEGINNER: 'beginner',
  INTERMEDIATE: 'intermediate',
  ADVANCED: 'advanced'
} as const;

export type CourseLevel = typeof CourseLevel[keyof typeof CourseLevel];

// 圈子类型
export const CircleType = {
  STUDY: 'study',
  LIFE: 'life',
  TECH: 'tech',
  COMMUNITY: 'community'
} as const;

export type CircleType = typeof CircleType[keyof typeof CircleType];

// AI 消息角色
export const MessageRole = {
  USER: 'user',
  AI: 'ai',
  SYSTEM: 'system'
} as const;

export type MessageRole = typeof MessageRole[keyof typeof MessageRole];

// 存储键
export const StorageKey = {
  TOKEN: 'smartclass_token',
  REFRESH_TOKEN: 'smartclass_refresh_token',
  USER_INFO: 'smartclass_user_info',
  TENANT_ID: 'smartclass_tenant_id',
  THEME: 'smartclass_theme',
  LANGUAGE: 'smartclass_language',
  SIDEBAR_COLLAPSED: 'smartclass_sidebar_collapsed'
} as const;

// 正则表达式
export const Regex = {
  EMAIL: /^[^\s@]+@[^\s@]+\.[^\s@]+$/,
  PHONE: /^1[3-9]\d{9}$/,
  PASSWORD: /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)[a-zA-Z\d@$!%*?&]{8,}$/,
  USERNAME: /^[a-zA-Z0-9_]{4,20}$/,
  URL: /^https?:\/\/.+/,
  CHINESE: /[\u4e00-\u9fa5]/,
  EMOJI: /[\u{1F600}-\u{1F64F}]|[\u{1F300}-\u{1F5FF}]|[\u{1F680}-\u{1F6FF}]|[\u{1F1E0}-\u{1F1FF}]/gu
} as const;

// 分页默认值
export const Pagination = {
  DEFAULT_PAGE: 1,
  DEFAULT_PAGE_SIZE: 10,
  MAX_PAGE_SIZE: 100
} as const;

// 文件上传限制
export const FileUpload = {
  MAX_AVATAR_SIZE: 2 * 1024 * 1024, // 2MB
  MAX_VIDEO_SIZE: 100 * 1024 * 1024, // 100MB
  MAX_DOCUMENT_SIZE: 20 * 1024 * 1024, // 20MB
  MAX_MATERIAL_SIZE: 50 * 1024 * 1024, // 50MB
  ALLOWED_VIDEO_TYPES: ['mp4', 'avi', 'mov', 'flv', 'wmv'],
  ALLOWED_DOCUMENT_TYPES: ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt'],
  ALLOWED_IMAGE_TYPES: ['jpg', 'jpeg', 'png', 'gif', 'webp'],
  ALLOWED_MATERIAL_TYPES: ['pdf', 'doc', 'docx', 'ppt', 'pptx', 'xls', 'xlsx', 'txt', 'zip', 'rar', '7z']
} as const;

// HTTP 状态码
export const HttpStatus = {
  OK: 200,
  CREATED: 201,
  NO_CONTENT: 204,
  BAD_REQUEST: 400,
  UNAUTHORIZED: 401,
  FORBIDDEN: 403,
  NOT_FOUND: 404,
  TOO_MANY_REQUESTS: 429,
  INTERNAL_SERVER_ERROR: 500,
  BAD_GATEWAY: 502,
  SERVICE_UNAVAILABLE: 503,
  GATEWAY_TIMEOUT: 504
} as const;

// 本地存储过期时间 (ms)
export const CacheExpiry = {
  SHORT: 5 * 60 * 1000, // 5分钟
  MEDIUM: 30 * 60 * 1000, // 30分钟
  LONG: 24 * 60 * 60 * 1000, // 24小时
  WEEK: 7 * 24 * 60 * 60 * 1000 // 7天
} as const;