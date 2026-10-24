<script setup lang="ts">
import { ref, computed, provide, inject, reactive } from 'vue';
import '@smartclass/styles/variables.css';

interface FormRules {
  [key: string]: Array<{
    required?: boolean;
    message?: string;
    trigger?: 'blur' | 'change' | 'submit';
    pattern?: RegExp;
    validator?: (rule: any, value: any, callback: (error?: Error) => void) => void;
    min?: number;
    max?: number;
    len?: number;
    type?: string;
    transform?: (value: any) => any;
  }>;
}

interface FormItemProps {
  label?: string;
  prop?: string;
  labelWidth?: string | number;
  required?: boolean;
  rules?: FormRules[string];
  error?: string;
  showMessage?: boolean;
  inlineMessage?: boolean;
}

interface FormProps {
  model: Record<string, any>;
  rules?: FormRules;
  labelWidth?: string | number;
  labelPosition?: 'left' | 'right' | 'top';
  inline?: boolean;
  statusIcon?: boolean;
  hideRequiredAsterisk?: boolean;
  showMessage?: boolean;
  inlineMessage?: boolean;
  disabled?: boolean;
  size?: 'large' | 'default' | 'small';
  validateOnRuleChange?: boolean;
}

const props = withDefaults(defineProps<FormProps>(), {
  labelWidth: '120px',
  labelPosition: 'right',
  inline: false,
  statusIcon: false,
  hideRequiredAsterisk: false,
  showMessage: true,
  inlineMessage: false,
  disabled: false,
  size: 'default',
  validateOnRuleChange: true
});

const emit = defineEmits<{
  validate: [prop: string, isValid: boolean, message: string];
}>();

const formRef = ref<HTMLFormElement>();
const fields = ref<Map<string, any>>(new Map());

// 注入表单上下文
const formContext = reactive({
  model: props.model,
  rules: props.rules || {},
  labelWidth: props.labelWidth,
  labelPosition: props.labelPosition,
  inline: props.inline,
  statusIcon: props.statusIcon,
  hideRequiredAsterisk: props.hideRequiredAsterisk,
  showMessage: props.showMessage,
  inlineMessage: props.inlineMessage,
  disabled: props.disabled,
  size: props.size,
  fields,
  addField: (field: any) => {
    if (field.prop) fields.value.set(field.prop, field);
  },
  removeField: (prop: string) => {
    fields.value.delete(prop);
  },
  validate: async (callback?: (valid: boolean, failedFields: any[]) => void) => {
    return await validate(callback);
  },
  validateField: async (props: string | string[], callback?: (error: Error | null) => void) => {
    return await validateField(props, callback);
  },
  resetFields: (props?: string[]) => {
    resetFields(props);
  },
  clearValidate: (props?: string[]) => {
    clearValidate(props);
  }
});

provide('sc-form', formContext);

// 验证整个表单
async function validate(callback?: (valid: boolean, failedFields: any[]) => void) {
  const fieldArray = Array.from(fields.value.values());
  const promises = fieldArray.map(field => {
    if (field.prop) return field.validate();
    return Promise.resolve();
  });
  
  try {
    await Promise.all(promises);
    callback?.(true, []);
    return true;
  } catch (failedFields) {
    callback?.(false, failedFields);
    return false;
  }
}

// 验证指定字段
async function validateField(props: string | string[], callback?: (error: Error | null) => void) {
  const propArray = Array.isArray(props) ? props : [props];
  const fieldArray = propArray.map(p => fields.value.get(p)).filter(Boolean);
  
  try {
    await Promise.all(fieldArray.map(field => field.validate()));
    callback?.(null);
    return true;
  } catch (errors) {
    callback?.(errors[0]);
    return false;
  }
}

// 重置字段
function resetFields(props?: string[]) {
  const targetFields = props 
    ? props.map(p => fields.value.get(p)).filter(Boolean)
    : Array.from(fields.value.values());
  
  targetFields.forEach(field => {
    if (field.resetField) field.resetField();
  });
}

// 清除验证状态
function clearValidate(props?: string[]) {
  const targetFields = props
    ? props.map(p => fields.value.get(p)).filter(Boolean)
    : Array.from(fields.value.values());
  
  targetFields.forEach(field => {
    if (field.clearValidate) field.clearValidate();
  });
}

// 提交表单
function handleSubmit(event: Event) {
  event.preventDefault();
  validate((valid) => {
    emit('submit', valid);
  });
}

function handleReset(event: Event) {
  event.preventDefault();
  resetFields();
  emit('reset');
}
</script>

<template>
  <form ref="formRef" class="sc-form" @submit="handleSubmit" @reset="handleReset" novalidate>
    <slot></slot>
  </form>
</template>

<style scoped>
.sc-form {
  display: block;
  font-family: var(--font-family-sans);
}

.sc-form--inline {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 24px;
}

.sc-form--inline .sc-form-item {
  flex: 1;
  min-width: 200px;
}
</style>