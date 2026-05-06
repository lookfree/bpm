import { defHttp } from '/@/utils/http/axios'

export const publishDefinition = (id: string, changeNote?: string) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/publish`, params: { changeNote } })

export const archiveDefinition = (id: string) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/archive` })

export const rollbackDefinition = (id: string, targetVersion: number) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/rollback`, params: { targetVersion } })

export const listVersions = (id: string) =>
  defHttp.get({ url: `/bpm/v1/definition/${id}/versions` })

export const cloneAsSandbox = (id: string) =>
  defHttp.post({ url: `/bpm/v1/definition/${id}/clone-as-sandbox` })
