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

      <a-table
        :dataSource="rows"
        :columns="antColumns"
        :loading="loading"
        :pagination="{ current: page.current, pageSize: page.size, total: page.total, showSizeChanger: true }"
        @change="onTableChange"
        rowKey="id"
        bordered
        size="middle"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'state'">
            <a-tag :color="stateColor(record.state)">{{ stateLabel(record.state) }}</a-tag>
          </template>
          <template v-else-if="column.key === 'actions'">
            <a-space>
              <a-button
                v-if="record.state === 'PUBLISHED'"
                type="link" size="small"
                @click="openStart(record)"
              >发起</a-button>
              <a-button type="link" size="small" @click="openDesigner(record)">设计</a-button>
              <a-button type="link" size="small" @click="openVersions(record)">版本</a-button>
              <a-popconfirm title="确认删除？" @confirm="onDelete(record)">
                <a-button type="link" size="small" danger :disabled="record.state === 'PUBLISHED'">删除</a-button>
              </a-popconfirm>
            </a-space>
          </template>
        </template>
      </a-table>
    </a-card>

    <VersionsModal v-model:open="versionsOpen" :definition-id="versionsId" />
  </div>
</template>

<script lang="ts" setup>
import { ref, reactive, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { message } from 'ant-design-vue';
import { listDefinitions, deleteDefinition } from '/@/api/bpm/definition';
import type { DefinitionVO, DefinitionQueryRequest } from '/@/api/bpm/model/definitionModel';
import VersionsModal from './components/VersionsModal.vue';

const router = useRouter();

const STATE_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '测试中', value: 'TESTING' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' },
];

const STATE_MAP: Record<string, { label: string; color: string }> = {
  DRAFT:     { label: '草稿',   color: 'blue' },
  TESTING:   { label: '测试中', color: 'orange' },
  PUBLISHED: { label: '已发布', color: 'green' },
  ARCHIVED:  { label: '已归档', color: 'default' },
};

function stateLabel(s: string) { return STATE_MAP[s]?.label ?? s; }
function stateColor(s: string) { return STATE_MAP[s]?.color ?? 'default'; }

const antColumns = [
  { title: '序号', key: 'seq', customRender: ({ index }: any) => index + 1, width: 60 },
  { title: '流程 KEY', dataIndex: 'defKey', key: 'defKey', width: 200 },
  { title: '名称', dataIndex: 'name', key: 'name' },
  { title: '分类', dataIndex: 'category', key: 'category', width: 120 },
  { title: '版本', dataIndex: 'version', key: 'version', width: 80 },
  { title: '状态', key: 'state', width: 100 },
  { title: '更新时间', dataIndex: 'updateTime', key: 'updateTime', width: 170 },
  { title: '操作', key: 'actions', width: 200 },
];

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
  } catch (err: any) {
    message.error('加载失败：' + (err?.message || err));
  } finally {
    loading.value = false;
  }
}

function onTableChange(pagination: any) {
  page.current = pagination.current;
  page.size = pagination.pageSize;
  reload();
}

function onCreate() {
  router.push({ name: 'BpmDesigner' });
}

function openDesigner(row: DefinitionVO) {
  router.push({ name: 'BpmDesigner', query: { id: row.id } });
}

function openStart(row: DefinitionVO) {
  router.push({ name: 'BpmInstanceStart', params: { defId: row.id } });
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
