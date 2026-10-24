<script setup lang="ts">
import { ref, computed } from 'vue';
import '@smartclass/styles/variables.css';

interface Column<T = any> {
  prop: string;
  label: string;
  width?: string | number;
  minWidth?: string | number;
  fixed?: 'left' | 'right';
  sortable?: boolean;
  formatter?: (value: any, row: T, index: number) => string | number;
  align?: 'left' | 'center' | 'right';
  headerAlign?: 'left' | 'center' | 'right';
  slot?: string;
}

interface Props {
  data: any[];
  columns: Column[];
  border?: boolean;
  stripe?: boolean;
  size?: 'large' | 'default' | 'small';
  highlightCurrentRow?: boolean;
  rowKey?: string;
  emptyText?: string;
  loading?: boolean;
  maxHeight?: string | number;
  showHeader?: boolean;
  showSummary?: boolean;
  sumText?: string;
  summaryMethod?: (param: { columns: Column[]; data: any[] }) => string[];
}

const props = withDefaults(defineProps<Props>(), {
  border: false,
  stripe: false,
  size: 'default',
  highlightCurrentRow: false,
  rowKey: 'id',
  emptyText: '暂无数据',
  loading: false,
  showHeader: true,
  showSummary: false,
  sumText: '合计'
});

const emit = defineEmits<{
  'sort-change': [column: Column, prop: string, order: 'ascending' | 'descending' | null];
  'current-change': [currentRow: any, oldCurrentRow: any];
  'select': [selection: any[], row: any];
  'select-all': [selection: any[]];
  'selection-change': [selection: any[]];
  'row-click': [row: any, column: Column, event: MouseEvent];
  'row-dblclick': [row: any, column: Column, event: MouseEvent];
  'cell-click': [row: any, column: Column, cell: HTMLTableCellElement, event: MouseEvent];
  'cell-dblclick': [row: any, column: Column, cell: HTMLTableCellElement, event: MouseEvent];
  'header-click': [column: Column, event: MouseEvent];
  'expand-change': [row: any, expandedRows: any[]];
}>;

const currentRow = ref<any>(null);
const sortColumn = ref<Column | null>(null);
const sortOrder = ref<'ascending' | 'descending' | null>(null);
const selection = ref<any[]>([]);

const tableClass = computed(() => [
  'sc-table',
  { 'is-bordered': props.border },
  { 'is-striped': props.stripe },
  { [`is-${props.size}`]: props.size },
  { 'has-fixed-left': columns.value.some(c => c.fixed === 'left') },
  { 'has-fixed-right': columns.value.some(c => c.fixed === 'right') }
]);

const columns = computed(() => props.columns.map((col, index) => ({
  ...col,
  _index: index,
  align: col.align || 'left',
  headerAlign: col.headerAlign || col.align || 'left'
})));

function handleSort(column: Column) {
  if (!column.sortable) return;
  
  let order: 'ascending' | 'descending' | null = 'ascending';
  if (sortColumn.value?.prop === column.prop) {
    if (sortOrder.value === 'ascending') order = 'descending';
    else if (sortOrder.value === 'descending') order = null;
    else order = 'ascending';
  }
  
  sortColumn.value = order ? column : null;
  sortOrder.value = order;
  emit('sort-change', column, column.prop, order);
}

function handleRowClick(row: any, column: Column, event: MouseEvent) {
  if (props.highlightCurrentRow) {
    currentRow.value = row;
  }
  emit('row-click', row, column, event);
}

function handleRowDblClick(row: any, column: Column, event: MouseEvent) {
  emit('row-dblclick', row, column, event);
}

function handleCellClick(row: any, column: Column, cell: HTMLTableCellElement, event: MouseEvent) {
  emit('cell-click', row, column, cell, event);
}

function handleHeaderClick(column: Column, event: MouseEvent) {
  if (column.sortable) {
    handleSort(column);
  }
  emit('header-click', column, event);
}

function handleSelect(row: any) {
  const index = selection.value.findIndex(r => r[props.rowKey] === row[props.rowKey]);
  if (index > -1) {
    selection.value.splice(index, 1);
  } else {
    selection.value.push(row);
  }
  emit('select', [...selection.value], row);
  emit('selection-change', [...selection.value]);
}

function handleSelectAll() {
  if (isAllSelected.value) {
    selection.value = [];
  } else {
    selection.value = [...props.data];
  }
  emit('select-all', [...selection.value]);
  emit('selection-change', [...selection.value]);
}

