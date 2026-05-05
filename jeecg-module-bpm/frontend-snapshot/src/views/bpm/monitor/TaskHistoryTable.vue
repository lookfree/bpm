<template>
  <a-table
    :columns="columns"
    :data-source="records"
    :loading="loading"
    :pagination="false"
    size="small"
    row-key="id"
  />
</template>

<script setup lang="ts">
import { ref, watch } from 'vue';
import { defHttp } from '/@/utils/http/axios';

const props = defineProps<{ instMetaId: string }>();

interface TaskHistoryRecord {
  id: string;
  nodeId: string;
  assigneeId: number;
  action: string;
  comment: string;
  opTime: string;
}

const records = ref<TaskHistoryRecord[]>([]);
const loading = ref(false);

const columns = [
  { title: '节点', dataIndex: 'nodeId', key: 'nodeId', width: 120 },
  { title: '操作人', dataIndex: 'assigneeId', key: 'assigneeId', width: 100 },
  { title: '动作', dataIndex: 'action', key: 'action', width: 100 },
  { title: '意见', dataIndex: 'comment', key: 'comment' },
  { title: '时间', dataIndex: 'opTime', key: 'opTime', width: 160 },
];

async function load() {
  if (!props.instMetaId) return;
  loading.value = true;
  try {
    const res = await defHttp.get({ url: `/bpm/v1/instance/${props.instMetaId}/history` });
    records.value = res || [];
  } catch {
    records.value = [];
  } finally {
    loading.value = false;
  }
}

watch(() => props.instMetaId, load, { immediate: true });
</script>
