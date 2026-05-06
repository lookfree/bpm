<template>
  <div>
    <div style="margin-bottom:8px">
      <DefinitionStateBadge v-if="run" :state="run.result === 'RUNNING' ? 'TESTING' : run.result === 'PASS' ? 'PUBLISHED' : 'ARCHIVED'" />
      <span style="margin-left:8px;font-size:12px;color:#888" v-if="run">{{ run.result }}</span>
    </div>
    <pre style="background:#f5f5f5;padding:12px;border-radius:4px;max-height:400px;overflow:auto;font-size:12px">{{ run?.log || '(no log)' }}</pre>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted, onUnmounted } from 'vue'
import { getSandboxRun } from '/@/api/bpm/sandbox'
import DefinitionStateBadge from '../components/DefinitionStateBadge.vue'

const props = defineProps<{ runId: number }>()
const run = ref<any>(null)
let timer: ReturnType<typeof setInterval> | null = null

async function poll() {
  run.value = await getSandboxRun(props.runId)
  if (run.value?.result !== 'RUNNING' && timer) {
    clearInterval(timer)
    timer = null
  }
}

onMounted(() => {
  poll()
  timer = setInterval(poll, 2000)
})
onUnmounted(() => { if (timer) clearInterval(timer) })
</script>
