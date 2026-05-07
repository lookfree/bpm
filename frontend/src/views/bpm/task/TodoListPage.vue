<template>
  <div class="todo-list-page">
    <a-card title="待办任务">
      <template #extra>
        <a-button type="primary" @click="loadTodoList" :loading="loading">刷新</a-button>
      </template>
      <a-table
        :columns="columns"
        :data-source="tableData"
        :loading="loading"
        :pagination="false"
        rowKey="taskId"
        bordered
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button type="link" size="small" @click="handleApprove(record.taskId)">审批</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listTodo } from '/@/api/bpm/task';

const router = useRouter();

const columns = [
  { title: '任务名称', dataIndex: 'taskName', key: 'taskName' },
  { title: '业务ID', dataIndex: 'businessKey', key: 'businessKey', width: 160 },
  { title: '状态', dataIndex: 'instanceState', key: 'instanceState', width: 100 },
  { title: '创建时间', dataIndex: 'createTime', key: 'createTime', width: 170 },
  { title: '操作', key: 'action', width: 100 },
];

const tableData = ref<any[]>([]);
const loading = ref(false);

async function loadTodoList() {
  loading.value = true;
  try {
    const res = await listTodo();
    tableData.value = Array.isArray(res) ? res : [];
  } catch (err) {
    console.error('Failed to load todo list:', err);
  } finally {
    loading.value = false;
  }
}

function handleApprove(taskId: string) {
  router.push(`/bpm/task/approve/${taskId}`);
}

onMounted(loadTodoList);
</script>

<style scoped>
.todo-list-page {
  padding: 24px;
}
</style>
