<template>
  <div class="form-binding-page">
    <div class="filter-section">
      <a-input
        v-model:value="filterDefId"
        placeholder="输入流程定义ID"
        style="width: 200px"
      />
      <a-button type="primary" @click="handleSearch">查询</a-button>
      <a-button type="primary" @click="showCreateModal">新建绑定</a-button>
    </div>

    <a-table
      :columns="columns"
      :data-source="tableData"
      :loading="loading"
      :pagination="false"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'action'">
          <a-popconfirm
            title="确定删除吗？"
            ok-text="确定"
            cancel-text="取消"
            @confirm="handleDelete(record.id)"
          >
            <a-button type="link" danger>删除</a-button>
          </a-popconfirm>
        </template>
      </template>
    </a-table>

    <a-modal
      v-model:visible="createModalVisible"
      title="新建表单绑定"
      ok-text="确定"
      cancel-text="取消"
      @ok="handleCreateOk"
    >
      <a-form :model="createForm" layout="vertical">
        <a-form-item label="流程定义ID">
          <a-input v-model:value="createForm.defId" placeholder="输入流程定义ID" />
        </a-form-item>
        <a-form-item label="表单ID">
          <a-input v-model:value="createForm.formId" placeholder="输入表单ID" />
        </a-form-item>
        <a-form-item label="目的">
          <a-select v-model:value="createForm.purpose" placeholder="选择目的">
            <a-select-option value="APPLY">申请</a-select-option>
            <a-select-option value="APPROVE">审批</a-select-option>
            <a-select-option value="ARCHIVE">归档</a-select-option>
          </a-select>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { listFormBindings, createFormBinding, deleteFormBinding } from '/@/api/bpm/form-binding';
import { message } from 'ant-design-vue';

interface FormBinding {
  id: string;
  defId: string;
  formId: string;
  purpose: 'APPLY' | 'APPROVE' | 'ARCHIVE';
}

const columns = [
  {
    title: '流程定义ID',
    dataIndex: 'defId',
    key: 'defId',
  },
  {
    title: '表单ID',
    dataIndex: 'formId',
    key: 'formId',
  },
  {
    title: '目的',
    dataIndex: 'purpose',
    key: 'purpose',
  },
  {
    title: '操作',
    key: 'action',
    width: 100,
  },
];

const filterDefId = ref<string>('');
const tableData = ref<FormBinding[]>([]);
const loading = ref<boolean>(false);
const createModalVisible = ref<boolean>(false);
const createForm = ref({
  defId: '',
  formId: '',
  purpose: 'APPLY' as 'APPLY' | 'APPROVE' | 'ARCHIVE',
});

async function handleSearch() {
  loading.value = true;
  try {
    const res = await listFormBindings(filterDefId.value);
    tableData.value = res.data || [];
  } catch (err) {
    message.error('查询失败');
    console.error(err);
  } finally {
    loading.value = false;
  }
}

function showCreateModal() {
  createForm.value = {
    defId: '',
    formId: '',
    purpose: 'APPLY',
  };
  createModalVisible.value = true;
}

async function handleCreateOk() {
  if (!createForm.value.defId || !createForm.value.formId) {
    message.warning('请填写所有必填项');
    return;
  }

  try {
    await createFormBinding({
      defId: createForm.value.defId,
      formId: createForm.value.formId,
      purpose: createForm.value.purpose,
    });
    message.success('创建成功');
    createModalVisible.value = false;
    await handleSearch();
  } catch (err) {
    message.error('创建失败');
    console.error(err);
  }
}

async function handleDelete(id: string) {
  try {
    await deleteFormBinding(id);
    message.success('删除成功');
    await handleSearch();
  } catch (err) {
    message.error('删除失败');
    console.error(err);
  }
}

onMounted(() => {
  handleSearch();
});
</script>

<style scoped>
.form-binding-page {
  padding: 16px;
}

.filter-section {
  margin-bottom: 16px;
  display: flex;
  gap: 8px;
}
</style>
