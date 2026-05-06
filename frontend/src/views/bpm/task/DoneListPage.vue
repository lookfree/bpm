<template>
  <div class="done-list-page">
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { listDone } from '/@/api/bpm/task';

interface Task {
  id: string;
  name: string;
  processDefinitionId: string;
  endTime: string;
}

const columns = [
  {
    title: '任务ID',
    dataIndex: 'id',
    key: 'id',
  },
  {
    title: '任务名称',
    dataIndex: 'name',
    key: 'name',
  },
  {
    title: '流程定义ID',
    dataIndex: 'processDefinitionId',
    key: 'processDefinitionId',
  },
  {
    title: '完成时间',
    dataIndex: 'endTime',
    key: 'endTime',
  },
];

const tableData = ref<Task[]>([]);
const loading = ref<boolean>(false);

async function loadDoneList() {
  loading.value = true;
  try {
    const res = await listDone();
    tableData.value = res.data || [];
  } catch (err) {
    console.error('Failed to load done list:', err);
  } finally {
    loading.value = false;
  }
}

onMounted(() => {
  loadDoneList();
});
</script>

<style scoped>
.done-list-page {
  padding: 16px;
}
</style>
