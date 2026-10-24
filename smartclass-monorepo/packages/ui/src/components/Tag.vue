<script setup lang="ts">
import { computed, useSlots } from 'vue';
import '@smartclass/styles/variables.css';

interface Props {
  type?: 'primary' | 'success' | 'warning' | 'error' | 'info';
  size?: 'large' | 'default' | 'small';
  closable?: boolean;
  disableTransitions?: boolean;
  hit?: boolean;
  color?: string;
  effect?: 'light' | 'dark' | 'plain';
  round?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'primary',
  size: 'default',
  closable: false,
  disableTransitions: false,
  hit: false,
  effect: 'light',
  round: false
});

const emit = defineEmits<{
  close: [event: MouseEvent];
  click: [event: MouseEvent];
}>();

const slots = useSlots();

const tagClass = computed(() => [
  'sc-tag',
  { [`sc-tag--${props.type}`]: !props.color && props.effect !== 'plain' },
  { [`sc-tag--${props.size}`]: true },
  { 'is-closable': props.closable },
  { 'is-hit': props.hit },
  { 'is-round': props.round },
  { [`sc-tag--${props.effect}`]: props.effect !== 'light' },
  { 'has-color': !!props.color }
]);

const tagStyle = computed(() => {
  if (!props.color) return {};
  if (props.effect === 'plain') {
    return { color: props.color, borderColor: props.color };
  }
  return { backgroundColor: props.color, borderColor: props.color };
});
import { computed } from 'vue';

const handleClick = (event: MouseEvent) => {
  if (!props.hit) return;
  emit('click', event);
};

const handleClose = (event: MouseEvent) => {
  event.stopPropagation();
  emit('close', event);
};
</script>

<template>
  <span
    :class="tagClass"
    :style="tagStyle"
    @click="handleClick"
    role="button"
    tabindex="0"
    :aria-pressed="hit"
  >
    <span class="sc-tag__content">
      <slot></slot>
    </span>
    <button
      v-if="closable"
      type="button"
      class="sc-tag__close"
      @click.stop="handleClose"
      aria-label="关闭标签"
    >
      <i class="sc-icon sc-icon-close"></i>
    </button>
  </span>
</template>


<style scoped>
.sc-tag {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 24px;
  padding: 0 8px;
  font-size: var(--font-size-sm);
  font-weight: var(--font-weight-medium);
  line-height: 1;
  border-radius: var(--radius-sm);
  border: 1px solid transparent;
  white-space: nowrap;
  vertical-align: middle;
  transition: all var(--transition-fast);
  box-sizing: border-box;
}

.sc-tag--large { height: 28px; padding: 0 10px; font-size: var(--font-size-base); }
.sc-tag--small { height: 20px; padding: 0 6px; font-size: var(--font-size-xs); }

.sc-tag.is-round { border-radius: var(--radius-full); }

.sc-tag__content {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.sc-tag__close {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  width: 14px;
  height: 14px;
  border: none;
  background: transparent;
  color: inherit;
  opacity: 0.6;
  cursor: pointer;
  border-radius: 50%;
  transition: opacity var(--transition-fast);
}

.sc-tag__close:hover { opacity: 1; }

/* Light effect (default) */
.sc-tag--primary { background-color: var(--color-primary-light); color: var(--color-primary-700); border-color: var(--color-primary-200); }
.sc-tag--success { background-color: var(--color-success-light); color: var(--color-success-700); border-color: var(--color-success-200); }
.sc-tag--warning { background-color: var(--color-warning-light); color: var(--color-warning-700); border-color: var(--color-warning-200); }
.sc-tag--error { background-color: var(--color-error-light); color: var(--color-error-700); border-color: var(--color-error-200); }
.sc-tag--info { background-color: var(--color-primary-light); color: var(--color-primary-700); border-color: var(--color-primary-200); }

/* Dark effect */
.sc-tag--dark.sc-tag--primary { background-color: var(--color-primary); color: var(--color-text-inverse); }
.sc-tag--dark.sc-tag--success { background-color: var(--color-success); color: var(--color-text-inverse); }
.sc-tag--dark.sc-tag--warning { background-color: var(--color-warning); color: var(--color-text-inverse); }
.sc-tag--dark.sc-tag--error { background-color: var(--color-error); color: var(--color-text-inverse); }
.sc-tag--dark.sc-tag--info { background-color: var(--color-primary); color: var(--color-text-inverse); }

/* Plain effect */
.sc-tag--plain.sc-tag--primary { color: var(--color-primary); background-color: transparent; border-color: var(--color-primary); }
.sc-tag--plain.sc-tag--success { color: var(--color-success); background-color: transparent; border-color: var(--color-success); }
.sc-tag--plain.sc-tag--warning { color: var(--color-warning); background-color: transparent; border-color: var(--color-warning); }
.sc-tag--plain.sc-tag--error { color: var(--color-error); background-color: transparent; border-color: var(--color-error); }
.sc-tag--plain.sc-tag--info { color: var(--color-primary); background-color: transparent; border-color: var(--color-primary); }

/* Hit state */
.sc-tag.is-hit { cursor: pointer; }
.sc-tag.is-hit:hover { opacity: 0.8; }
.sc-tag.is-hit:active { transform: scale(0.98); }

/* Custom color */
.sc-tag.has-color { color: var(--color-text-inverse); }
</style>