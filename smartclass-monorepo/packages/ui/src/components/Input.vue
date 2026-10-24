<script setup lang="ts">
import { ref, computed } from 'vue';
import '@smartclass/styles/variables.css';

interface Props {
  modelValue: string;
  type?: 'text' | 'password' | 'textarea' | 'number' | 'email' | 'url' | 'search';
  size?: 'large' | 'default' | 'small';
  disabled?: boolean;
  readonly?: boolean;
  placeholder?: string;
  clearable?: boolean;
  showPassword?: boolean;
  prefixIcon?: string;
  suffixIcon?: string;
  maxLength?: number;
  showWordLimit?: boolean;
  autocomplete?: string;
  autofocus?: boolean;
  rows?: number;
  autosize?: boolean | { minRows: number; maxRows: number };
  formatter?: (value: string) => string;
  parser?: (value: string) => string;
}

const props = withDefaults(defineProps<Props>(), {
  type: 'text',
  size: 'default',
  disabled: false,
  readonly: false,
  clearable: false,
  showPassword: false,
  showWordLimit: false,
  rows: 2,
  autosize: false
});

const emit = defineEmits<{
  'update:modelValue': [value: string];
  change: [value: string];
  focus: [event: FocusEvent];
  blur: [event: FocusEvent];
  input: [value: string];
  clear: [];
}>();

const inputRef = ref<HTMLInputElement | HTMLTextAreaElement>();
const showPasswordVisible = ref(false);
const isFocused = ref(false);
const isHovered = ref(false);

const inputClass = computed(() => [
  'sc-input',
  `sc-input--${props.size}`,
  { 'is-disabled': props.disabled },
  { 'is-readonly': props.readonly },
  { 'is-focused': isFocused.value },
  { 'is-hovered': isHovered.value },
  { 'has-prefix': !!props.prefixIcon || !!$slots.prefix },
  { 'has-suffix': !!props.suffixIcon || !!$slots.suffix || props.showPassword || props.clearable && modelValue },
  { 'has-word-limit': props.showWordLimit && props.maxLength }
]);

const textareaClass = computed(() => [
  'sc-textarea',
  `sc-textarea--${props.size}`,
  { 'is-disabled': props.disabled },
  { 'is-readonly': props.readonly },
  { 'is-focused': isFocused.value },
  { 'is-hovered': isHovered.value }
]);

const computedMaxLength = computed(() => props.maxLength || (props.showWordLimit ? 0 : undefined));

function handleInput(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement;
  let value = target.value;
  if (props.parser) value = props.parser(value);
  emit('update:modelValue', value);
  emit('input', value);
}

function handleChange(event: Event) {
  const target = event.target as HTMLInputElement | HTMLTextAreaElement;
  emit('change', target.value);
}

function handleFocus(event: FocusEvent) {
  isFocused.value = true;
  emit('focus', event);
}

function handleBlur(event: FocusEvent) {
  isFocused.value = false;
  emit('blur', event);
}

function handleClear() {
  emit('update:modelValue', '');
  emit('clear');
}

function togglePasswordVisibility() {
  showPasswordVisible.value = !showPasswordVisible.value;
  nextTick(() => inputRef.value?.focus());
}
</script>

<template>
  <div :class="inputClass" @mouseenter="isHovered = true" @mouseleave="isHovered = false">
    <!-- Input / Textarea -->
    <div class="sc-input__inner">
      <!-- Prefix -->
      <span v-if="props.prefixIcon || $slots.prefix" class="sc-input__prefix">
        <slot name="prefix">
          <i v-if="props.prefixIcon" :class="['sc-icon', props.prefixIcon]"></i>
        </slot>
      </span>
      
      <template v-if="props.type === 'textarea'">
        <textarea
          ref="inputRef"
          v-bind="$attrs"
          :class="textareaClass"
          :value="modelValue"
          :disabled="disabled"
          :readonly="readonly"
          :placeholder="placeholder"
          :maxlength="computedMaxLength"
          :rows="rows"
          @input="handleInput"
          @change="handleChange"
          @focus="handleFocus"
          @blur="handleBlur"
          style="resize: vertical;"
        ></textarea>
      </template>
      
      <template v-else>
        <input
          ref="inputRef"
          v-bind="$attrs"
          :class="['sc-input__native', `sc-input__native--${props.size}`]"
          :type="showPasswordVisible ? 'text' : props.type"
          :value="modelValue"
          :disabled="disabled"
          :readonly="readonly"
          :placeholder="placeholder"
          :maxlength="computedMaxLength"
          :autocomplete="autocomplete"
          :autofocus="autofocus"
          @input="handleInput"
          @change="handleChange"
          @focus="handleFocus"
          @blur="handleBlur"
        />
      </template>
      
      <!-- Suffix -->
      <span v-if="props.suffixIcon || $slots.suffix || props.showPassword || (props.clearable && !props.disabled && !props.readonly && modelValue)" class="sc-input__suffix">
        <slot name="suffix">
          <i v-if="props.suffixIcon" :class="['sc-icon', props.suffixIcon]"></i>
        </slot>
        
        <!-- Show Password Toggle -->
        <button
          v-if="props.showPassword"
          type="button"
          class="sc-input__password-toggle"
          @click="togglePasswordVisibility"
          :aria-label="showPasswordVisible ? '隐藏密码' : '显示密码'"
        >
          <i :class="['sc-icon', showPasswordVisible ? 'sc-icon-eye-off' : 'sc-icon-eye']"></i>
        </button>
        
        <!-- Clear Button -->
        <button
          v-else-if="props.clearable && !props.disabled && !props.readonly && modelValue"
          type="button"
          class="sc-input__clear"
          @click="handleClear"
          aria-label="清空内容"
        >
          <i class="sc-icon sc-icon-close"></i>
        </button>
      </span>
    </div>
    
    <!-- Word Limit -->
    <div v-if="props.showWordLimit && props.maxLength" class="sc-input__count">
      <span class="sc-input__count-inner">{{ modelValue.length }}/{{ props.maxLength }}</span>
    </div>
  </div>
