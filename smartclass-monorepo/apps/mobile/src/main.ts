import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import router from './router';
import { useAppStore } from '@smartclass/stores';
import '@smartclass/styles';

// PWA 注册
if ('serviceWorker' in navigator) {
  window.addEventListener('load', () => {
    navigator.serviceWorker.register('/sw.js').catch(() => {
      // 忽略注册失败
    });
  });
}

const app = createApp(App);
const pinia = createPinia();

app.use(pinia);
app.use(router);

// 初始化全局 Store
const appStore = useAppStore();
appStore.init();

app.mount('#app');