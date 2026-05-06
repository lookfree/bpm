<template>
  <a-space v-if="record.state === 'RUNNING'">
    <a-popconfirm title="确认强制完成当前任务？" @confirm="intervene('FORCE_COMPLETE_TASK')">
      <a-button size="small" type="link">强制完成</a-button>
    </a-popconfirm>
    <a-popconfirm title="确认强制取消流程？" @confirm="intervene('FORCE_CANCEL')">
      <a-button size="small" type="link" danger>强制取消</a-button>
    </a-popconfirm>
  </a-space>
</template>

<script setup lang="ts">
import { message } from 'ant-design-vue';
import { defHttp } from '/@/utils/http/axios';

const props = defineProps<{ record: { id: string; state: string } }>();
const emit = defineEmits<{ (e: 'done'): void }>();

async function intervene(action: string) {
  try {
    await defHttp.post({
      url: `/bpm/v1/monitor/instances/${props.record.id}/intervene`,
      data: { action },
    });
    message.success('操作成功');
    emit('done');
  } catch {
    message.error('操作失败');
  }
}
</script>
