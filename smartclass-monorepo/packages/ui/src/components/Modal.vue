<script setup lang="ts">
import { ref, computed, watch, onMounted, onUnmounted } from 'vue';
import '@smartclass/styles/variables.css';
import Button from './Button.vue';

interface Props {
  modelValue: boolean;
  title?: string;
  width?: string | number;
  fullscreen?: boolean;
  top?: string;
  draggable?: boolean;
  modal?: boolean;
  closeOnClickModal?: boolean;
  closeOnPressEscape?: boolean;
  showClose?: boolean;
  beforeClose?: (done: () => void) => void;
  destroyOnClose?: boolean;
  appendToBody?: boolean;
  customClass?: string;
  footer?: boolean;
}

const props = withDefaults(defineProps<Props>(), {
  title: '',
  width: '500px',
  fullscreen: false,
  top: '15vh',
  draggable: false,
  modal: true,
  closeOnClickModal: true,
  closeOnPressEscape: true,
  showClose: true,
  destroyOnClose: false,
  appendToBody: true,
  footer: true
});

const emit = defineEmits<{
  'update:modelValue': [value: boolean];
  open: [];
  opened: [];
  close: [];
  closed: [];
  'before-close': [done: () => void];
}>();

const visible = ref(false);
const modalRef = ref<HTMLDivElement>();
const headerRef = ref<HTMLDivElement>();
const isDragging = ref(false);
const dragOffset = ref({ x: 0, y: 0 });

const modalStyle = computed(() => ({
  width: props.fullscreen ? '100%' : props.width,
  maxWidth: props.fullscreen ? 'none' : 'calc(100% - 32px)',
  top: props.fullscreen ? 0 : props.top
}));

const modalClass = computed(() => [
  'sc-modal',
  { 'is-fullscreen': props.fullscreen },
  { 'has-footer': props.footer && ($slots.footer || true) },
  props.customClass
]);

function handleClose() {
  if (props.beforeClose) {
    props.beforeClose(() => doClose());
  } else {
    doClose();
  }
}

function doClose() {
  if (!visible.value) return;
  visible.value = false;
  emit('update:modelValue', false);
  emit('close');
}

function handleKeyDown(event: KeyboardEvent) {
  if (event.key === 'Escape' && props.closeOnPressEscape) {
    handleClose();
  }
}

function handleOverlayClick() {
  if (props.closeOnClickModal) {
    handleClose();
  }
}

function handleDragStart(event: MouseEvent) {
  if (!props.draggable || !headerRef.value) return;
  if ((event.target as HTMLElement).closest('button')) return;
  
  isDragging.value = true;
  const rect = modalRef.value!.getBoundingClientRect();
  dragOffset.value = {
    x: event.clientX - rect.left,
    y: event.clientY - rect.top
  };
  
  document.addEventListener('mousemove', handleDragMove);
  document.addEventListener('mouseup', handleDragEnd);
  event.preventDefault();
}

function handleDragMove(event: MouseEvent) {
  if (!isDragging.value || !modalRef.value) return;
  
  const x = event.clientX - dragOffset.value.x;
  const y = event.clientY - dragOffset.value.y;
  
  modalRef.value.style.left = `${Math.max(0, Math.min(x, window.innerWidth - modalRef.value.offsetWidth))}px`;
  modalRef.value.style.top = `${Math.max(0, Math.min(y, window.innerHeight - modalRef.value.offsetHeight))}px`;
  modalRef.value.style.margin = '0';
}

function handleDragEnd() {
  isDragging.value = false;
  document.removeEventListener('mousemove', handleDragMove);
  document.removeEventListener('mouseup', handleDragEnd);
}

function handleAfterLeave() {
  emit('closed');
}

function handleAfterEnter() {
  emit('opened');
}

watch(() => props.modelValue, (val) => {
  visible.value = val;
  if (val) {
    emit('open');
    document.body.style.overflow = 'hidden';
  } else {
    document.body.style.overflow = '';
  }
}, { immediate: true });

