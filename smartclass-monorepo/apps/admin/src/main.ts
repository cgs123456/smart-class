import { createApp } from 'vue';
import { createPinia } from 'pinia';
import App from './App.vue';
import '@smartclass/ui/styles';

const app = createApp(App);
app.use(createPinia());
app.mount('#app');
