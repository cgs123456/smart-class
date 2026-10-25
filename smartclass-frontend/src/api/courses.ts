import { CourseControllerService } from '../services/services/CourseControllerService';
import { CourseCategoryControllerService } from '../services/services/CourseCategoryControllerService';
import type { CourseVO } from '../services/models/CourseVO';
import type { Course } from './mock';

// 后端 difficulty：1/2/3 → 初级/中级/高级，其余/缺失归为综合
const difficultyToLevel = (difficulty?: number): string =>
  difficulty === 1 ? '初级' : difficulty === 2 ? '中级' : difficulty === 3 ? '高级' : '综合';

const levelTagColor: Record<string, string> = {
  初级: '#4caf50',
  中级: '#1989fa',
  高级: '#ff976a',
  综合: '#969799',
};

/**
 * 将后端 CourseVO 映射为前端展示用 Course 结构（沿用 mock 时代的字段形态，
 * 避免一次性改动所有消费组件）。subject 取首个 tag，无 tag 归为「综合」。
 */
export function mapCourseVO(vo: CourseVO): Course {
  const level = difficultyToLevel(vo.difficulty);
  const firstTag =
    (vo.tags ?? '')
      .split(/[,，]/)
      .map((s) => s.trim())
      .filter(Boolean)[0] ?? '';
  return {
    id: vo.id ?? 0,
    title: vo.title ?? '未命名课程',
    brief: vo.subtitle || vo.description || '',
    cover: vo.coverImage || '',
    tag: firstTag || level,
    tagColor: levelTagColor[level] ?? '',
    level,
    duration: vo.totalDuration ?? 0,
    studentsCount: vo.studyCount ?? vo.buyCount ?? 0,
    subject: firstTag || '综合',
  };
}

// 生成客户端返回的是完整 BaseResponse（code/data/message），此处统一解包
function unwrap<T>(res: { code?: number; data?: T; message?: string }): T {
  if (res.code !== 0) {
    throw new Error(res.message || '接口返回异常');
  }
  return res.data as T;
}

/** 推荐课程列表（真实接口：GET /api/course/recommend） */
export async function fetchRecommendCourses(limit = 6): Promise<Course[]> {
  const res = await CourseControllerService.getRecommendCoursesUsingGet(undefined, undefined, limit);
  return unwrap(res).map(mapCourseVO);
}

/** 课程详情（真实接口：GET /api/course/get/vo）；课程不存在时返回 null */
export async function fetchCourseDetailById(id: number): Promise<Course | null> {
  const res = await CourseControllerService.getCourseVoByIdUsingGet(id);
  const vo = unwrap(res);
  return vo ? mapCourseVO(vo) : null;
}

/** 顶级课程分类（真实接口：GET /api/course/category/top） */
export async function fetchTopCategories(): Promise<{ id: number; name: string; icon: string }[]> {
  const res = await CourseCategoryControllerService.getTopCategoriesUsingGet();
  return unwrap(res).map((c) => ({
    id: c.id ?? 0,
    name: c.name ?? '未分类',
    icon: c.icon || 'bookmark-o',
  }));
}
