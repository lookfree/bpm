import type { VxeColumnPropTypes } from 'vxe-table';

export const STATE_OPTIONS = [
  { label: '草稿', value: 'DRAFT' },
  { label: '已发布', value: 'PUBLISHED' },
  { label: '已归档', value: 'ARCHIVED' },
];

export const columns: VxeColumnPropTypes.Type[] = [
  { type: 'seq', width: 60 },
  { field: 'defKey', title: '流程 KEY', width: 160 },
  { field: 'name', title: '名称', minWidth: 180 },
  { field: 'category', title: '分类', width: 120 },
  { field: 'version', title: '版本', width: 80 },
  { field: 'state', title: '状态', width: 110, slots: { default: 'state' } },
  { field: 'updateTime', title: '更新时间', width: 170 },
  { title: '操作', width: 260, slots: { default: 'actions' } },
] as any;
