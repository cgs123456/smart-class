import { defineStore } from 'pinia';
import { ref, computed } from 'vue';

function getInitialTheme(): 'light' | 'dark' {
  const stored = localStorage.getItem('smartclass_theme');
  if (stored === 'light' || stored === 'dark') return stored;
  return window.matchMedia('(prefers-color-scheme: dark)').matches ? 'dark' : 'light';
}

export const useAppStore = defineStore('app', () => {
  // 状态
  const sidebarCollapsed = ref(false);
  const theme = ref<'light' | 'dark'>(getInitialTheme());
  const language = ref<'zh-CN' | 'en-US'>(
    (localStorage.getItem('smartclass_language') as 'zh-CN' | 'en-US' | null) || 'zh-CN'
  );
  const globalLoading = ref(false);
  const notifications = ref<Array<{
    id: string;
    type: 'success' | 'error' | 'warning' | 'info';
    title: string;
    message?: string;
    duration?: number;
  }>>([]);
  const deviceType = ref<'mobile' | 'tablet' | 'desktop'>('desktop');
  const isOnline = ref(navigator.onLine);

  // 计算属性
  const isMobile = computed(() => deviceType.value === 'mobile');
  const isDark = computed(() => theme.value === 'dark');

  // Actions
  function toggleSidebar() {
    sidebarCollapsed.value = !sidebarCollapsed.value;
    localStorage.setItem('smartclass_sidebar_collapsed', String(sidebarCollapsed.value));
  }

  function setSidebarCollapsed(collapsed: boolean) {
    sidebarCollapsed.value = collapsed;
    localStorage.setItem('smartclass_sidebar_collapsed', String(collapsed));
  }

  function setTheme(newTheme: 'light' | 'dark') {
    theme.value = newTheme;
    localStorage.setItem('smartclass_theme', newTheme);
    document.documentElement.setAttribute('data-theme', newTheme);
  }

  function toggleTheme() {
    setTheme(theme.value === 'light' ? 'dark' : 'light');
  }

  function setLanguage(lang: 'zh-CN' | 'en-US') {
    language.value = lang;
    localStorage.setItem('smartclass_language', lang);
    document.documentElement.lang = lang;
  }

  function setGlobalLoading(loading: boolean) {
    globalLoading.value = loading;
  }

  function addNotification(notification: Omit<typeof notifications.value[0], 'id'>) {
    const id = `${Date.now()}-${Math.random().toString(36).slice(2, 9)}`;
    notifications.value.push({ ...notification, id });
    
    // 自动移除
    const duration = notification.duration ?? 3000;
    setTimeout(() => {
      removeNotification(id);
    }, duration);
  }

  function removeNotification(id: string) {
    const index = notifications.value.findIndex(n => n.id === id);
    if (index > -1) notifications.value.splice(index, 1);
  }

  function clearNotifications() {
    notifications.value = [];
  }

  function setDeviceType(type: 'mobile' | 'tablet' | 'desktop') {
    deviceType.value = type;
  }

  function setOnlineStatus(online: boolean) {
    isOnline.value = online;
  }

  // 初始化
  function init() {
    // 监听系统主题变化
    const mediaQuery = window.matchMedia('(prefers-color-scheme: dark)');
    mediaQuery.addEventListener('change', (e) => {
      if (!localStorage.getItem('smartclass_theme')) {
        setTheme(e.matches ? 'dark' : 'light');
      }
    });
    
    // 监听在线状态
    window.addEventListener('online', () => setOnlineStatus(true));
    window.addEventListener('offline', () => setOnlineStatus(false));
    
    // 检测设备类型
    const updateDeviceType = () => {
      const width = window.innerWidth;
      if (width < 768) setDeviceType('mobile');
      else if (width < 1024) setDeviceType('tablet');
      else setDeviceType('desktop');
    };
    updateDeviceType();
    window.addEventListener('resize', updateDeviceType);
    
    // 应用主题
    document.documentElement.setAttribute('data-theme', theme.value);
    document.documentElement.lang = language.value;
  }

  return {
    // 状态
    sidebarCollapsed,
    theme,
    language,
    globalLoading,
    notifications,
    deviceType,
    isOnline,
    // 计算属性
    isMobile,
    isDark,
    // Actions
    toggleSidebar,
    setSidebarCollapsed,
    setTheme,
    toggleTheme,
    setLanguage,
    setGlobalLoading,
    addNotification,
    removeNotification,
    clearNotifications,
    setDeviceType,
    setOnlineStatus,
    init
  };
});