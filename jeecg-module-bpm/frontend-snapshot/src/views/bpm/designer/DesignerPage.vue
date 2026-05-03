<template>
  <div class="designer-page">
    <a-page-header :title="`流程设计器：${title || '新建草稿'}`">
      <template #extra>
        <a-space>
          <a-button @click="onSave" :loading="saving">保存草稿</a-button>
          <a-button type="primary" @click="onPublish" :disabled="!definitionId">发布</a-button>
        </a-space>
      </template>
    </a-page-header>
    <BpmnDesigner v-model="bpmnXml" />
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import BpmnDesigner from './BpmnDesigner.vue';
import { getDefinition, createDefinition, updateDefinition, publishDefinition }
  from '/@/api/bpm/definition';

const route = useRoute();
const router = useRouter();
const definitionId = ref<string | undefined>(route.query.id as string | undefined);
const title = ref<string>('');
const bpmnXml = ref<string>('');
const saving = ref(false);

onMounted(async () => {
  if (definitionId.value) {
    const vo = await getDefinition(definitionId.value);
    title.value = vo.name;
    bpmnXml.value = vo.bpmnXml || '';
  }
});

async function onSave() {
  saving.value = true;
  try {
    if (definitionId.value) {
      await updateDefinition(definitionId.value, { bpmnXml: bpmnXml.value });
      message.success('已保存');
    } else {
      const vo = await createDefinition({
        defKey: prompt('流程 key（英文，唯一）') || 'auto_' + Date.now(),
        name: prompt('流程名称') || '未命名',
        bpmnXml: bpmnXml.value,
      });
      definitionId.value = vo.id;
      title.value = vo.name;
      router.replace({ query: { id: vo.id } });
      message.success('草稿已创建');
    }
  } finally {
    saving.value = false;
  }
}

async function onPublish() {
  if (!definitionId.value) return;
  await publishDefinition(definitionId.value, '从设计器发布');
  message.success('已发布');
}
</script>
