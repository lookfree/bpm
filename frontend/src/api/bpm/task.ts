import { defHttp } from '/@/utils/http/axios';

export function listTodo(params?: { defKey?: string; page?: number; size?: number }) {
  return defHttp.get({ url: '/bpm/v1/task/todo', params });
}

export function listDone(params?: { defKey?: string; page?: number; size?: number }) {
  return defHttp.get({ url: '/bpm/v1/task/done', params });
}

export function completeTask(id: string, p: { action: 'APPROVE' | 'REJECT'; comment?: string; formData?: Record<string, any> }) {
  return defHttp.post({ url: `/bpm/v1/task/${id}/complete`, data: p });
}

export function getTaskForm(id: string) {
  return defHttp.get({ url: `/bpm/v1/task/${id}/form` });
}
