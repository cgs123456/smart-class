import { Course, mockPopularCourses } from "./mock";

// 模拟获取课程详情

export const fetchCourseDetail = (id: number): Promise<Course> => {
return new Promise((resolve) => {
setTimeout(() => {
const course = mockPopularCourses.find((c) => c.id === id);
// 如果找不到课程，返回第一个作为默认值，确保数组不为空
if (course) {
resolve(course);
} else {
const defaultCourse = mockPopularCourses.length > 0 ? mockPopularCourses[0] : { id: 0, title: '未知课程', brief: '', cover: '', tag: '', tagColor: '', studentsCount: 0 };
resolve(defaultCourse);
}
}, 800);
});
};
