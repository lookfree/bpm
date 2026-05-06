import { defHttp } from '/@/utils/http/axios';
import type {
  DefinitionVO,
  DefinitionCreateRequest,
  DefinitionUpdateRequest,
  DefinitionQueryRequest,
  PageResult,
  VersionVO,
} from './model/definitionModel';

const PREFIX = '/bpm/v1/definition';

export const listDefinitions = (q: DefinitionQueryRequest) =>
  defHttp.get<PageResult<DefinitionVO>>({ url: PREFIX, params: q });

export const getDefinition = (id: string) =>
  defHttp.get<DefinitionVO>({ url: `${PREFIX}/${id}` });

export const createDefinition = (req: DefinitionCreateRequest) =>
  defHttp.post<DefinitionVO>({ url: PREFIX, data: req });

export const updateDefinition = (id: string, req: DefinitionUpdateRequest) =>
  defHttp.put<DefinitionVO>({ url: `${PREFIX}/${id}`, data: req });

export const deleteDefinition = (id: string) =>
  defHttp.delete<void>({ url: `${PREFIX}/${id}` });

export const publishDefinition = (id: string, changeNote?: string) =>
  defHttp.post<DefinitionVO>({ url: `${PREFIX}/${id}/publish`, params: { changeNote } });

export const listVersions = (id: string) =>
  defHttp.get<VersionVO[]>({ url: `${PREFIX}/${id}/versions` });
