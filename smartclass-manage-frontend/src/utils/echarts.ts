// ECharts 按需注册：仅引入管理端实际使用的图表与组件，替代全量 echarts 引入。
// 新增图表类型时在此处补注册，页面继续使用统一的 <EChartsReact /> 包装。
import React from 'react';
import * as echarts from 'echarts/core';
import { BarChart, HeatmapChart, LineChart, PieChart, RadarChart } from 'echarts/charts';
import {
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
} from 'echarts/components';
import { CanvasRenderer } from 'echarts/renderers';
import ReactEChartsCore from 'echarts-for-react/lib/core';

echarts.use([
  BarChart,
  HeatmapChart,
  LineChart,
  PieChart,
  RadarChart,
  GridComponent,
  LegendComponent,
  TitleComponent,
  TooltipComponent,
  VisualMapComponent,
  CanvasRenderer,
]);

export type EChartsReactProps = React.ComponentProps<typeof ReactEChartsCore>;

// 注入按需 echarts 实例的包装组件，用法与 echarts-for-react 默认导出一致
export const EChartsReact: React.FC<EChartsReactProps> = (props) =>
  React.createElement(ReactEChartsCore, { echarts, ...props });
