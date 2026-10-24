import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import { useUserStore } from '@smartclass/stores';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/home'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { guest: true }
  },
  {
    path: '/home',
    name: 'Home',
    component: () => import('@/views/Home.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/course',
    name: 'CourseList',
    component: () => import('@/views/course/List.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/course/:id',
    name: 'CourseDetail',
    component: () => import('@/views/course/Detail.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/course/:id/learn',
    name: 'CourseLearn',
    component: () => import('@/views/course/Learn.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/intelligence',
    name: 'Intelligence',
    component: () => import('@/views/intelligence/Index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/intelligence/chat/:avatarId',
    name: 'AiChat',
    component: () => import('@/views/intelligence/Chat.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/circle',
    name: 'Circle',
    component: () => import('@/views/circle/Index.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/profile',
    name: 'Profile',
    component: () => import('@/views/Profile.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/settings',
    name: 'Settings',
    component: () => import('@/views/Settings.vue'),
    meta: { requiresAuth: true }
  },
  {
    path: '/:pathMatch(.*)*',
    name: 'NotFound',
    component: () => import('@/views/NotFound.vue')
  }
];

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) return savedPosition;
    return { top: 0 };
  }
});

// 全局前置守卫
router.beforeEach(async (to, _from, next) => {
  const userStore = useUserStore();
  
  // 初始化用户状态
  if (!userStore.isLoggedIn && userStore.token) {
    await userStore.refreshUserInfo();
  }
  
  // 需要认证的页面
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next({ name: 'Login', query: { redirect: to.fullPath } });
    return;
  }
  
  // 游客页面（已登录用户重定向）
  if (to.meta.guest && userStore.isLoggedIn) {
    next({ name: 'Home' });
    return;
  }
  
  next();
});

export default router;