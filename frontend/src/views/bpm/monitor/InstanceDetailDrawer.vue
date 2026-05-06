<template>
  <a-drawer
    :open="open"
    :title="`实例详情 — ${record?.defName ?? ''}`"
    width="720"
    @close="emit('close')"
  >
    <template v-if="record">
      <a-descriptions :column="2" bordered size="small" style="margin-bottom: 16px">
        <a-descriptions-item label="流程定义">{{ record.defKey }} v{{ record.defVersion }}</a-descriptions-item>
        <a-descriptions-item label="状态">{{ record.state }}</a-descriptions-item>
        <a-descriptions-item label="申请人">{{ record.applyUserName }}</a-descriptions-item>
        <a-descriptions-item label="申请部门">{{ record.applyDeptName }}</a-descriptions-item>
        <a-descriptions-item label="开始时间">{{ record.startTime }}</a-descriptions-item>
        <a-descriptions-item label="结束时间">{{ record.endTime ?? '—' }}</a-descriptions-item>
      </a-descriptions>

      <a-tabs>
        <a-tab-pane key="diagram" tab="流程图">
          <DiagramViewer :inst-meta-id="record.id" />
        </a-tab-pane>
        <a-tab-pane key="history" tab="审批历史">
          <TaskHistoryTable :inst-meta-id="record.id" />
        </a-tab-pane>
      </a-tabs>
    </template>
  </a-drawer>
</template>

<script setup lang="ts">
import type { MonitorInstanceVO } from '/@/api/bpm/monitor';
import DiagramViewer from './DiagramViewer.vue';
import TaskHistoryTable from './TaskHistoryTable.vue';

defineProps<{ open: boolean; record: MonitorInstanceVO | null }>();
const emit = defineEmits<{ (e: 'close'): void }>();
</script>
