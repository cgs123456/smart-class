import { request } from '../client';
import type { Course, CourseChapter, CourseLesson, UserCourseProgress, PageResult, PageRequest } from '@smartclass/shared';

/**
 * 课程相关 API
 */
export const courseApi = {
  /**
   * 课程列表分页查询
   */
  pageQuery: (params: PageRequest & {
    title?: string;
    categoryId?: number;
    level?: string;
    isFree?: number;
    isPublished?: number;
  }) =>
    request.get<PageResult<Course>>('/course/page', { params }),
  
  /**
   * 获取课程详情
   */
  getDetail: (id: number) =>
    request.get<Course>(`/course/get/${id}`),
  
  /**
   * 获取课程章节列表
   */
  getChapters: (courseId: number) =>
    request.get<CourseChapter[]>(`/course/chapter/list/${courseId}`),
  
  /**
   * 获取章节下的课时列表
   */
  getLessons: (chapterId: number) =>
    request.get<CourseLesson[]>(`/course/lesson/list/${chapterId}`),
  
  /**
   * 获取课时详情
   */
  getLessonDetail: (id: number) =>
    request.get<CourseLesson>(`/course/lesson/get/${id}`),
  
  /**
   * 获取用户学习进度
   */
  getProgress: (courseId: number) =>
    request.get<UserCourseProgress>(`/course/progress/${courseId}`),
  
  /**
   * 更新学习进度
   */
  updateProgress: (data: {
    courseId: number;
    chapterId?: number;
    lessonId?: number;
    progressPercent: number;
    status?: 'not_started' | 'in_progress' | 'completed' | 'paused';
    studyTime?: number;
  }) =>
    request.post<UserCourseProgress>('/course/progress/update', data),
  
  /**
   * 开始学习课程
   */
  startLearning: (courseId: number) =>
    request.post<UserCourseProgress>(`/course/learn/start/${courseId}`),
  
  /**
   * 完成课时
   */
  completeLesson: (lessonId: number, studyTime: number) =>
    request.post(`/course/learn/complete/${lessonId}`, { studyTime }),
  
  /**
   * 收藏/取消收藏课程
   */
  toggleFavour: (courseId: number) =>
    request.post<boolean>(`/course/favour/toggle/${courseId}`),
  
  /**
   * 获取收藏的课程列表
   */
  getFavourList: (params: PageRequest) =>
    request.get<PageResult<Course>>('/course/favour/list', { params }),
  
  /**
   * 点赞/取消点赞课程
   */
  toggleLike: (courseId: number) =>
    request.post<boolean>(`/course/like/toggle/${courseId}`),
  
  /**
   * 获取推荐课程
   */
  getRecommend: (limit = 10) =>
    request.get<Course[]>(`/course/recommend?limit=${limit}`),
  
  /**
   * 搜索课程
   */
  search: (keyword: string, params: PageRequest) =>
    request.get<PageResult<Course>>(`/course/search`, { 
      params: { ...params, keyword } 
    }),
  
  /**
   * 获取课程分类
   */
  getCategories: () =>
    request.get<Array<{ id: number; name: string; code: string }>>('/course/category/list'),
  
  /**
   * 获取热门标签
   */
  getHotTags: (limit = 20) =>
    request.get<string[]>(`/course/tags/hot?limit=${limit}`)
};

/**
 * 课程学习相关
 */
export const courseLearn = {
  /**
   * 获取视频播放地址
   */
  getVideoUrl: (lessonId: number, quality: 'auto' | '720p' | '1080p' = 'auto') =>
    request.get<{ url: string; token?: string }>(`/course/video/url/${lessonId}`, { 
      params: { quality } 
    }),
  
  /**
   * 记录视频观看进度
   */
  recordVideoProgress: (lessonId: number, currentTime: number, duration: number) =>
    request.post('/course/video/progress', { lessonId, currentTime, duration }),
  
  /**
   * 获取课程笔记
   */
  getNotes: (courseId: number) =>
    request.get<Array<{ id: number; content: string; timestamp: number; lessonId: number }>>(`/course/note/list/${courseId}`),
  
  /**
   * 添加笔记
   */
  addNote: (data: { courseId: number; lessonId: number; content: string; timestamp: number }) =>
    request.post('/course/note/add', data),
  
  /**
   * 更新笔记
   */
  updateNote: (id: number, content: string) =>
    request.put(`/course/note/update/${id}`, { content }),
  
  /**
   * 删除笔记
   */
  deleteNote: (id: number) =>
    request.delete(`/course/note/delete/${id}`)
};