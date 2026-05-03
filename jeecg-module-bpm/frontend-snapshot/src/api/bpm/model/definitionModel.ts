export interface DefinitionVO {
  id: string;
  defKey: string;
  name: string;
  category?: string;
  version: number;
  state: 'DRAFT' | 'TESTING' | 'PUBLISHED' | 'ARCHIVED';
  formId?: string;
  actDefId?: string;
  description?: string;
  bpmnXml?: string;
  createBy?: string;
  createTime?: string;
  updateBy?: string;
  updateTime?: string;
}

export interface DefinitionCreateRequest {
  defKey: string;
  name: string;
  category?: string;
  description?: string;
  bpmnXml?: string;
  formId?: string;
}

export interface DefinitionUpdateRequest {
  name?: string;
  category?: string;
  description?: string;
  bpmnXml?: string;
  formId?: string;
}

export interface DefinitionQueryRequest {
  defKey?: string;
  name?: string;
  state?: string;
  category?: string;
  pageNo?: number;
  pageSize?: number;
}

export interface PageResult<T> {
  records: T[];
  total: number;
  size: number;
  current: number;
}

export interface VersionVO {
  id: string;
  defId: string;
  defKey: string;
  version: number;
  bpmnXml?: string;
  changeNote?: string;
  publishedBy?: string;
  publishedTime?: string;
}
