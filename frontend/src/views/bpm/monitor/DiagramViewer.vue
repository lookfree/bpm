<template>
  <div class="diagram-viewer">
    <a-spin :spinning="loading">
      <div v-if="!bpmnXml" class="empty-tip">暂无流程图数据</div>
      <div v-else ref="containerRef" class="bpmn-container" />
    </a-spin>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onBeforeUnmount, nextTick } from 'vue';
import { getInstanceDiagram } from '/@/api/bpm/monitor';

const props = defineProps<{ instMetaId: string }>();

const containerRef = ref<HTMLElement | null>(null);
const bpmnXml = ref('');
const currentNodeIds = ref<string[]>([]);
const loading = ref(false);
let viewer: any = null;

async function loadDiagram() {
  if (!props.instMetaId) return;
  loading.value = true;
  try {
    const res = await getInstanceDiagram(props.instMetaId);
    bpmnXml.value = res.bpmnXml || '';
    currentNodeIds.value = res.currentNodeIds || [];
    await nextTick();
    await renderDiagram();
  } catch {
    bpmnXml.value = '';
  } finally {
    loading.value = false;
  }
}

async function renderDiagram() {
  if (!containerRef.value || !bpmnXml.value) return;
  try {
    const BpmnViewer = (await import('bpmn-js')).default;
    if (viewer) {
      viewer.destroy();
    }
    viewer = new BpmnViewer({ container: containerRef.value });
    await viewer.importXML(bpmnXml.value);
    const canvas = viewer.get('canvas');
    canvas.zoom('fit-viewport');
    highlightCurrentNodes();
  } catch (e) {
    console.error('BPMN render error', e);
  }
}

function highlightCurrentNodes() {
  if (!viewer || !currentNodeIds.value.length) return;
  try {
    const canvas = viewer.get('canvas');
    currentNodeIds.value.forEach((id) => {
      canvas.addMarker(id, 'highlight-current');
    });
  } catch {
    // node may not be in diagram
  }
}

watch(() => props.instMetaId, loadDiagram, { immediate: true });

onBeforeUnmount(() => {
  if (viewer) viewer.destroy();
});
</script>

<style scoped>
.diagram-viewer {
  width: 100%;
  min-height: 360px;
}
.bpmn-container {
  width: 100%;
  height: 360px;
}
.empty-tip {
  text-align: center;
  color: #999;
  padding: 40px 0;
}
</style>

<style>
.highlight-current .djs-visual > :nth-child(1) {
  fill: #e6f7ff !important;
  stroke: #1890ff !important;
  stroke-width: 2px !important;
}
</style>