</template>

<style scoped>
.sc-input,
.sc-textarea {
  display: inline-flex;
  flex-direction: column;
  width: 100%;
  font-family: var(--font-family-sans);
}

.sc-input__inner,
.sc-textarea__inner {
  display: flex;
  align-items: center;
  background-color: var(--color-bg-primary);
  border: 1px solid var(--color-border-primary);
  border-radius: var(--radius-md);
  transition: all var(--transition-fast);
}

.sc-input__inner:hover:not(.is-disabled):not(.is-readonly),
.sc-textarea__inner:hover:not(.is-disabled):not(.is-readonly) {
  border-color: var(--color-border-secondary);
}

.sc-input__inner.is-hovered:not(.is-disabled):not(.is-readonly),
.sc-textarea__inner.is-hovered:not(.is-disabled):not(.is-readonly) {
  border-color: var(--color-border-secondary);
}

.sc-input__inner.is-focused,
.sc-textarea__inner.is-focused {
  border-color: var(--color-border-focus);
  box-shadow: 0 0 0 3px var(--color-primary-light);
}

.sc-input__inner.is-disabled,
.sc-textarea__inner.is-disabled {
  background-color: var(--color-bg-tertiary);
  border-color: var(--color-border-primary);
  cursor: not-allowed;
}

.sc-input__inner.is-readonly,
.sc-textarea__inner.is-readonly {
  background-color: var(--color-bg-secondary);
}

/* 尺寸 */
.sc-input--large .sc-input__native,
.sc-textarea--large {
  padding: 12px 16px;
  font-size: var(--font-size-lg);
}

.sc-input--default .sc-input__native,
.sc-textarea--default {
  padding: 8px 12px;
  font-size: var(--font-size-base);
}

.sc-input--small .sc-input__native,
.sc-textarea--small {
  padding: 6px 10px;
  font-size: var(--font-size-sm);
}

/* 原生 Input */
.sc-input__native {
  flex: 1;
  min-width: 0;
  background: transparent;
  border: none;
  outline: none;
  color: var(--color-text-primary);
  font-family: inherit;
  line-height: 1.5;
}

.sc-input__native::placeholder {
  color: var(--color-text-tertiary);
}

.sc-input__native:disabled,
.sc-input__native[readonly] {
  color: var(--color-text-tertiary);
}

/* Textarea */
.sc-textarea {
  min-height: 80px;
}

.sc-textarea__inner {
  padding: 8px 12px;
}

.sc-textarea--large { min-height: 100px; }
.sc-textarea--small { min-height: 60px; }

/* Prefix / Suffix */
.sc-input__prefix,
.sc-input__suffix {
  display: flex;
  align-items: center;
  color: var(--color-text-tertiary);
  padding: 0 12px;
}

.sc-input__prefix { border-right: 1px solid var(--color-border-primary); }
.sc-input__suffix { border-left: 1px solid var(--color-border-primary); }

.sc-input__password-toggle,
.sc-input__clear {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  color: var(--color-text-tertiary);
  cursor: pointer;
  border-radius: var(--radius-sm);
  transition: color var(--transition-fast);
}

.sc-input__password-toggle:hover,
.sc-input__clear:hover {
  color: var(--color-text-secondary);
}

/* Word Limit */
.sc-input__count {
  display: flex;
  justify-content: flex-end;
  margin-top: 4px;
  font-size: var(--font-size-xs);
  color: var(--color-text-tertiary);
}

.sc-input__count-inner {
  display: inline-block;
}

/* Icon sizes */
.sc-input--large .sc-icon { font-size: 20px; }
.sc-input--default .sc-icon { font-size: 16px; }
.sc-input--small .sc-icon { font-size: 14px; }
</style>