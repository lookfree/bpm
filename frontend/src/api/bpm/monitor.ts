import { defHttp } from '/@/utils/http/axios';

export interface MonitorInstanceQuery {
  defKey?: string;
  defVersion?: number;
  applyDeptId?: number;
  applyUserId?: number;
  state?: string;
  startTimeFrom?: string;
  startTimeTo?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface MonitorInstanceVO {
  id: string;
  actInstId: string;
  defKey: string;
  defName: string;
  defVersion: number;
  businessKey: string;
  applyUserId: number;
  applyUserName: string;
  applyDeptId: number;
  applyDeptName: string;
  state: string;
  startTime: string;
  endTime: string;
}

export interface InstanceDiagramVO {
  bpmnXml: string;
  currentNodeIds: string[];
}

export function listMonitorInstances(q: MonitorInstanceQuery) {
  return defHttp.get<{ total: number; records: MonitorInstanceVO[]; pageNo: number; pageSize: number }>({
    url: '/bpm/v1/monitor/instances',
    params: q,
  });
}

export function getInstanceDiagram(id: string) {
  return defHttp.get<InstanceDiagramVO>({ url: `/bpm/v1/monitor/instances/${id}/diagram` });
}

export function getStats(params?: Record<string, any>) {
  return defHttp.get({ url: '/bpm/v1/monitor/stats', params });
}
