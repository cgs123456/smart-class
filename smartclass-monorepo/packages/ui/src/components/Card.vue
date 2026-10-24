<script setup lang="ts">
import '@smartclass/styles/variables.css';

interface Props {
  title?: string;
  subtitle?: string;
  extra?: string;
  bordered?: boolean;
  hoverable?: boolean;
  shadow?: 'never' | 'always' | 'hover';
  bodyStyle?: Record<string, string>;
}

const props = withDefaults(defineProps<Props>(), {
  bordered: true,
  hoverable: false,
  shadow: 'never'
});

const cardClass = computed(() => [
  'sc-card',
  { 'is-bordered': props.bordered },
  { 'is-hoverable': props.hoverable },
  { 'shadow-never': props.shadow === 'never' },
  { 'shadow-always': props.shadow === 'always' },
  { 'shadow-hover': props.shadow === 'hover' }
]);
import { computed } from 'vue';
</script>

<template>
  <div :class="cardClass" :style="props.bodyStyle">
    <div v-if="props.title || props.extra || $slots.header" class="sc-card__header">
      <div class="sc-card__header-main">
        <div v-if="props.title" class="sc-card__title">{{ props.title }}</div>
        <div v-if="props.subtitle" class="sc-card__subtitle">{{ props.subtitle }}</div>
      </div>
      <div v-if="props.extra || $slots.extra" class="sc-card__extra">
        <slot name="extra">{{ props.extra }}</slot>
      </div>
    </div>
    
    <div class="sc-card__body">
      <slot></slot>
    </div>
    
    <div v-if="$slots.footer" class="sc-card__footer">
      <slot name="footer"></slot>
    </div>
  </div>
</template>


<style scoped>
.sc-card {
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  transition: all var(--transition-normal);
}

.sc-card.is-bordered {
  border: 1px solid var(--color-border-primary);
}

.sc-card.is-hoverable:hover {
  box-shadow: var(--shadow-md);
  transform: translateY(-2px);
}

.sc-card.shadow-always {
  box-shadow: var(--shadow-md);
}

.sc-card.shadow-hover:hover {
  box-shadow: var(--shadow-lg);
}

.sc-card__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--color-border-primary);
}

.sc-card__header-main {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.sc-card__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
}

.sc-card__subtitle {
  font-size: var(--font-size-sm);
  color: var(--color-text-secondary);
  margin: 0;
}

.sc-card__extra {
  color: var(--color-text-secondary);
  font-size: var(--font-size-sm);
}

.sc-card__body {
  padding: 20px;
}

.sc-card__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--color-border-primary);
  border-bottom-left-radius: var(--radius-lg);
  border-bottom-right-radius: var(--radius-lg);
  background-color: var(--color-bg-secondary);
}
</style>