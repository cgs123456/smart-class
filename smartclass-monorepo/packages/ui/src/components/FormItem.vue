<script setup lang="ts">
import { ref, computed, inject, onMounted, onUnmounted, watch } from 'vue';
import '@smartclass/styles/variables.css';

interface FormItemProps {
  label?: string;
  prop?: string;
  labelWidth?: string | number;
  required?: boolean;
  rules?: any[];
  error?: string;
  showMessage?: boolean;
  inlineMessage?: boolean;
  for?: string;
}

const props = withDefaults(defineProps<FormItemProps>(), {
  required: false,
  showMessage: true,
  inlineMessage: false
});

const formContext = inject<any>('sc-form', null);
const inputRef = ref<HTMLElement>();

const fieldContext = ref<any>({
  prop: props.prop,
  validate: async () => {},
  resetField: () => {},
  clearValidate: () => {}
});

const labelWidth = computed(() => props.labelWidth || formContext?.labelWidth || '120px');
const labelPosition = computed(() => formContext?.labelPosition || 'right');
const inline = computed(() => formContext?.inline || false);
const size = computed(() => formContext?.size || 'default');
const disabled = computed(() => formContext?.disabled || false);
const showMessage = computed(() => props.showMessage && (formContext?.showMessage !== false));
const inlineMessage = computed(() => props.inlineMessage || formContext?.inlineMessage || false);
const statusIcon = computed(() => formContext?.statusIcon || false);
const hideRequiredAsterisk = computed(() => formContext?.hideRequiredAsterisk || false);

const classes = computed(() => [
  'sc-form-item',
  { 'is-required': isRequired.value },
  { 'is-error': validateState.value === 'error' },
  { 'is-validating': validateState.value === 'validating' },
  { [`label-${labelPosition.value}`]: labelPosition.value !== 'top' },
  { 'is-inline': inline.value },
  { [`is-${size.value}`]: size.value }
]);

const isRequired = computed(() => {
  if (props.required !== undefined) return props.required;
  if (!formContext?.rules || !props.prop) return false;
  const rules = formContext.rules[props.prop];
  if (!rules) return false;
  return rules.some((rule: any) => rule.required);
});

const validateState = ref<'success' | 'error' | 'validating' | ''>('');
const validateMessage = ref('');

const validate = async (trigger?: string) => {
  if (!formContext?.rules || !props.prop) return;
  
  const rules = formContext.rules[props.prop];
  if (!rules || rules.length === 0) return;
  
  const filteredRules = trigger 
    ? rules.filter((r: any) => !r.trigger || r.trigger === trigger)
    : rules;
  
  if (filteredRules.length === 0) return;
  
  validateState.value = 'validating';
  validateMessage.value = '';
  
  try {
    await Promise.all(filteredRules.map(async (rule: any) => {
      const value = formContext.model[props.prop];
      
      if (rule.required && (value === undefined || value === null || value === '')) {
        throw new Error(rule.message || `${props.label || props.prop} 不能为空`);
      }
      
      if (value !== undefined && value !== null && value !== '') {
        if (rule.pattern && !rule.pattern.test(value)) {
          throw new Error(rule.message || '格式不正确');
        }
        
        if (rule.min !== undefined && String(value).length < rule.min) {
          throw new Error(rule.message || `长度不能少于 ${rule.min} 个字符`);
        }
        
        if (rule.max !== undefined && String(value).length > rule.max) {
          throw new Error(rule.message || `长度不能超过 ${rule.max} 个字符`);
        }
        
        if (rule.len !== undefined && String(value).length !== rule.len) {
          throw new Error(rule.message || `长度必须为 ${rule.len} 个字符`);
        }
        
        if (rule.validator) {
          await new Promise<void>((resolve, reject) => {
            rule.validator(rule, value, (error?: Error) => {
              if (error) reject(error);
              else resolve();
            });
          });
        }
      }
    }));
    
    validateState.value = 'success';
  } catch (error: any) {
    validateState.value = 'error';
    validateMessage.value = error.message || '验证失败';
  }
};

const resetField = () => {
  validateState.value = '';
  validateMessage.value = '';
};

const clearValidate = () => {
  validateState.value = '';
  validateMessage.value = '';
};