const isAllSelected = computed(() => 
  props.data.length > 0 && selection.value.length === props.data.length
);

const isIndeterminate = computed(() => 
  selection.value.length > 0 && selection.value.length < props.data.length
);
const isRowSelected = (row: any) => {
  return selection.value.some(r => r[props.rowKey] === row[props.rowKey]);
};

const handleCellDblClick = (row: any, column: any, cell: HTMLTableCellElement, event: MouseEvent) => {
  emit('cell-dblclick', row, column, cell, event);
};
</script>

<template>
  <div :class="tableClass">
    <!-- Loading Overlay -->
    <div v-if="loading" class="sc-table__loading">
      <div class="sc-table__loading-spinner"></div>
    </div>
    
    <div class="sc-table__wrapper" :style="{ maxHeight: props.maxHeight }">
      <table class="sc-table__table" cellspacing="0" cellpadding="0">
        <colgroup>
          <template v-for="column in columns" :key="column.prop">
            <col :style="{ width: column.width, minWidth: column.minWidth }" />
          </template>
        </colgroup>
        
        <!-- Header -->
        <thead v-if="showHeader" class="sc-table__header">
          <tr>
            <template v-for="column in columns" :key="column.prop">
              <th
                :class="[
                  'sc-table__header-cell',
                  { sortable: column.sortable, 'is-fixed-left': column.fixed === 'left', 'is-fixed-right': column.fixed === 'right' }
                ]"
                :style="{ textAlign: column.headerAlign, width: column.width, minWidth: column.minWidth }"
                @click="handleHeaderClick(column, $event)"
              >
                <div class="sc-table__header-wrapper">
                  <slot :name="`header-${column.prop}`">
                    <span>{{ column.label }}</span>
                  </slot>
                  <i v-if="column.sortable" class="sc-table__sort-icon">
                    <i v-if="sortColumn?.prop === column.prop && sortOrder === 'ascending'" class="sc-icon sc-icon-arrow-up"></i>
                    <i v-else-if="sortColumn?.prop === column.prop && sortOrder === 'descending'" class="sc-icon sc-icon-arrow-down"></i>
                    <i v-else class="sc-icon sc-icon-sort"></i>
                  </i>
                </div>
              </th>
            </template>
          </tr>
        </thead>
        
        <!-- Body -->
        <tbody class="sc-table__body">
          <template v-if="data.length === 0">
            <tr>
              <td :colspan="columns.length" class="sc-table__empty">
                <slot name="empty">{{ emptyText }}</slot>
              </td>
            </tr>
          </template>
          
          <tr
            v-else
            v-for="(row, rowIndex) in data"
            :key="row[props.rowKey] || rowIndex"
            :class="[
              'sc-table__row',
              { 'current-row': highlightCurrentRow && currentRow === row },
              { 'is-selected': isRowSelected(row) },
              { 'sc-table__row--striped': stripe && rowIndex % 2 === 1 }
            ]"
            @click="handleRowClick(row, { prop: '' } as any, $event)"
            @dblclick="handleRowDblClick(row, { prop: '' } as any, $event)"
          >
            <template v-for="column in columns" :key="column.prop">
              <td
                :class="[
                  'sc-table__cell',
                  { 'is-fixed-left': column.fixed === 'left', 'is-fixed-right': column.fixed === 'right' }
                ]"
                :style="{ textAlign: column.align, width: column.width, minWidth: column.minWidth }"
                @click="handleCellClick(row, column, $event.target as HTMLTableCellElement, $event)"
                @dblclick="handleCellDblClick(row, column, $event.target as HTMLTableCellElement, $event)"
              >
                <div class="sc-table__cell-wrapper">
                  <slot :name="column.slot || column.prop" :row="row" :column="column" :index="rowIndex" :value="row[column.prop]">
                    <template v-if="column.formatter">
                      {{ column.formatter(row[column.prop], row, rowIndex) }}
                    </template>
                    <template v-else>
                      {{ row[column.prop] }}
                    </template>
                  </slot>
                </div>
              </td>
            </template>
          </tr>
        </tbody>
        
        <!-- Summary -->
        <tfoot v-if="showSummary" class="sc-table__summary">
          <tr>
            <template v-for="column in columns" :key="column.prop">
              <td
                :class="[
                  'sc-table__summary-cell',
                  { 'is-fixed-left': column.fixed === 'left', 'is-fixed-right': column.fixed === 'right' }
                ]"
                :style="{ textAlign: column.align }"
              >
                <div class="sc-table__summary-wrapper">
                  <slot :name="`summary-${column.prop}`">
                    <span v-if="column.prop === columns[0].prop">{{ sumText }}</span>
                    <span v-else-if="summaryMethod">
                      {{ summaryMethod({ columns: columns, data: data })[column._index] }}
                    </span>
                  </slot>
                </div>
              </td>
            </template>
          </tr>
        </tfoot>
      </table>
    </div>
  </div>
