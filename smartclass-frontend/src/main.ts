import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { Lazyload } from 'vant';
// Vant 组件样式由 @vant/auto-import-resolver 按需注入；
// 函数式调用（showToast 等）不经过模板编译，样式需在此集中引入
import 'vant/es/toast/style';
import 'vant/es/dialog/style';
import 'vant/es/image-preview/style';
import 'vant/es/notify/style';
// 引入iconfont图标
import '../public/icons/iconfont.js';
import '../public/icons/iconfont.css';

// 导入Capacitor核心功能
import { SplashScreen } from '@capacitor/splash-screen';
import { Capacitor } from '@capacitor/core';
import capacitor from './capacitor';

// 设置网页标题
document.title = '智云星课';

// 添加全局返回按钮处理
window.handleBackButton = () => {
  try {
    const currentRoute = router.currentRoute.value;
    const path = currentRoute.path;

    console.log('处理返回按钮，当前路径:', path);

    // 一级页面列表（底部导航栏页面）
    const mainRoutes = ['/', '/chat', '/circle', '/courses', '/profile'];

    if (mainRoutes.includes(path)) {
      if (path !== '/') {
        // 如果在底部导航页面但不是主页，则返回到主页
        router.push('/');
        return true;
      }
      // 在主页，返回false允许默认行为
      return false;
    } else {
      // 如果不在一级页面，执行路由返回
      router.back();
      return true;
    }
  } catch {
    return false;
  }
};

const app = createApp(App);
const pinia = createPinia();

// 全局错误处理器，防止未捕获的错误导致整个应用崩溃
// TODO: 后续可接入错误上报系统（如 Sentry），需配置 DSN 与环境信息
app.config.errorHandler = (err) => {
  console.error('Global error:', err);
};

// 注册Capacitor服务
app.config.globalProperties.$capacitor = capacitor;

// 初始化Capacitor插件
const initCapacitor = async () => {
  if (Capacitor.isNativePlatform()) {
    // 隐藏启动页
    await SplashScreen.hide();

    // 触发网络状态初始化并注册监听，网络恢复由各页面自行订阅
    await capacitor.networkServices.getNetworkStatus();

    capacitor.networkServices.addNetworkListener(() => {
    });
  }
};

app.use(router);
// Vant 组件本体经 unplugin-vue-components 按需注册；此处仅注册 Lazyload 指令
app.use(Lazyload);
app.use(pinia);

// 挂载应用
app.mount('#app');

// 初始化Capacitor (在挂载之后)
initCapacitor().catch((error) => console.error('Capacitor初始化失败:', error));

// 声明全局类型
declare global {
  interface Window {
    handleBackButton: () => boolean;
  }
}
