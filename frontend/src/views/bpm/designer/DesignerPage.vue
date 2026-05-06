<template>
  <div class="designer-page">
    <a-page-header :title="`流程设计器：${title || '新建草稿'}`">
      <template #extra>
        <a-space>
          <a-tag v-if="defState" :color="stateColor">{{ stateLabel }}</a-tag>
          <a-button @click="onSave" :loading="saving"
                    :disabled="defState === 'PUBLISHED' || defState === 'ARCHIVED'">保存草稿</a-button>
          <a-button type="primary" @click="onPublish"
                    :disabled="!definitionId || defState === 'PUBLISHED' || defState === 'ARCHIVED'">
            {{ publishLabel }}
          </a-button>
        </a-space>
      </template>
    </a-page-header>
    <BpmnDesigner ref="designerRef" v-model="bpmnXml" />

    <a-modal v-model:visible="createModalOpen" title="新建流程草稿" @ok="onCreateConfirm"
             :confirm-loading="saving" ok-text="创建" cancel-text="取消">
      <a-form :model="createForm" :label-col="{ span: 6 }" :wrapper-col="{ span: 16 }">
        <a-form-item label="流程 Key" required>
          <a-input v-model:value="createForm.defKey" placeholder="英文，全局唯一" />
        </a-form-item>
        <a-form-item label="流程名称" required>
          <a-input v-model:value="createForm.name" placeholder="请输入流程名称" />
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, computed, onMounted } from 'vue';
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
const defState = ref<string>('');
const saving = ref(false);

const designerRef = ref<any>(null);
const createModalOpen = ref(false);
const createForm = reactive({ defKey: '', name: '' });

const STATE_MAP: Record<string, { label: string; color: string }> = {
  DRAFT:     { label: '草稿',   color: 'blue' },
  TESTING:   { label: '测试中', color: 'orange' },
  PUBLISHED: { label: '已发布', color: 'green' },
  ARCHIVED:  { label: '已归档', color: 'default' },
};

const stateLabel = computed(() => STATE_MAP[defState.value]?.label ?? defState.value);
const stateColor = computed(() => STATE_MAP[defState.value]?.color ?? 'default');
const publishLabel = computed(() => defState.value === 'DRAFT' ? '提交测试' : defState.value === 'TESTING' ? '正式发布' : '发布');

async function syncXml() {
  await designerRef.value?.getXml?.();
}

onMounted(async () => {
  if (definitionId.value) {
    const vo = await getDefinition(definitionId.value);
    title.value = vo.name;
    defState.value = vo.state;
    bpmnXml.value = vo.bpmnXml || '';
  }
});

async function onSave() {
  await syncXml();
  if (definitionId.value) {
    saving.value = true;
    try {
      await updateDefinition(definitionId.value, { bpmnXml: bpmnXml.value });
      message.success('已保存');
    } catch (err: any) {
      message.error('保存失败：' + (err?.message || err));
    } finally {
      saving.value = false;
    }
  } else {
    createForm.defKey = 'proc_' + Date.now();
    createForm.name = '';
    createModalOpen.value = true;
  }
}

async function onCreateConfirm() {
  if (!createForm.defKey.trim() || !createForm.name.trim()) {
    message.warning('Key 和名称不能为空');
    return;
  }
  await syncXml();
  saving.value = true;
  try {
    const vo = await createDefinition({
      defKey: createForm.defKey.trim(),
      name: createForm.name.trim(),
      bpmnXml: bpmnXml.value,
    });
    definitionId.value = vo.id;
    title.value = vo.name;
    defState.value = vo.state;
    createModalOpen.value = false;
    router.replace({ query: { id: vo.id } });
    message.success('草稿已创建');
  } catch (err: any) {
    message.error('创建失败：' + (err?.message || err));
  } finally {
    saving.value = false;
  }
}

async function onPublish() {
  if (!definitionId.value) return;
  try {
    await syncXml();
    if (defState.value === 'DRAFT' || defState.value === 'TESTING') {
      await updateDefinition(definitionId.value, { bpmnXml: bpmnXml.value });
    }
    const vo = await publishDefinition(definitionId.value, '从设计器发布');
    defState.value = vo?.state ?? defState.value;
    if (vo?.state === 'TESTING') {
      message.success('已提交测试，再次点击「正式发布」上线');
    } else {
      message.success('已正式发布');
    }
  } catch (err: any) {
    message.error('发布失败：' + (err?.message || err));
  }
}
</script>
