<template>
  <div style="padding:24px">
    <h2>沙箱测试</h2>
    <a-form layout="inline" style="margin-bottom:16px">
      <a-form-item label="选择沙箱定义">
        <a-select v-model:value="selectedDefId" style="width:300px" placeholder="请选择" @change="onDefChange">
          <a-select-option v-for="d in sandboxDefs" :key="d.id" :value="d.id">
            {{ d.name }}
          </a-select-option>
        </a-select>
      </a-form-item>
      <a-form-item>
        <a-button type="primary" :disabled="!selectedDefId || running" @click="runSandbox">
          {{ running ? '运行中…' : '运行' }}
        </a-button>
      </a-form-item>
    </a-form>

    <div v-if="runId">
      <SandboxRunLog :run-id="runId" />
    </div>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { startSandbox } from '/@/api/bpm/sandbox'
import { defHttp } from '/@/utils/http/axios'
import SandboxRunLog from './SandboxRunLog.vue'

const sandboxDefs = ref<any[]>([])
const selectedDefId = ref<string>('')
const running = ref(false)
const runId = ref<number | null>(null)

async function loadSandboxDefs() {
  const result = await defHttp.get({ url: '/bpm/v1/definition', params: { includeSandbox: true } })
  sandboxDefs.value = (result.records || result).filter((d: any) => d.category === 'SANDBOX')
}

function onDefChange() { runId.value = null }

async function runSandbox() {
  if (!selectedDefId.value) return
  running.value = true
  try {
    const res = await startSandbox(selectedDefId.value, {})
    runId.value = res.runId
  } finally {
    running.value = false
  }
}

onMounted(loadSandboxDefs)
</script>
