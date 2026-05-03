<template>
  <div class="instance-start-page">
    <a-spin :spinning="loading">
      <div class="header">
        <h2>{{ definitionName }}</h2>
      </div>

      <a-form layout="vertical">
        <a-form-item label="表单数据（JSON格式）">
          <a-textarea
            v-model:value="formDataJson"
            placeholder="请输入JSON格式的表单数据"
            :rows="10"
          />
        </a-form-item>
      </a-form>

      <div class="action-buttons">
        <a-button type="primary" @click="handleSubmit">提交</a-button>
        <a-button @click="handleCancel">取消</a-button>
      </div>
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { getDefinition } from '/@/api/bpm/definition';
import { listFormBindings } from '/@/api/bpm/form-binding';
import { startInstance } from '/@/api/bpm/instance';
import { message } from 'ant-design-vue';

const router = useRouter();
const route = useRoute();

const defId = route.params.defId as string;
const loading = ref<boolean>(false);
const definitionName = ref<string>('');
const formId = ref<string>('');
const formDataJson = ref<string>('{}');

async function loadDefinition() {
  loading.value = true;
  try {
    const res = await getDefinition(defId);
    definitionName.value = res.name || defId;

    // Try to find the APPLY form binding
    const bindingRes = await listFormBindings(defId);
    const applyBinding = (bindingRes.data || []).find((b: any) => b.purpose === 'APPLY');
    if (applyBinding) {
      formId.value = applyBinding.formId;
    }
  } catch (err) {
    message.error('加载流程定义失败');
    console.error(err);
  } finally {
    loading.value = false;
  }
}

async function handleSubmit() {
  if (!formId.value) {
    message.warning('未找到关联的表单');
    return;
  }

  try {
    const formData = JSON.parse(formDataJson.value);
    await startInstance({
      defId,
      formId: formId.value,
      formData,
    });
    message.success('流程启动成功');
    router.push('/bpm/todo');
  } catch (err: any) {
    if (err instanceof SyntaxError) {
      message.error('表单数据JSON格式错误');
    } else {
      message.error('流程启动失败');
    }
    console.error(err);
  }
}

function handleCancel() {
  router.back();
}

onMounted(() => {
  loadDefinition();
});
</script>

<style scoped>
.instance-start-page {
  padding: 24px;
  max-width: 800px;
  margin: 0 auto;
}

.header {
  margin-bottom: 24px;
}

.header h2 {
  margin: 0;
}

.action-buttons {
  display: flex;
  gap: 8px;
  margin-top: 16px;
}
</style>
