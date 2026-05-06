<template>
  <div class="bpmn-designer">
    <div ref="canvasRef" class="bpmn-canvas"></div>
    <div ref="propertiesRef" class="bpmn-properties"></div>
  </div>
</template>

<script lang="ts" setup>
import { ref, onMounted, onBeforeUnmount, watch } from 'vue';
import BpmnModeler from 'bpmn-js/lib/Modeler';
import 'bpmn-js/dist/assets/diagram-js.css';
import 'bpmn-js/dist/assets/bpmn-font/css/bpmn.css';

interface Props {
  modelValue?: string;
  readonly?: boolean;
}
const props = withDefaults(defineProps<Props>(), {
  modelValue: '',
  readonly: false,
});

const emit = defineEmits<{
  (e: 'update:modelValue', xml: string): void;
  (e: 'ready'): void;
  (e: 'error', err: Error): void;
}>();

const canvasRef = ref<HTMLElement | null>(null);
const propertiesRef = ref<HTMLElement | null>(null);
let modeler: any = null;

const EMPTY_XML = `<?xml version="1.0" encoding="UTF-8"?>
<definitions xmlns="http://www.omg.org/spec/BPMN/20100524/MODEL"
             xmlns:flowable="http://flowable.org/bpmn"
             xmlns:bpmndi="http://www.omg.org/spec/BPMN/20100524/DI"
             xmlns:dc="http://www.omg.org/spec/DD/20100524/DC"
             targetNamespace="http://iimt.com/bpm">
  <process id="Process_1" isExecutable="true">
    <startEvent id="StartEvent_1"/>
  </process>
  <bpmndi:BPMNDiagram id="d_1">
    <bpmndi:BPMNPlane id="p_1" bpmnElement="Process_1">
      <bpmndi:BPMNShape id="sh_se" bpmnElement="StartEvent_1">
        <dc:Bounds x="156" y="81" width="36" height="36"/>
      </bpmndi:BPMNShape>
    </bpmndi:BPMNPlane>
  </bpmndi:BPMNDiagram>
</definitions>`;

async function importXml(xml: string) {
  if (!modeler) return;
  try {
    await modeler.importXML(xml || EMPTY_XML);
    emit('ready');
  } catch (e: any) {
    emit('error', e);
  }
}

async function emitXml() {
  if (!modeler) return;
  const { xml } = await modeler.saveXML({ format: true });
  emit('update:modelValue', xml || '');
}

defineExpose({ getXml: emitXml, importXml });

onMounted(() => {
  modeler = new BpmnModeler({ container: canvasRef.value!, propertiesPanel: { parent: propertiesRef.value } });
  modeler.on('commandStack.changed', emitXml);
  importXml(props.modelValue);
});

onBeforeUnmount(() => {
  if (modeler) {
    modeler.destroy();
    modeler = null;
  }
});

watch(() => props.modelValue, (v) => {
  // 来自外部赋值（例如 detail load）
  if (modeler && v !== undefined) importXml(v);
});
</script>

<style scoped>
.bpmn-designer { display: flex; height: calc(100vh - 200px); width: 100%; }
.bpmn-canvas { flex: 1; border-right: 1px solid #f0f0f0; min-height: 500px; }
.bpmn-properties { width: 320px; padding: 8px; overflow: auto; }
</style>
