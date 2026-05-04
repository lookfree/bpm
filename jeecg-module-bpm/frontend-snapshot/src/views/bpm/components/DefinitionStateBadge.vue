<template>
  <a-tag :color="color">{{ label }}</a-tag>
</template>
<script setup lang="ts">
import { computed } from 'vue'
const props = defineProps<{ state: 'DRAFT' | 'TESTING' | 'PUBLISHED' | 'ARCHIVED' }>()
const COLORS = { DRAFT: 'default', TESTING: 'blue', PUBLISHED: 'green', ARCHIVED: 'gray' } as const
const LABELS = { DRAFT: '草稿', TESTING: '测试中', PUBLISHED: '已发布', ARCHIVED: '已归档' } as const
const color = computed(() => COLORS[props.state] ?? 'default')
const label = computed(() => LABELS[props.state] ?? props.state)
</script>