onMounted(() => {
  document.addEventListener('keydown', handleKeyDown);
});

onUnmounted(() => {
  document.removeEventListener('keydown', handleKeyDown);
  document.body.style.overflow = '';
});
</script>

<template>
  <Transition name="sc-modal-fade">
    <div v-if="visible" class="sc-modal__wrapper" @click.self="handleOverlayClick">
      <div class="sc-modal__overlay" :class="{ 'is-fullscreen': props.fullscreen }"></div>
      
      <div
        ref="modalRef"
        :class="modalClass"
        :style="modalStyle"
        role="dialog"
        aria-modal="true"
        :aria-labelledby="props.title ? 'sc-modal-title' : undefined"
      >
        <div v-if="props.draggable" ref="headerRef" class="sc-modal__header" @mousedown="handleDragStart">
          <span id="sc-modal-title" class="sc-modal__title">{{ props.title }}</span>
          
          <div class="sc-modal__header-btns">
            <button
              v-if="props.showClose"
              type="button"
              class="sc-modal__close"
              @click="handleClose"
              aria-label="关闭"
            >
              <i class="sc-icon sc-icon-close"></i>
            </button>
          </div>
        </div>
        
        <template v-else>
          <div class="sc-modal__header">
            <span id="sc-modal-title" class="sc-modal__title">{{ props.title }}</span>
            <div class="sc-modal__header-btns">
              <button
                v-if="props.showClose"
                type="button"
                class="sc-modal__close"
                @click="handleClose"
                aria-label="关闭"
              >
                <i class="sc-icon sc-icon-close"></i>
              </button>
            </div>
          </div>
        </template>
        
        <div class="sc-modal__body">
          <slot></slot>
        </div>
        
        <div v-if="props.footer" class="sc-modal__footer">
          <slot name="footer">
            <Button size="small" @click="handleClose">取消</Button>
            <Button size="small" type="primary" @click="$emit('confirm')">确定</Button>
          </slot>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
.sc-modal__wrapper {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: var(--z-index-modal);
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 16px;
  overflow: auto;
}

.sc-modal__overlay {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  animation: sc-fade-in 0.2s ease;
}

.sc-modal__overlay.is-fullscreen {
  background-color: var(--color-bg-primary);
}

.sc-modal {
  position: relative;
  background-color: var(--color-bg-primary);
  border-radius: var(--radius-lg);
  box-shadow: var(--shadow-xl);
  display: flex;
  flex-direction: column;
  max-height: 90vh;
  animation: sc-slide-up 0.3s ease;
}

.sc-modal.is-fullscreen {
  width: 100%;
  height: 100%;
  max-height: 100%;
  border-radius: 0;
}

.sc-modal__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 24px;
  border-bottom: 1px solid var(--color-border-primary);
  cursor: move;
}

.sc-modal__title {
  font-size: var(--font-size-lg);
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  margin: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sc-modal__header-btns {
  display: flex;
  gap: 8px;
}

.sc-modal__close {
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
  transition: all var(--transition-fast);
}

.sc-modal__close:hover {
  background-color: var(--color-bg-tertiary);
  color: var(--color-text-primary);
}

.sc-modal__body {
  padding: 24px;
  overflow: auto;
  flex: 1;
}

.sc-modal__footer {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px;
  border-top: 1px solid var(--color-border-primary);
  border-bottom-left-radius: var(--radius-lg);
  border-bottom-right-radius: var(--radius-lg);
}

@keyframes sc-fade-in {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes sc-slide-up {
  from {
    opacity: 0;
    transform: translateY(-20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.sc-modal-fade-enter-active,
.sc-modal-fade-leave-active {
  transition: all 0.3s ease;
}

.sc-modal-fade-enter-from,
.sc-modal-fade-leave-to {
  opacity: 0;
}
</style>