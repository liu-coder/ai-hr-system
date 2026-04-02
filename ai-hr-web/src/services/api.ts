import { http } from './http'

export type LoginRequest = {
  tenantId: string
  username: string
  password: string
}

export async function login(payload: LoginRequest) {
  const { data } = await http.post('/v1/auth/token', payload)
  return data as { tokenType: string; accessToken: string }
}

export async function listAttendanceAnomalies(employeeId: string, start: string, end: string) {
  const { data } = await http.get('/v1/attendance/anomalies', { params: { employeeId, start, end } })
  return data as Array<{
    workDate?: string
    type?: string
    severity?: string
    snapshotId?: string
    evidence?: { summary?: string }
    ruleHit?: { message?: string }
  }>
}

export async function previewSalary(payload: {
  payPeriod: string
  employeeId: string
  input: Record<string, unknown>
  policyId?: string
}) {
  const { data } = await http.post('/v1/salary/preview/employee', payload)
  return data as { taskId: string }
}

export async function listSalaryLines(calcRunId: string) {
  const { data } = await http.get('/v1/salary/runs/lines', { params: { calcRunId } })
  return data as Array<{
    id?: string
    employeeId?: string
    itemCode?: string
    itemName?: string
    amountCents?: number
    currency?: string
    detail?: Record<string, unknown>
  }>
}

export async function askAi(message: string, sessionId?: string) {
  const { data } = await http.post('/v1/ai/chat', {
    sessionId: sessionId || null,
    message,
    toolArgs: {}
  })
  return data as {
    sessionId?: string
    llmResult?: string | null
    evidenceSummary?: string | null
    fallbackReason?: string | null
    llmAvailable?: boolean
    route?: { intentLabel?: string; route?: string }
    toolCall?: { toolName?: string; status?: string }
  }
}

export async function getGatewayHealth() {
  const { data } = await http.get('/actuator/health')
  return data as { status?: string }
}
