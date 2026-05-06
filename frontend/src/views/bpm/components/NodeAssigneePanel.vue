<template>
  <div class="node-assignee-panel">
    <div class="form-group">
      <label>分配方式</label>
      <a-select
        v-model:value="assignType"
        placeholder="选择分配方式"
        @change="handleTypeChange"
      >
        <a-select-option value="USER">指定用户</a-select-option>
        <a-select-option value="ROLE">指定角色</a-select-option>
        <a-select-option value="DEPT_LEADER">部门负责人</a-select-option>
        <a-select-option value="UPPER_DEPT">上级部门负责人</a-select-option>
        <a-select-option value="FORM_FIELD">表单字段</a-select-option>
        <a-select-option value="SCRIPT">脚本表达式</a-select-option>
      </a-select>
    </div>

    <div v-if="assignType === 'USER'" class="form-group">
      <label>选择用户</label>
      <a-select
        v-model:value="userIds"
        mode="multiple"
        placeholder="选择用户"
        @change="emitUpdate"
      />
    </div>

    <div v-if="assignType === 'ROLE'" class="form-group">
      <label>选择角色</label>
      <a-select
        v-model:value="roleCodes"
        mode="multiple"
        placeholder="选择角色"
        @change="emitUpdate"
      />
    </div>

    <div v-if="assignType === 'DEPT_LEADER' || assignType === 'UPPER_DEPT'" class="form-group">
      <p>运行时从申请人部门自动获取</p>
    </div>

    <div v-if="assignType === 'FORM_FIELD'" class="form-group">
      <label>表单字段名</label>
      <a-input
        v-model:value="fieldName"
        placeholder="表单字段名"
        @change="emitUpdate"
      />
    </div>

    <div v-if="assignType === 'SCRIPT'" class="form-group">
      <label>Aviator 表达式</label>
      <a-textarea
        v-model:value="scriptExpr"
        placeholder="Aviator 表达式 (P3 启用)"
        :disabled="true"
      />
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, watch, onMounted } from 'vue';

const props = defineProps<{
  modelValue: string;
}>();

const emit = defineEmits<{
  'update:modelValue': [value: string];
}>();

const assignType = ref<'USER' | 'ROLE' | 'DEPT_LEADER' | 'UPPER_DEPT' | 'FORM_FIELD' | 'SCRIPT'>('USER');
const userIds = ref<string[]>([]);
const roleCodes = ref<string[]>([]);
const fieldName = ref<string>('');
const scriptExpr = ref<string>('');

function parseModelValue(value: string) {
  if (!value) {
    return;
  }
  try {
    const parsed = JSON.parse(value);
    assignType.value = parsed.type;
    const payload = parsed.payload || {};

    switch (parsed.type) {
      case 'USER':
        userIds.value = payload.userIds || [];
        break;
      case 'ROLE':
        roleCodes.value = payload.roleCode ? [payload.roleCode] : [];
        break;
      case 'FORM_FIELD':
        fieldName.value = payload.fieldName || '';
        break;
      case 'SCRIPT':
        scriptExpr.value = payload.expr || '';
        break;
      default:
        break;
    }
  } catch (e) {
    console.error('Failed to parse modelValue:', e);
  }
}

function emitUpdate() {
  let payload: any = {};

  switch (assignType.value) {
    case 'USER':
      payload = { userIds: userIds.value };
      break;
    case 'ROLE':
      payload = { roleCode: roleCodes.value[0] || '' };
      break;
    case 'DEPT_LEADER':
    case 'UPPER_DEPT':
      payload = {};
      break;
    case 'FORM_FIELD':
      payload = { fieldName: fieldName.value };
      break;
    case 'SCRIPT':
      payload = { expr: scriptExpr.value };
      break;
  }

  const result = {
    type: assignType.value,
    payload,
  };

  emit('update:modelValue', JSON.stringify(result));
}

function handleTypeChange() {
  userIds.value = [];
  roleCodes.value = [];
  fieldName.value = '';
  scriptExpr.value = '';
  emitUpdate();
}

onMounted(() => {
  parseModelValue(props.modelValue);
});

watch(
  () => props.modelValue,
  (newVal) => {
    parseModelValue(newVal);
  }
);
</script>

<style scoped>
.node-assignee-panel {
  padding: 16px;
}

.form-group {
  margin-bottom: 16px;
}

.form-group label {
  display: block;
  margin-bottom: 8px;
  font-weight: 500;
}

.form-group p {
  margin: 0;
  color: #666;
}
</style>