</template>


<style scoped>
.sc-table {
  width: 100%;
  font-family: var(--font-family-sans);
  color: var(--color-text-primary);
}

.sc-table__wrapper {
  position: relative;
  overflow: auto;
}

.sc-table__table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
}

.sc-table__header {
  background-color: var(--color-bg-secondary);
}

.sc-table__header-cell {
  padding: 12px 16px;
  font-weight: var(--font-weight-semibold);
  color: var(--color-text-primary);
  text-align: left;
  white-space: nowrap;
  border-bottom: 1px solid var(--color-border-primary);
  user-select: none;
  transition: background-color var(--transition-fast);
}

.sc-table__header-cell.sortable:hover {
  background-color: var(--color-bg-tertiary);
  cursor: pointer;
}

.sc-table__header-wrapper {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.sc-table__sort-icon {
  display: flex;
  color: var(--color-text-tertiary);
  transition: color var(--transition-fast);
}

.sc-table__sort-icon .sc-icon-arrow-up,
.sc-table__sort-icon .sc-icon-arrow-down {
  color: var(--color-primary);
}

.sc-table__body {
  background-color: var(--color-bg-primary);
}

.sc-table__row {
  transition: background-color var(--transition-fast);
}

.sc-table__row:hover {
  background-color: var(--color-bg-secondary);
}

.sc-table__row.current-row {
  background-color: var(--color-primary-light);
}

.sc-table__row.is-selected {
  background-color: var(--color-primary-light);
}

.sc-table__row--striped {
  background-color: var(--color-bg-secondary);
}

.sc-table__cell {
  padding: 12px 16px;
  border-bottom: 1px solid var(--color-border-primary);
  word-break: break-all;
  vertical-align: middle;
}

.sc-table__cell-wrapper {
  display: flex;
  align-items: center;
}

.sc-table__empty {
  padding: 48px 16px;
  text-align: center;
  color: var(--color-text-tertiary);
}

.sc-table__summary {
  background-color: var(--color-bg-secondary);
  border-top: 2px solid var(--color-border-primary);
}

.sc-table__summary-cell {
  padding: 12px 16px;
  font-weight: var(--font-weight-medium);
  color: var(--color-text-secondary);
}

.sc-table__summary-wrapper {
  display: flex;
  align-items: center;
}

.sc-table__loading {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(255, 255, 255, 0.9);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 10;
}

.sc-table__loading-spinner {
  width: 32px;
  height: 32px;
  border: 3px solid var(--color-border-primary);
  border-top-color: var(--color-primary);
  border-radius: 50%;
  animation: sc-spin 1s linear infinite;
}

@keyframes sc-spin {
  to { transform: rotate(360deg); }
}

/* Fixed columns */
.sc-table__header-cell.is-fixed-left,
.sc-table__cell.is-fixed-left {
  position: sticky;
  left: 0;
  background-color: var(--color-bg-secondary);
  z-index: 2;
  box-shadow: 2px 0 4px -2px rgba(0, 0, 0, 0.1);
}

.sc-table__header-cell.is-fixed-right,
.sc-table__cell.is-fixed-right {
  position: sticky;
  right: 0;
  background-color: var(--color-bg-secondary);
  z-index: 2;
  box-shadow: -2px 0 4px -2px rgba(0, 0, 0, 0.1);
}

.sc-table__body .sc-table__cell.is-fixed-left,
.sc-table__body .sc-table__cell.is-fixed-right {
  background-color: var(--color-bg-primary);
}

/* Size variants */
.sc-table.is-large .sc-table__header-cell,
.sc-table.is-large .sc-table__cell {
  padding: 16px 20px;
}

.sc-table.is-small .sc-table__header-cell,
.sc-table.is-small .sc-table__cell {
  padding: 8px 12px;
}
</style>