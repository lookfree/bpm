<template>
  <div class="bpm-definition-list">
    <a-card title="流程定义">
      <template #extra>
        <a-space>
          <a-input v-model:value="query.name" placeholder="名称" allowClear style="width: 180px" />
          <a-select v-model:value="query.state" :options="STATE_OPTIONS"
                    placeholder="状态" allowClear style="width: 130px" />
          <a-button type="primary" @click="reload">查询</a-button>
          <a-button @click="onCreate">新建草稿</a-button>
        </a-space>
      </template>
      <vxe-table :data="rows" :loading="loading" border stripe height="auto">
        <vxe-column v-for="c in columns" :key="c.field || c.title" v-bind="c">
          <template #state="{ row }">
            <a-tag :color="row.state === 'PUBLISHED' ? 'green' : (row.state === 'DRAFT' ? 'blue' : 'default')">
              {{ row.state }}
            </a-tag>
          </template>
          <template #actions="{ row }">
            <a-button type="link" @click="openDesigner(row)">设计</a-button>
            <a-button type="link" @click="openVersions(row)">版本</a-button>
            <a-popconfirm title="确认删除？" @confirm="onDelete(row)">
              <a-button type="link" danger :disabled="row.state==='PUBLISHED'">删除</a-button>
            </a-popconfirm>
          </template>
        </vxe-column>
      </vxe-table>
      <a-pagination v-model:current="page.current" v-model:pageSize="page.size" :total="page.total"
                    show-size-changer @change="reload" style="margin-top: 12px; text-align: right;" />
    </a-card>

    <VersionsModal v-model:open="versionsOpen" :definition-id="versionsId" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { columns, STATE_OPTIONS } from './DefinitionList.data';
import { listDefinitions, deleteDefinition } from '/@/api/bpm/definition';
import type { DefinitionVO, DefinitionQueryRequest } from '/@/api/bpm/model/definitionModel';
import VersionsModal from './components/VersionsModal.vue';

const router = useRouter();

const query = reactive<DefinitionQueryRequest>({ name: undefined, state: undefined });
const page = reactive({ current: 1, size: 20, total: 0 });
const rows = ref<DefinitionVO[]>([]);
const loading = ref(false);

const versionsOpen = ref(false);
const versionsId = ref<string>('');

async function reload() {
  loading.value = true;
  try {
    const r = await listDefinitions({
      ...query,
      pageNo: page.current,
      pageSize: page.size,
    });
    rows.value = r.records;
    page.total = r.total;
  } finally {
    loading.value = false;
  }
}

function onCreate() {
  router.push({ name: 'BpmDesigner' });
}

function openDesigner(row: DefinitionVO) {
  router.push({ name: 'BpmDesigner', query: { id: row.id } });
}

function openVersions(row: DefinitionVO) {
  versionsId.value = row.id;
  versionsOpen.value = true;
}

async function onDelete(row: DefinitionVO) {
  await deleteDefinition(row.id);
  message.success('已删除');
  await reload();
}

onMounted(reload);
</script>
