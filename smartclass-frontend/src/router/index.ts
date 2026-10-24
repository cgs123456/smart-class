import {
  createRouter,
  createWebHistory,
  RouteRecordRaw,
  NavigationGuardNext,
  RouteLocationNormalized,
} from 'vue-router';

declare module 'vue-router' {
  interface RouteMeta {
    requiresAuth: boolean;
  }
}

const routes: Array<RouteRecordRaw> = [
  // 认证相关路由
  {
    path: '/login',
    name: 'login',
    component: () => import('../views/user/Login.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  {
    path: '/register',
    name: 'register',
    component: () => import('../views/user/Register.vue'),
    meta: {
      requiresAuth: false,
    },
  },
  
  // 主页相关路由
  {
    path: '/',
    name: 'home',
    component: () => import('../views/home/Home.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/search',
    name: 'search',
    component: () => import('../views/common').then(m => m.SearchPage),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/notices',
    name: 'notices',
    component: () => import('../views/home/NoticeList.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 聊天相关路由
  {
    path: '/chat',
    name: 'chat',
    component: () => import('../views/chat').then(m => m.ChatContainer),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/chat/detail',
    name: 'chat-detail',
    component: () => import('../views/chat').then(m => m.AIChatDetail),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/chat/detail/:assistantId',
    name: 'chat-detail-with-assistant',
    component: () => import('../views/chat').then(m => m.AIChatDetail),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/userchat/:userId',
    name: 'user-chat-detail',
    component: () => import('../views/chat').then(m => m.UserChatDetail),
    meta: {
      requiresAuth: true,
    },
    props: true,
  },
  {
    path: '/chat-history',
    redirect: '/chat',
  },
  
  // 好友相关路由
  {
    path: '/friends/requests',
    name: 'friend-requests',
    component: () => import('../views/chat/friends/FriendRequests.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/friends/add',
    name: 'add-friend',
    component: () => import('../views/chat/friends/AddFriend.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/qr/scan',
    name: 'qr-scan',
    component: () => import('../views/chat/QRCodeScanPage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/qr/display',
    name: 'qr-display',
    component: () => import('../views/chat/QRCodeDisplayPage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 课程相关路由
  {
    path: '/courses',
    name: 'courses',
    component: () => import('../views/course/Courses.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/courses/schedule',
    name: 'course-schedule',
    component: () => import('../views/course/CourseSchedule.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/courses/popular',
    name: 'popular-courses',
    component: () => import('../views/course/PopularCourses.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/courses/task-plans',
    name: 'task-plans',
    component: () => import('../views/course/TaskPlans.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/courses/study/:id',
    name: 'course-study',
    component: () => import('../views/course/CourseStudy.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/courses/history',
    name: 'course-history',
    component: () => import('../views/course/HistoryPage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 个人中心相关路由
  {
    path: '/profile',
    name: 'profile',
    component: () => import('../views/my-profile/Profile.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/achievements',
    name: 'achievements',
    component: () => import('../views/my-profile/achievements/AchievementsPage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings',
    name: 'settings',
    component: () => import('../views/my-profile/settings/Settings.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/info',
    name: 'settings-profile',
    component: () => import('../views/my-profile/settings/SettingsProfile.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/about',
    name: 'settings-about',
    component: () => import('../views/my-profile/settings/SettingsAbout.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/terms',
    name: 'settings-terms',
    component: () => import('../views/my-profile/settings/SettingsTerms.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/privacy',
    name: 'settings-privacy',
    component: () => import('../views/my-profile/settings/SettingsPrivacy.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/feedback',
    name: 'settings-feedback',
    component: () => import('../views/my-profile/settings/SettingsFeedback.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/profile/settings/avatar-cropper',
    name: 'avatar-cropper',
    component: () => import('../views/my-profile/settings/AvatarCropper.vue'),
    meta: {
      requiresAuth: true,
      title: '裁剪头像',
    },
  },
  
  // 词汇相关路由
  {
    path: '/vocabulary',
    name: 'vocabulary',
    component: () => import('../views/home/vocabulary/VocabularyList.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/vocabulary/collected',
    name: 'collected-words',
    component: () => import('../views/home/vocabulary/CollectedWords.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 文章相关路由
  {
    path: '/articles',
    name: 'articles',
    component: () => import('../views/home/articles/ArticlesList.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/daily/article/:id',
    name: 'article-detail',
    component: () => import('../views/home/articles/ArticleDetailPage.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 圈子相关路由
  {
    path: '/circle',
    name: 'circle',
    component: () => import('../views/circle/Circle.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/circle/post/:id',
    name: 'post-detail',
    component: () => import('../views/circle/PostDetail.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  {
    path: '/circle/post/create',
    name: 'post-create',
    component: () => import('../views/circle/PostCreate.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 用户相关路由
  {
    path: '/users/:id',
    name: 'user-profile',
    component: () => import('../views/user/UserProfile.vue'),
    meta: {
      requiresAuth: true,
    },
  },
  
  // 404页面 - 放在最后匹配所有未找到的路由
  {
    path: '/:pathMatch(.*)*',
    name: 'not-found',
    component: () => import('../views/common/NotFound.vue'),
    meta: {
      requiresAuth: false,
      title: '页面未找到',
    },
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior(_to, _from, savedPosition) {
    if (savedPosition) {
      return savedPosition;
    } else {
      // 在路由切换时立即滚动到顶部，没有平滑滚动效果
      return { top: 0, behavior: 'auto' };
    }
  },
});

// 路由守卫
router.beforeEach(
  async (
    to: RouteLocationNormalized,
    from: RouteLocationNormalized,
    next: NavigationGuardNext,
  ) => {
    const { useUserStore } = await import('../stores/userStore');
    const userStore = useUserStore();

    // 优先检查 store 中的登录状态（响应式），其次检查 localStorage
    // 注意：当前依赖"头像是否为默认值"作为登录态信号之一，逻辑相对脆弱。
    // 后续可优化为独立的 isAuthenticated 判断逻辑，例如：
    //   const isLoggedIn = localStorage.getItem('userInfo') !== null;
    // 现暂保留头像判断以避免破坏现有功能。
    const isLoggedIn = userStore.getUserAvatar() !== userStore.DEFAULT_USER_AVATAR
      || localStorage.getItem('userInfo') !== null;

    if (to.meta.requiresAuth && !isLoggedIn) {
      next({
        path: '/login',
        query: { redirect: to.fullPath },
      });
    } else if ((to.path === '/login' || to.path === '/register') && isLoggedIn) {
      next('/');
    } else {
      next();
    }
  },
);

export default router;
