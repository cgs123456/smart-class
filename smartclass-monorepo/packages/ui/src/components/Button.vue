<script setup lang="ts">
import { computed } from 'vue';
import '@smartclass/styles/variables.css';

interface Props {
  type?: 'primary' | 'success' | 'warning' | 'error' | 'info' | 'default';
  size?: 'large' | 'default' | 'small';
  disabled?: boolean;
  loading?: boolean;
  nativeType?: 'button' | 'submit' | 'reset';
  round?: boolean;
  circle?: boolean;
  plain?: boolean;
  autofocus?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
  size: 'default',
  disabled: false,
  loading: false,
  nativeType: 'button',
  round: false,
  circle: false,
  plain: false,
  autofocus: false
});

const emit = defineEmits<{
  click: [event: MouseEvent];
}>();

const buttonClass = computed(() => [
  'sc-button',
  `sc-button--${props.type}`,
  `sc-button--${props.size}`,
  { 'is-disabled': props.disabled || props.loading },
  { 'is-round': props.round },
  { 'is-circle': props.circle },
  { 'is-plain': props.plain }
]);

function handleClick(event: MouseEvent) {
  if (!props.disabled && !props.loading) {
    emit('click', event);
  }
}
</script>

<template>
  <button
    :class="buttonClass"
    :type="nativeType"
    :disabled="disabled || loading"
    :autofocus="autofocus"
    @click="handleClick"
  >
    <span v-if="loading" class="sc-button__loader" aria-hidden="true">
      <svg class="sc-icon-spin" viewBox="0 0 50 50">
        <circle cx="25" cy="25" r="20" fill="none" stroke="currentColor" stroke-width="3" stroke-linecap="round" stroke-dasharray="80 150" />
      </svg>
    </span>
    <slot></slot>
  </button>
</template>

<style scoped>
.sc-button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-family: var(--font-family-sans);
  font-weight: var(--font-weight-medium);
  white-space: nowrap;
  user-select: none;
  border: 1px solid transparent;
  cursor: pointer;
  transition: all var(--transition-fast);
  position: relative;
  outline: none;
}

.sc-button:focus-visible {
  outline: 2px solid var(--color-border-focus);
  outline-offset: 2px;
}

.sc-button.is-disabled,
.sc-button[disabled] {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 尺寸 */
.sc-button--large {
  height: 44px;
  padding: 0 20px;
  font-size: var(--font-size-lg);
  border-radius: var(--radius-lg);
}

.sc-button--default {
  height: 36px;
  padding: 0 16px;
  font-size: var(--font-size-base);
  border-radius: var(--radius-md);
}

.sc-button--small {
  height: 28px;
  padding: 0 12px;
  font-size: var(--font-size-sm);
  border-radius: var(--radius-sm);
}

/* 圆角/圆形 */
.sc-button.is-round {
  border-radius: var(--radius-full);
}

.sc-button.is-circle {
  padding: 0;
  width: 36px;
  height: 36px;
  border-radius: var(--radius-full);
}

.sc-button--large.is-circle {
  width: 44px;
  height: 44px;
}

.sc-button--small.is-circle {
  width: 28px;
  height: 28px;
}

/* 类型 - Primary */
.sc-button--primary {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-inverse);
}

.sc-button--primary:hover:not(.is-disabled) {
  background-color: var(--color-primary-hover);
  border-color: var(--color-primary-hover);
}

.sc-button--primary:active:not(.is-disabled) {
  background-color: var(--color-primary-active);
  border-color: var(--color-primary-active);
}

.sc-button--primary.is-plain {
  background-color: transparent;
  border-color: var(--color-primary);
  color: var(--color-primary);
}

.sc-button--primary.is-plain:hover:not(.is-disabled) {
  background-color: var(--color-primary-light);
}

/* Success */
.sc-button--success {
  background-color: var(--color-success);
  border-color: var(--color-success);
  color: var(--color-text-inverse);
}

.sc-button--success:hover:not(.is-disabled) {
  background-color: var(--color-success);
  filter: brightness(1.1);
}

/* Warning */
.sc-button--warning {
  background-color: var(--color-warning);
  border-color: var(--color-warning);
  color: var(--color-text-inverse);
}

/* Error/Danger */
.sc-button--error {
  background-color: var(--color-error);
  border-color: var(--color-error);
  color: var(--color-text-inverse);
}

/* Info */
.sc-button--info {
  background-color: var(--color-primary);
  border-color: var(--color-primary);
  color: var(--color-text-inverse);
}

/* Default */
.sc-button--default {
  background-color: var(--color-bg-tertiary);
  border-color: var(--color-border-primary);
  color: var(--color-text-primary);
}

.sc-button--default:hover:not(.is-disabled) {
  background-color: var(--color-border-secondary);
}

/* 加载状态 */
.sc-button__loader {
  display: inline-flex;
  margin-right: 8px;
  width: 1em;
  height: 1em;
  animation: sc-spin 1s linear infinite;
}

.sc-button--small .sc-button__loader {
  margin-right: 6px;
}

@keyframes sc-spin {
  from { transform: rotate(0deg); }
  to { transform: rotate(360deg); }
}

.sc-icon-spin {
  animation: sc-spin 1s linear infinite;
}
</style>