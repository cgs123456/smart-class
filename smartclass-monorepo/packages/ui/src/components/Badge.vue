<script setup lang="ts">
import { computed } from 'vue';
import '@smartclass/styles/variables.css';

interface Props {
  value?: string | number;
  max?: number;
  isDot?: boolean;
  hidden?: boolean;
  type?: 'primary' | 'success' | 'warning' | 'error' | 'info';
  size?: 'large' | 'default' | 'small';
  showZero?: boolean;
  color?: string;
  offset?: [number, number];
}

const props = withDefaults(defineProps<Props>(), {
  max: 99,
  isDot: false,
  hidden: false,
  type: 'error',
  size: 'default',
  showZero: false,
  offset: [0, 0] as [number, number]
});

const badgeClass = computed(() => [
  'sc-badge',
  { 'is-dot': props.isDot },
  { 'is-fixed': !props.isDot },
  { [`sc-badge--${props.type}`]: !props.color },
  { [`sc-badge--${props.size}`]: !props.isDot },
  { 'is-hidden': props.hidden }
]);

const content = computed(() => {
  if (props.isDot) return '';
  if (typeof props.value === 'number' && props.value > props.max) {
    return `${props.max}+`;
  }
  return props.value?.toString() || '';
});

const style = computed(() => {
  if (!props.color) return {};
  return {
    backgroundColor: props.color,
    borderColor: props.color
  };
});

const offsetStyle = computed(() => {
  if (!props.offset || props.offset[0] === 0 && props.offset[1] === 0) return {};
  return {
    transform: `translate(${props.offset[0]}px, ${props.offset[1]}px)`
  };
});

const isDot = computed(() => props.isDot);
const sizeFontSize = computed(() => {
  if (props.size === 'large') return '12px';
  if (props.size === 'small') return '10px';
  return '11px';
});
</script>

<template>
  <span :class="badgeClass" :style="offsetStyle">
    <slot></slot>
    <sup
      v-if="!isDot && !hidden && (showZero || content)"
      :class="['sc-badge__content', { 'sc-badge__content--fixed': !isDot }]"
      :style="[style, { fontSize: sizeFontSize }]"
    >
      {{ content }}
    </sup>
  </span>
</template>


<style scoped>
.sc-badge {
  position: relative;
  display: inline-flex;
  vertical-align: middle;
}

.sc-badge__content {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  font-size: 11px;
  font-weight: var(--font-weight-bold);
  line-height: 1;
  color: var(--color-text-inverse);
  border-radius: 8px;
  background-color: var(--color-error);
  border: 1px solid var(--color-bg-primary);
  box-sizing: border-box;
  white-space: nowrap;
}

.sc-badge__content--fixed {
  border-radius: 8px;
}

.sc-badge--large .sc-badge__content {
  height: 18px;
  padding: 0 6px;
  font-size: 12px;
  min-width: 18px;
}

.sc-badge--small .sc-badge__content {
  height: 14px;
  padding: 0 3px;
  font-size: 10px;
  min-width: 14px;
}

/* Types */
.sc-badge--primary .sc-badge__content { background-color: var(--color-primary); }
.sc-badge--success .sc-badge__content { background-color: var(--color-success); }
.sc-badge--warning .sc-badge__content { background-color: var(--color-warning); }
.sc-badge--error .sc-badge__content { background-color: var(--color-error); }
.sc-badge--info .sc-badge__content { background-color: var(--color-primary); }

/* Dot */
.sc-badge.is-dot:after {
  content: '';
  position: absolute;
  top: 0;
  right: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: var(--color-error);
  border: 2px solid var(--color-bg-primary);
}

.sc-badge.is-dot.sc-badge--primary:after { background-color: var(--color-primary); }
.sc-badge.is-dot.sc-badge--success:after { background-color: var(--color-success); }
.sc-badge.is-dot.sc-badge--warning:after { background-color: var(--color-warning); }
.sc-badge.is-dot.sc-badge--error:after { background-color: var(--color-error); }
.sc-badge.is-dot.sc-badge--info:after { background-color: var(--color-primary); }

.sc-badge.is-dot.sc-badge--large:after { width: 10px; height: 10px; }
.sc-badge.is-dot.sc-badge--small:after { width: 6px; height: 6px; }
</style>