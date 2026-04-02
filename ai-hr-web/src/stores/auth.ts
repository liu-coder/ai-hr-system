import { defineStore } from 'pinia'

type LoginPayload = {
  tenantId: string
  username: string
  accessToken: string
}

export const STORAGE_KEY = 'aihr-auth'

function parseJwtExp(token: string): number | null {
  try {
    const payload = token.split('.')[1]
    if (!payload) return null
    const json = JSON.parse(atob(payload))
    return typeof json.exp === 'number' ? json.exp : null
  } catch {
    return null
  }
}

export const useAuthStore = defineStore('auth', {
  state: () => ({
    tenantId: '',
    username: '',
    accessToken: ''
  }),
  getters: {
    isAuthenticated: (state) => {
      if (!state.accessToken) return false
      const exp = parseJwtExp(state.accessToken)
      if (!exp) return true
      return exp * 1000 > Date.now()
    }
  },
  actions: {
    initFromStorage() {
      const raw = localStorage.getItem(STORAGE_KEY)
      if (!raw) return
      try {
        const parsed = JSON.parse(raw) as LoginPayload
        this.tenantId = parsed.tenantId
        this.username = parsed.username
        this.accessToken = parsed.accessToken
      } catch {
        localStorage.removeItem(STORAGE_KEY)
      }
    },
    setLogin(payload: LoginPayload) {
      this.tenantId = payload.tenantId
      this.username = payload.username
      this.accessToken = payload.accessToken
      localStorage.setItem(STORAGE_KEY, JSON.stringify(payload))
    },
    logout() {
      this.tenantId = ''
      this.username = ''
      this.accessToken = ''
      localStorage.removeItem(STORAGE_KEY)
    }
  }
})
