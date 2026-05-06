<template>
  <a-modal :visible="open" title="历史版本" :footer="null" width="600px"
           @cancel="emit('update:open', false)">
    <a-list :data-source="rows" item-layout="horizontal">
      <template #renderItem="{ item }">
        <a-list-item>
          <a-list-item-meta :description="`v${item.version} · ${item.publishedTime} · ${item.publishedBy}`">
            <template #title>{{ item.changeNote || '(无说明)' }}</template>
          </a-list-item-meta>
        </a-list-item>
      </template>
    </a-list>
  </a-modal>
</template>

<script lang="ts" setup>
import { ref, watch } from 'vue';
import { listVersions } from '/@/api/bpm/definition';
import type { VersionVO } from '/@/api/bpm/model/definitionModel';

const props = defineProps<{ open: boolean; definitionId: string }>();
const emit = defineEmits<{ (e: 'update:open', v: boolean): void }>();

const rows = ref<VersionVO[]>([]);

watch(() => [props.open, props.definitionId], async ([open, id]) => {
  if (open && id) rows.value = await listVersions(id as string);
});
</script>
