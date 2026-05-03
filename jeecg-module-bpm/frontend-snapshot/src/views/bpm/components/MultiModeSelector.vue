<template>
  <div class="multi-mode-selector">
    <div style="display:flex;gap:16px;flex-wrap:wrap">
      <label v-for="opt in options" :key="opt.value" style="cursor:pointer;display:flex;align-items:flex-start;gap:4px">
        <input
          type="radio"
          :value="opt.value"
          :checked="modelValue === opt.value"
          style="margin-top:3px"
          @change="$emit('update:modelValue', opt.value)"
        />
        <span>
          <b>{{ opt.label }}</b>
          <br /><small style="color:#888">{{ opt.desc }}</small>
        </span>
      </label>
    </div>
    <div v-if="completionCondition" style="margin-top:8px;font-size:12px;color:#555">
      完成条件预览：<code>{{ completionCondition }}</code>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed } from 'vue'

const props = defineProps<{ modelValue: string }>()
defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const options = [
  { value: '', label: '不启用', desc: '单人审批' },
  { value: 'SEQUENCE', label: '串行会签', desc: '依次审批，全部通过' },
  { value: 'PARALLEL', label: '并行会签', desc: '同时审批，全部通过' },
  { value: 'ANY', label: '或签', desc: '同时审批，一人通过' },
]

const conditionMap: Record<string, string> = {
  SEQUENCE: '${nrOfCompletedInstances == nrOfInstances}',
  PARALLEL: '${nrOfCompletedInstances/nrOfInstances >= 1.0}',
  ANY: '${nrOfCompletedInstances >= 1}',
}

const completionCondition = computed(() => conditionMap[props.modelValue] ?? '')
</script>
