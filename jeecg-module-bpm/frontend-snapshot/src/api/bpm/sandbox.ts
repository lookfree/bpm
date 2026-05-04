import { defHttp } from '/@/utils/http/axios'

export const startSandbox = (defId: string, formData: Record<string, any>) =>
  defHttp.post({ url: `/bpm/v1/sandbox/${defId}/start`, data: formData })

export const getSandboxRun = (runId: number) =>
  defHttp.get({ url: `/bpm/v1/sandbox/${runId}` })
