<template>
  <div style="padding: 24px">
    <a-row :gutter="16" style="margin-bottom: 24px">
      <a-col :span="8">
        <a-card title="流程总览" size="small">
          <a-statistic title="总实例数" :value="summary.totalInstances" />
          <a-statistic title="已完成" :value="summary.completedInstances" style="margin-top: 8px" />
        </a-card>
      </a-col>
      <a-col :span="16">
        <a-form layout="inline">
          <a-form-item label="流程Key">
            <a-input v-model:value="query.defKey" allow-clear style="width: 160px" />
          </a-form-item>
          <a-form-item>
            <a-button type="primary" @click="loadStats">刷新</a-button>
          </a-form-item>
        </a-form>
      </a-col>
    </a-row>

    <a-row :gutter="16">
      <a-col :span="12">
        <a-card title="按流程定义统计" size="small">
          <div ref="defChartRef" style="height: 300px" />
        </a-card>
      </a-col>
      <a-col :span="12">
        <a-card title="按申请部门统计" size="small">
          <div ref="deptChartRef" style="height: 300px" />
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="16" style="margin-top: 16px">
      <a-col :span="24">
        <a-card title="流程定义明细" size="small">
          <a-table
            :columns="defColumns"
            :data-source="stats.byDefinition"
            :pagination="false"
            size="small"
            row-key="defKey"
          />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted, onBeforeUnmount, nextTick } from 'vue';
import { getStats } from '/@/api/bpm/monitor';

const query = reactive<{ defKey?: string }>({});
const stats = reactive<{
  byDefinition: any[];
  byApplyDept: any[];
}>({ byDefinition: [], byApplyDept: [] });

const defChartRef = ref<HTMLElement | null>(null);
const deptChartRef = ref<HTMLElement | null>(null);
let defChart: any = null;
let deptChart: any = null;

const summary = computed(() => ({
  totalInstances: stats.byDefinition.reduce((s: number, r: any) => s + (r.instanceCount || 0), 0),
  completedInstances: stats.byDefinition.reduce((s: number, r: any) => s + (r.completedCount || 0), 0),
}));

const defColumns = [
  { title: '流程Key', dataIndex: 'defKey', key: 'defKey' },
  { title: '流程名称', dataIndex: 'defName', key: 'defName' },
  { title: '总实例', dataIndex: 'instanceCount', key: 'instanceCount', width: 90 },
  { title: '已完成', dataIndex: 'completedCount', key: 'completedCount', width: 90 },
  {
    title: '完成率',
    key: 'completionRate',
    width: 90,
    customRender: ({ record }: any) =>
      record.completionRate != null ? `${(record.completionRate * 100).toFixed(1)}%` : '—',
  },
  {
    title: '平均耗时(ms)',
    key: 'avgDurationMs',
    width: 120,
    customRender: ({ record }: any) =>
      record.avgDurationMs != null ? Math.round(record.avgDurationMs) : '—',
  },
];

async function loadStats() {
  const res = await getStats({ ...query });
  stats.byDefinition = res.byDefinition || [];
  stats.byApplyDept = res.byApplyDept || [];
  await nextTick();
  renderCharts();
}

async function renderCharts() {
  const echarts = await import('echarts');
  renderDefChart(echarts);
  renderDeptChart(echarts);
}

function renderDefChart(echarts: any) {
  if (!defChartRef.value) return;
  if (!defChart) defChart = echarts.init(defChartRef.value);
  const data = stats.byDefinition.slice(0, 10);
  defChart.setOption({
    tooltip: {},
    xAxis: { type: 'category', data: data.map((r: any) => r.defKey || r.defName), axisLabel: { rotate: 30 } },
    yAxis: { type: 'value' },
    series: [
      { name: '总实例', type: 'bar', data: data.map((r: any) => r.instanceCount) },
      { name: '已完成', type: 'bar', data: data.map((r: any) => r.completedCount) },
    ],
    legend: { data: ['总实例', '已完成'] },
  });
}

function renderDeptChart(echarts: any) {
  if (!deptChartRef.value) return;
  if (!deptChart) deptChart = echarts.init(deptChartRef.value);
  const data = stats.byApplyDept.slice(0, 8);
  deptChart.setOption({
    tooltip: { trigger: 'item' },
    series: [
      {
        name: '部门申请',
        type: 'pie',
        radius: '60%',
        data: data.map((r: any) => ({
          name: r.applyDeptName || String(r.applyDeptId),
          value: r.instanceCount,
        })),
      },
    ],
  });
}

function handleResize() {
  defChart?.resize();
  deptChart?.resize();
}

onMounted(() => {
  loadStats();
  window.addEventListener('resize', handleResize);
});

onBeforeUnmount(() => {
  window.removeEventListener('resize', handleResize);
  defChart?.dispose();
  deptChart?.dispose();
});
</script>
