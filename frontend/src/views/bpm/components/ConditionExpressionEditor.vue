<template>
  <div class="condition-expr-editor">
    <textarea
      ref="taRef"
      :value="modelValue"
      rows="4"
      style="width:100%;font-family:monospace;padding:4px"
      @input="$emit('update:modelValue', ($event.target as HTMLTextAreaElement).value)"
    />
    <div style="margin:4px 0;display:flex;flex-wrap:wrap;gap:4px">
      <span
        v-for="f in formFields"
        :key="f"
        style="cursor:pointer;background:#e6f4ff;border:1px solid #91caff;border-radius:3px;padding:1px 6px;font-size:12px"
        @click="insertField(f)"
      >form.{{ f }}</span>
    </div>
    <div style="display:flex;align-items:center;gap:8px">
      <button :disabled="testing" @click="runTest" style="padding:2px 12px">
        {{ testing ? 'Testing…' : 'Test' }}
      </button>
      <span v-if="testResult !== null" style="font-size:12px">
        Result: <b>{{ testResult }}</b>
        <span style="color:#888;margin-left:8px">({{ testDuration }}ms)</span>
      </span>
      <span v-if="testError" style="color:red;font-size:12px">{{ testError }}</span>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { testExpression } from '/@/api/bpm/expression'

const props = defineProps<{ modelValue: string; formFields: string[] }>()
defineEmits<{ (e: 'update:modelValue', v: string): void }>()

const taRef = ref<HTMLTextAreaElement | null>(null)
const testing = ref(false)
const testResult = ref<unknown>(null)
const testError = ref('')
const testDuration = ref(0)

function insertField(name: string) {
  const ta = taRef.value
  if (!ta) return
  const start = ta.selectionStart ?? ta.value.length
  const end = ta.selectionEnd ?? ta.value.length
  const insert = `form.${name}`
  const next = ta.value.slice(0, start) + insert + ta.value.slice(end)
  ta.value = next
  ta.dispatchEvent(new Event('input'))
  ta.setSelectionRange(start + insert.length, start + insert.length)
  ta.focus()
}

async function runTest() {
  testing.value = true
  testError.value = ''
  testResult.value = null
  try {
    const res = await testExpression({ expression: props.modelValue, formData: {} })
    testResult.value = res.result
    testDuration.value = res.durationMs
    if (res.error) testError.value = res.error
  } catch (e: any) {
    testError.value = e?.message ?? 'unknown error'
  } finally {
    testing.value = false
  }
}
</script>