onMounted(() => {
  if (formContext && props.prop) {
    formContext.addField(fieldContext.value);
    fieldContext.value.validate = validate;
    fieldContext.value.resetField = resetField;
    fieldContext.value.clearValidate = clearValidate;
  }
});

onUnmounted(() => {
  if (formContext && props.prop) {
    formContext.removeField(props.prop);
  }
});

watch(() => formContext?.model?.[props.prop], () => {
  if (validateState.value === 'error' || validateState.value === 'success') {
    validate('change');
  }
});
</script>

<template>
  <div :class="classes">
    <label
      v-if="label || $slots.label"
      :for="props.for || props.prop"
      :style="{ width: labelWidth, flexBasis: labelWidth }"
      class="sc-form-item__label"
    >
      <slot name="label">
        <span>{{ label }}</span>
        <template v-if="isRequired && !hideRequiredAsterisk">
          <span class="sc-form-item__required" aria-hidden="true">*</span>
        </template>
      </slot>
    </label>
    
    <div class="sc-form-item__content">
      <div class="sc-form-item__input-wrapper">
        <slot></slot>
        
        <div v-if="statusIcon && validateState" class="sc-form-item__status-icon">
          <i v-if="validateState === 'validating'" class="sc-icon sc-icon-loading sc-icon-spin"></i>
          <i v-else-if="validateState === 'success'" class="sc-icon sc-icon-check" style="color: var(--color-success);"></i>
          <i v-else-if="validateState === 'error'" class="sc-icon sc-icon-close" style="color: var(--color-error);"></i>
        </div>
      </div>
      
      <transition name="sc-form-item-feedback">
        <div 
          v-if="showMessage && validateMessage && (validateState === 'error' || (validateState === 'success' && statusIcon))"
          :class="['sc-form-item__feedback', { 'is-inline': inlineMessage }]"
        >
          <i v-if="validateState === 'error'" class="sc-icon sc-icon-error"></i>
          <span>{{ validateMessage }}</span>
        </div>
      </transition>
    </div>
  </div>
</template>

<style scoped>
.sc-form-item {
  display: flex;
  align-items: flex-start;
  margin-bottom: 24px;
  font-family: var(--font-family-sans);
}

.sc-form-item.is-inline {
  display: inline-flex;
  margin-bottom: 0;
  margin-right: 24px;
  vertical-align: bottom;
}

.sc-form-item__label {
  display: flex;
  align-items: center;
  font-size: var(--font-size-base);
  color: var(--color-text-primary);
  font-weight: var(--font-weight-normal);
  white-space: nowrap;
  padding: 8px 0;
  box-sizing: border-box;
}

.sc-form-item__label[style*="right"] {
  text-align: right;
  padding-right: 16px;
}

.sc-form-item__label[style*="top"] {
  margin-bottom: 8px;
  padding-right: 0;
}

.sc-form-item__required {
  color: var(--color-error);
  margin-left: 4px;
  font-family: inherit;
}

.sc-form-item__content {
  display: flex;
  flex: 1;
  align-items: center;
  min-width: 0;
}

.sc-form-item__input-wrapper {
  display: flex;
  align-items: center;
  flex: 1;
  position: relative;
}

.sc-form-item__input-wrapper:focus-within {
  z-index: 1;
}

.sc-form-item__status-icon {
  position: absolute;
  right: 12px;
  pointer-events: none;
  font-size: 14px;
  transition: all var(--transition-fast);
}

.sc-form-item__feedback {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 6px 10px;
  font-size: var(--font-size-sm);
  border-radius: var(--radius-sm);
  color: var(--color-error);
  background-color: var(--color-error-light);
}

.sc-form-item__feedback.is-inline {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 4px;
  min-width: 200px;
  z-index: 10;
  box-shadow: var(--shadow-md);
}

.sc-form-item__feedback .sc-icon-error {
  flex-shrink: 0;
}

.sc-form-item-feedback-enter-active,
.sc-form-item-feedback-leave-active {
  transition: all 0.3s ease;
}

.sc-form-item-feedback-enter-from,
.sc-form-item-feedback-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

/* Size variants */
.sc-form-item.is-large .sc-form-item__label {
  font-size: var(--font-size-lg);
  padding: 10px 0;
}

.sc-form-item.is-small .sc-form-item__label {
  font-size: var(--font-size-sm);
  padding: 6px 0;
}
</style>