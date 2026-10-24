<script setup lang="ts">
import { computed } from 'vue';
import '@smartclass/styles/variables.css';

interface Props {
  src?: string;
  alt?: string;
  size?: 'large' | 'default' | 'small' | number;
  shape?: 'circle' | 'square';
  fit?: 'cover' | 'contain' | 'fill' | 'none' | 'scale-down';
  srcSet?: string;
  alt?: string;
}

const props = withDefaults(defineProps<Props>(), {
  size: 'default',
  shape: 'circle',
  fit: 'cover'
});

const avatarClass = computed(() => [
  'sc-avatar',
  { [`sc-avatar--${typeof props.size === 'number' ? 'custom' : props.size}`]: true },
  { [`sc-avatar--${props.shape}`]: true }
]);

const sizeStyle = computed(() => {
  if (typeof props.size === 'number') {
    return { width: `${props.size}px`, height: `${props.size}px`, fontSize: `${props.size * 0.4}px` };
  }
  return {};
});

// 图片加载失败时移除 img 并标记错误态
function handleError(event: Event) {
  const host = event.currentTarget as HTMLElement | null;
  if (!host) return;
  host.querySelector('img')?.remove();
  host.classList.add('sc-avatar--error');
}

</script>

<template>
  <span :class="avatarClass" :style="sizeStyle" role="img" :aria-label="alt || '用户头像'">
    <img
      v-if="src"
      :src="src"
      :srcset="srcSet"
      :alt="alt"
      :style="{ objectFit: fit }"
      class="sc-avatar__image"
      @error="handleError"
    />
    <span v-else class="sc-avatar__placeholder">
      <slot>{{ alt || '用户' }}</slot>
    </span>
  </span>
</template>


<style scoped>
.sc-avatar {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-tertiary);
  vertical-align: middle;
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  user-select: none;
}

.sc-avatar--large { width: 48px; height: 48px; font-size: 20px; }
.sc-avatar--default { width: 36px; height: 36px; font-size: 16px; }
.sc-avatar--small { width: 28px; height: 28px; font-size: 14px; }
.sc-avatar--custom { font-size: calc(var(--size, 36px) * 0.4); }

.sc-avatar--circle { border-radius: 50%; }
.sc-avatar--square { border-radius: var(--radius-md); }

.sc-avatar__image {
  width: 100%;
  height: 100%;
  display: block;
}

.sc-avatar__placeholder {
  width: 100%;
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-tertiary);
  text-transform: uppercase;
  font-weight: var(--font-weight-semibold);
}

.sc-avatar--error .sc-avatar__placeholder {
  background-color: var(--color-error-light);
  color: var(--color-error);
}
</style>