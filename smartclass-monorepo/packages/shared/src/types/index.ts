// 枚举别名复用 constants 中的定义（与后端模型对齐）
import type { CircleType, CourseLevel, MessageRole, UserGender, UserRole } from '../constants';

// 通用 API 响应
export interface ApiResponse<T = any> {
  code: number;
  message: string;
  data: T | null;
  timestamp?: number;
  trace_id?: string;
}

// 分页请求
export interface PageRequest {
  current?: number;
  pageSize?: number;
  sortField?: string;
  sortOrder?: 'asc' | 'desc';
}

// 分页响应
export interface PageResult<T = any> {
  current: number;
  pageSize: number;
  total: number;
  pages: number;
  records: T[];
}

// 基础实体
export interface BaseEntity {
  id: number | string;
  createTime?: string | number;
  updateTime?: string | number;
  isDelete?: number;
}

// 用户相关类型
export interface User extends BaseEntity {
  userAccount: string;
  userName?: string;
  userAvatar?: string;
  userProfile?: string;
  userRole: UserRole;
  userEmail?: string;
  userPhone?: string;
  userGender?: UserGender;
  province?: string;
  city?: string;
  district?: string;
  wechatId?: string;
  unionId?: string;
  mpOpenId?: string;
  birthday?: string | number;
}

export interface LoginUserVO {
  id: number;
  userAccount: string;
  userName?: string;
  userAvatar?: string;
  userProfile?: string;
  userRole: UserRole;
}

export interface UserVO extends LoginUserVO {
  birthdayYear?: number;
  createTime?: string;
}

// 课程相关类型
export interface Course extends BaseEntity {
  title: string;
  coverImgUrl?: string;
  summary?: string;
  providerId?: number;
  categoryId?: number;
  level: CourseLevel;
  duration?: string;
  studyCount: number;
  likeCount: number;
  isPublished: number;
  isFree: number;
  price: number;
  salePrice?: number;
  tags?: string;
  description?: string;
  teachingPlan?: string;
}

export interface CourseChapter extends BaseEntity {
  courseId: number;
  chapterName: string;
  chapterOrder: number;
  isPublished: number;
}

export interface CourseLesson extends BaseEntity {
  chapterId: number;
  lessonName: string;
  lessonOrder: number;
  videoUrl?: string;
  videoCoverUrl?: string;
  duration: number;
  isFree: number;
  isPublished: number;
}

export interface UserCourseProgress extends BaseEntity {
  userId: number;
  courseId: number;
  chapterId?: number;
  lessonId?: number;
  progressPercent: number;
  status: 'not_started' | 'in_progress' | 'completed' | 'paused';
  lastLessonTime?: string;
  totalStudyTime: number;
  completedLessons: number;
  totalLessons: number;
}

// 文章相关类型
export interface Article extends BaseEntity {
  title: string;
  coverImgUrl?: string;
  content: string;
  authorId: number;
  categoryId?: number;
  tags?: string;
  viewCount: number;
  likeCount: number;
  collectCount: number;
  commentCount: number;
  isPublished: number;
  isTop: number;
  isRecommend: number;
}

export interface ArticleCategory extends BaseEntity {
  name: string;
  code: string;
  description?: string;
  sort: number;
  isSystem: number;
  isEnabled: number;
}

export interface ArticleTag extends BaseEntity {
  name: string;
  code: string;
  category?: string;
  sort: number;
  isSystem: number;
  isEnabled: number;
}

// 圈子相关类型
export interface Circle extends BaseEntity {
  title?: string;
  coverImgUrl?: string;
  desc?: string;
  type?: CircleType;
  creatorId: number;
  memberCount: number;
  postCount: number;
  isPublic: number;
  isVerified: number;
}

export interface CirclePost extends BaseEntity {
  circleId: number;
  title?: string;
  content?: string;
  authorId: number;
  thumbNum: number;
  favourNum: number;
  commentNum: number;
  isTop: number;
  isRecommend: number;
}

// AI 相关类型
export interface AiAvatar extends BaseEntity {
  name: string;
  description?: string;
  avatarImgUrl?: string;
  category?: string;
  modelType?: string;
  personality?: string;
  abilities?: string;
  tags?: string;
  rating: number;
  usageCount: number;
  baseUrl?: string;
  avatarAuth?: string;
  status: number;
  isPublic: number;
  creatorId?: number;
  adminId?: number;
}

export interface ChatMessage extends BaseEntity {
  userId: number;
  aiAvatarId: number;
  sessionId?: string;
  role: MessageRole;
  content: string;
  messageId?: string;
  tokens: number;
}

// 学习统计类型
export interface UserLearningStats extends BaseEntity {
  userId: number;
  level: number;
  experience: number;
  nextLevelExp: number;
  learningDays: number;
  continuousCheckIn: number;
  totalCheckIn: number;
  totalPoints: number;
  totalBadges: number;
  lastCheckInTime?: string;
}

// 成就类型
export interface Achievement extends BaseEntity {
  name: string;
  description: string;
  iconUrl: string;
  badgeUrl: string;
  bannerUrl?: string;
  category: string;
  level: number;
  points: number;
  achievementCondition: string;
  conditionType: string;
  conditionValue: number;
  isHidden: number;
  isSecret: number;
  rewardType?: string;
  rewardValue?: string;
  sort: number;
  adminId: number;
}

export interface UserAchievement extends BaseEntity {
  userId: number;
  achievementId: number;
  progress: number;
  progressMax: number;
  progressPercent: number;
  isCompleted: number;
  completedTime?: string;
  isRewarded: number;
  rewardTime?: string;
}

// 租户类型
export interface TenantInfo {
  id: string;
  name: string;
  tier: 'SHARED' | 'SCHEMA' | 'DATABASE' | 'DEDICATED';
  status: 'ACTIVE' | 'SUSPENDED' | 'EXPIRED' | 'TRIAL';
  enabledFeatures: string[];
  adminId?: string;
}

// 表单验证规则
export interface ValidationRule {
  required?: boolean;
  min?: number;
  max?: number;
  pattern?: RegExp;
  message?: string;
  validator?: (value: any) => boolean | Promise<boolean>;
}

// 组件通用 Props
export interface ComponentSize {
  size?: 'large' | 'default' | 'small';
}

export interface ComponentDisabled {
  disabled?: boolean;
}

export interface ComponentLoading {
  loading?: boolean;
}