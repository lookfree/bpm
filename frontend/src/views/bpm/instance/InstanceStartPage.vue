<template>
  <div class="instance-start-page">
    <a-page-header :title="`发起流程：${definitionName}`" @back="handleCancel" />
    <a-spin :spinning="loading">
      <a-card style="margin-top: 16px;">
        <a-alert
          v-if="!formId"
          type="info"
          message="该流程未绑定申请表单，将以空表单数据发起"
          show-icon
          style="margin-bottom: 16px;"
        />
        <a-form layout="vertical">
          <a-form-item label="表单数据（JSON 格式）">
            <a-textarea
              v-model:value="formDataJson"
              placeholder='{"key": "value"}'
              :rows="10"
              :status="jsonError ? 'error' : ''"
            />
            <div v-if="jsonError" style="color: #ff4d4f; margin-top: 4px; font-size: 12px;">
              JSON 格式错误：{{ jsonError }}
            </div>
          </a-form-item>
        </a-form>
        <a-space style="margin-top: 8px;">
          <a-button type="primary" :loading="submitting" @click="handleSubmit">提交发起</a-button>
          <a-button @click="handleCancel">取消</a-button>
        </a-space>
      </a-card>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getDefinition } from '/@/api/bpm/definition';
import { listFormBindings } from '/@/api/bpm/form-binding';
import { startInstance } from '/@/api/bpm/instance';
import { message } from 'ant-design-vue';

const router = useRouter();
const route = useRoute();
const defId = route.params.defId as string;

const loading = ref(false);
const submitting = ref(false);
const definitionName = ref('');
const formId = ref('');
const formDataJson = ref('{}');
const jsonError = ref('');

watch(formDataJson, (val) => {
  try { JSON.parse(val); jsonError.value = ''; }
  catch (e: any) { jsonError.value = e.message; }
});

onMounted(async () => {
  loading.value = true;
  try {
    const def = await getDefinition(defId);
    definitionName.value = def.name || defId;

    const bindings: any[] = await listFormBindings(defId);
    const apply = bindings.find((b) => b.purpose === 'APPLY');
    if (apply) formId.value = apply.formId;
  } catch {
    message.error('加载流程信息失败');
  } finally {
    loading.value = false;
  }
});

async function handleSubmit() {
  if (jsonError.value) {
    message.error('请先修正 JSON 格式错误');
    return;
  }
  submitting.value = true;
  try {
    const formData = JSON.parse(formDataJson.value);
    await startInstance({ defId, formId: formId.value, formData });
    message.success('流程已发起');
    router.push('/bpm/todo');
  } catch (err: any) {
    message.error('发起失败：' + (err?.message || err));
  } finally {
    submitting.value = false;
  }
}

function handleCancel() {
  router.back();
}
</script>

<style scoped>
.instance-start-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}
</style>
