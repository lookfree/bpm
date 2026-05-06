import { defHttp } from '/@/utils/http/axios';

export function createFormBinding(p: { defId: string; formId: string; purpose: 'APPLY' | 'APPROVE' | 'ARCHIVE' }) {
  return defHttp.post({ url: '/bpm/v1/form-binding', data: p });
}

export function listFormBindings(defId: string) {
  return defHttp.get({ url: '/bpm/v1/form-binding', params: { defId } });
}

export function deleteFormBinding(id: string) {
  return defHttp.delete({ url: `/bpm/v1/form-binding/${id}` });
}
