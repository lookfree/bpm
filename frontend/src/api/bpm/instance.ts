import { defHttp } from '/@/utils/http/axios';

export function startInstance(p: { defId: string; formId: string; formData: Record<string, any> }) {
  return defHttp.post({ url: '/bpm/v1/instance', data: p });
}

export function getInstance(id: string) {
  return defHttp.get({ url: `/bpm/v1/instance/${id}` });
}
