<template>
  <div style="padding: 16px">
    <a-form layout="inline" style="margin-bottom: 16px" @finish="handleSearch">
      <a-form-item label="流程Key">
        <a-input v-model:value="query.defKey" placeholder="流程Key" allow-clear style="width: 160px" />
      </a-form-item>
      <a-form-item label="状态">
        <a-select v-model:value="query.state" placeholder="全部" allow-clear style="width: 120px">
          <a-select-option value="RUNNING">进行中</a-select-option>
          <a-select-option value="COMPLETED">已完成</a-select-option>
          <a-select-option value="CANCELLED">已取消</a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" html-type="submit">查询</a-button>
        <a-button style="margin-left: 8px" @click="handleReset">重置</a-button>
      </a-form-item>
    </a-form>

    <a-table
      :columns="columns"
      :data-source="records"
      :loading="loading"
      :pagination="pagination"
      row-key="id"
      @change="handleTableChange"
    >
      <template #bodyCell="{ column, record }">
        <template v-if="column.key === 'state'">
          <a-tag :color="stateColor(record.state)">{{ record.state }}</a-tag>
        </template>
        <template v-if="column.key === 'action'">
          <a-space>
            <a-button type="link" size="small" @click="openDetail(record)">详情</a-button>
            <InterveneActions :record="record" @done="loadData" />
          </a-space>
        </template>
      </template>
    </a-table>

    <InstanceDetailDrawer :open="drawerOpen" :record="selectedRecord" @close="drawerOpen = false" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, onMounted } from 'vue';
import { listMonitorInstances, type MonitorInstanceVO, type MonitorInstanceQuery } from '/@/api/bpm/monitor';
import InstanceDetailDrawer from './InstanceDetailDrawer.vue';
import InterveneActions from './InterveneActions.vue';

const query = reactive<MonitorInstanceQuery>({ pageNo: 1, pageSize: 20 });
const records = ref<MonitorInstanceVO[]>([]);
const total = ref(0);
const loading = ref(false);

const drawerOpen = ref(false);
const selectedRecord = ref<MonitorInstanceVO | null>(null);

const columns = [
  { title: '流程Key', dataIndex: 'defKey', key: 'defKey', width: 140 },
  { title: '版本', dataIndex: 'defVersion', key: 'defVersion', width: 60 },
  { title: '申请人', dataIndex: 'applyUserName', key: 'applyUserName', width: 100 },
  { title: '申请部门', dataIndex: 'applyDeptName', key: 'applyDeptName', width: 120 },
  { title: '状态', key: 'state', width: 90 },
  { title: '开始时间', dataIndex: 'startTime', key: 'startTime', width: 160 },
  { title: '结束时间', dataIndex: 'endTime', key: 'endTime', width: 160 },
  { title: '操作', key: 'action', width: 160 },
];

const pagination = ref({ current: 1, pageSize: 20, total: 0, showSizeChanger: true });

function stateColor(state: string) {
  const map: Record<string, string> = {
    RUNNING: 'processing',
    COMPLETED: 'success',
    CANCELLED: 'default',
    REJECTED: 'error',
  };
  return map[state] ?? 'default';
}

async function loadData() {
  loading.value = true;
  try {
    const res = await listMonitorInstances(query);
    records.value = res.records || [];
    total.value = res.total || 0;
    pagination.value = { ...pagination.value, total: total.value };
  } catch {
    records.value = [];
  } finally {
    loading.value = false;
  }
}

function handleSearch() {
  query.pageNo = 1;
  pagination.value.current = 1;
  loadData();
}

function handleReset() {
  query.defKey = undefined;
  query.state = undefined;
  query.pageNo = 1;
  pagination.value.current = 1;
  loadData();
}

function handleTableChange(pag: any) {
  query.pageNo = pag.current;
  query.pageSize = pag.pageSize;
  pagination.value.current = pag.current;
  pagination.value.pageSize = pag.pageSize;
  loadData();
}

function openDetail(record: MonitorInstanceVO) {
  selectedRecord.value = record;
  drawerOpen.value = true;
}

onMounted(loadData);
</script>
