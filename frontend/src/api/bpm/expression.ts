import { defHttp } from '/@/utils/http/axios'

export interface ExpressionTestRequest {
  expression: string
  formData?: Record<string, unknown>
}

export interface ExpressionTestResult {
  result: unknown
  error?: string
  durationMs: number
}

export function testExpression(req: ExpressionTestRequest): Promise<ExpressionTestResult> {
  return defHttp.post({ url: '/bpm/v1/expression/test', data: req })
}
