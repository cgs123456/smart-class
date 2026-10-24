<script setup lang="ts">
import { RouterView } from 'vue-router';
import { useAppStore } from '@smartclass/stores';

const appStore = useAppStore();
</script>

<template>
  <div class="app">
    <RouterView v-slot="{ Component }">
      <transition name="page" mode="out-in">
        <component :is="Component" />
      </transition>
    </RouterView>
    
    <!-- 全局加载 -->
    <div v-if="appStore.globalLoading" class="global-loading">
      <div class="global-loading__spinner"></div>
    </div>
    
    <!-- 离线提示 -->
    <div v-if="!appStore.isOnline" class="offline-banner">
      <i class="sc-icon sc-icon-wifi-off"></i>
      <span>当前处于离线状态，部分功能可能不可用</span>
    </div>
  </div>
</template>

<style>
.app {
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.page-enter-active,
.page-leave-active {
  transition: all 0.3s ease;
}

.page-enter-from,
.page-leave-to {
  opacity: 0;
  transform: translateX(20px);
}

.global-loading {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 3px;
  background: linear-gradient(90deg, var(--color-primary), var(--color-primary-hover));
  z-index: 9999;
}

.global-loading__spinner {
  width: 100%;
  height: 100%;
  background: linear-gradient(90deg, transparent, var(--color-text-inverse), transparent);
  animation: loading-slide 1.5s infinite;
}

@keyframes loading-slide {
  0% { transform: translateX(-100%); }
  100% { transform: translateX(100%); }
}

.offline-banner {
  position: fixed;
  bottom: 0;
  left: 0;
  right: 0;
  padding: 12px 16px;
  background-color: var(--color-warning);
  color: var(--color-warning-900);
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  font-size: var(--font-size-sm);
  z-index: 999;
  animation: slide-up 0.3s ease;
}

@keyframes slide-up {
  from {
    opacity: 0;
    transform: translateY(100%);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>