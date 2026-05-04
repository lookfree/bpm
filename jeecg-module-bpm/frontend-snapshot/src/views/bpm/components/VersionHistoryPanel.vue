<template>
  <div>
    <a-spin :spinning="loading">
      <a-table :data-source="versions" :columns="columns" row-key="id" size="small">
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'action'">
            <a-button size="small" @click="viewBpmn(record)">查看</a-button>
            <a-button size="small" danger style="margin-left:8px" @click="confirmRollback(record)">回滚</a-button>
          </template>
          <template v-if="column.key === 'version'">
            <b>v{{ record.version }}</b>
            <a-tag v-if="record.version === currentVersion" color="blue" style="margin-left:4px">当前</a-tag>
          </template>
        </template>
      </a-table>
    </a-spin>
    <ConfirmDialog
      v-model:visible="rollbackVisible"
      title="确认回滚"
      :message="`确认将定义回滚到 v${rollbackTarget?.version}？这将覆盖当前 BPMN 并将状态重置为 DRAFT。`"
      @confirm="doRollback"
    />
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { listVersions, rollbackDefinition } from '/@/api/bpm/lifecycle'
import ConfirmDialog from './ConfirmDialog.vue'

const props = defineProps<{ defId: string; currentVersion?: number }>()
const emit = defineEmits<{ (e: 'refresh'): void }>()

const loading = ref(false)
const versions = ref<any[]>([])
const rollbackVisible = ref(false)
const rollbackTarget = ref<any>(null)

const columns = [
  { title: '版本', key: 'version', dataIndex: 'version' },
  { title: 'change note', dataIndex: 'changeNote' },
  { title: '发布人', dataIndex: 'publishedBy' },
  { title: '发布时间', dataIndex: 'publishedTime' },
  { title: '操作', key: 'action' },
]

async function loadVersions() {
  loading.value = true
  try { versions.value = await listVersions(props.defId) }
  finally { loading.value = false }
}

function viewBpmn(record: any) {
  window.alert('BPMN XML:\n' + record.bpmnXml)
}

function confirmRollback(record: any) {
  rollbackTarget.value = record
  rollbackVisible.value = true
}

async function doRollback() {
  if (!rollbackTarget.value) return
  await rollbackDefinition(props.defId, rollbackTarget.value.version)
  emit('refresh')
}

onMounted(loadVersions)
</script>
