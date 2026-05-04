<template>
  <a-modal :visible="visible" :title="title" @ok="handleOk" @cancel="$emit('update:visible', false)">
    <p>{{ message }}</p>
  </a-modal>
</template>
<script setup lang="ts">
const props = defineProps<{ visible: boolean; title: string; message: string }>()
const emit = defineEmits<{ (e: 'update:visible', v: boolean): void; (e: 'confirm'): void }>()
function handleOk() {
  emit('confirm')
  emit('update:visible', false)
}
</script>
