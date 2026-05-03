<template>
  <div class="task-approve-page">
    <a-spin :spinning="loading">
      <a-form :model="formData" layout="vertical">
        <div v-for="field in visibleFields" :key="field.fieldName">
          <a-form-item :label="field.fieldLabel">
            <a-input
              v-model:value="formData[field.fieldName]"
              :disabled="field.readonly"
            />
          </a-form-item>
        </div>
      </a-form>

      <a-form-item label="审批意见">
        <a-textarea
          v-model:value="comment"
          placeholder="请输入审批意见"
          :rows="4"
        />
      </a-form-item>

      <div class="action-buttons">
        <a-button type="primary" @click="handleApprove">同意</a-button>
        <a-button danger @click="handleReject">拒绝</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getTaskForm, completeTask } from '/@/api/bpm/task';
import { message } from 'ant-design-vue';

interface FormField {
  fieldName: string;
  fieldLabel: string;
  readonly?: boolean;
  hidden?: boolean;
}

const router = useRouter();
const route = useRoute();

const taskId = route.params.taskId as string;
const loading = ref<boolean>(false);
const formData = ref<Record<string, any>>({});
const comment = ref<string>('');
const schema = ref<{ fields: FormField[] }>({ fields: [] });

const visibleFields = computed(() => {
  return schema.value.fields.filter((field) => !field.hidden);
});

async function loadTaskForm() {
  loading.value = true;
  try {
    const res = await getTaskForm(taskId);
    schema.value = res.schema || { fields: [] };
    formData.value = res.data || {};
  } catch (err) {
    message.error('加载表单失败');
    console.error(err);
  } finally {
    loading.value = false;
  }
}

async function handleApprove() {
  try {
    await completeTask(taskId, {
      action: 'APPROVE',
      comment: comment.value,
      formData: formData.value,
    });
    message.success('审批成功');
    router.push('/bpm/todo');
  } catch (err) {
    message.error('审批失败');
    console.error(err);
  }
}

async function handleReject() {
  try {
    await completeTask(taskId, {
      action: 'REJECT',
      comment: comment.value,
      formData: formData.value,
    });
    message.success('拒绝成功');
    router.push('/bpm/todo');
  } catch (err) {
    message.error('拒绝失败');
    console.error(err);
  }
}

onMounted(() => {
  loadTaskForm();
});
</script>

<style scoped>
.task-approve-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
</style>
