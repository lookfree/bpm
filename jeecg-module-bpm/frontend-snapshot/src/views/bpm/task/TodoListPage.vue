<template>
  <div class="todo-list-page">
    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-button type="link" @click="handleApprove(record.id)">审批</a-button>
        </template>
      </template>
    </a-table>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { listTodo } from '/@/api/bpm/task';

interface Task {
  id: string;
  name: string;
  processDefinitionId: string;
  createTime: string;
}

const router = useRouter();

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
    title: '创建时间',
    dataIndex: 'createTime',
    key: 'createTime',
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
  },
];

const tableData = ref<Task[]>([]);
const loading = ref<boolean>(false);

async function loadTodoList() {
  loading.value = true;
  try {
    const res = await listTodo();
    tableData.value = res.data || [];
  } catch (err) {
    console.error('Failed to load todo list:', err);
  } finally {
    loading.value = false;
  }
}

function handleApprove(taskId: string) {
  router.push(`/bpm/task/approve/${taskId}`);
}

onMounted(() => {
  loadTodoList();
});
</script>

<style scoped>
.todo-list-page {
  padding: 16px;
}
</style>
