export function toErrorMessage(err: unknown): string {
  if (typeof err === 'string') return err
  if (err && typeof err === 'object') {
    const e = err as {
      response?: {
        data?: { message?: string; error?: string; code?: string }
        statusText?: string
      }
      message?: string
    }
    const data = e.response?.data
    if (data && typeof data.message === 'string' && data.message) return data.message
    if (data && typeof data.error === 'string' && data.error) return data.error
    if (data && typeof data.code === 'string' && data.code) return data.code
    return e.response?.statusText || e.message || '请求失败'
  }
  return '请求失败'
}
