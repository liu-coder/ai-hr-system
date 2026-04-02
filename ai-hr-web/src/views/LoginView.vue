<template>
  <div class="login-wrap">
    <div class="card login-card">
      <h2>登录 AI-HR</h2>
      <label>
        租户ID
        <input v-model="tenantId" class="input" placeholder="演示租户：t-demo" />
      </label>
      <label>
        用户名
        <input v-model="username" class="input" />
      </label>
      <label>
        密码
        <input v-model="password" class="input" type="password" />
      </label>
      <button class="btn" :disabled="loading" @click="submit">{{ loading ? '登录中...' : '登录' }}</button>
      <p v-if="error" class="err">{{ error }}</p>
      <p class="hint">默认网关地址：{{ baseUrl }}</p>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '../services/api'
import { useAuthStore } from '../stores/auth'
import { toErrorMessage } from '../utils/error'

const router = useRouter()
const auth = useAuthStore()
auth.initFromStorage()

const tenantId = ref(import.meta.env.DEV ? 't-demo' : '')
const username = ref(import.meta.env.DEV ? 'admin' : '')
const password = ref(import.meta.env.DEV ? 'admin' : '')
const loading = ref(false)
const error = ref('')
const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'

async function submit() {
  error.value = ''
  loading.value = true
  try {
    const data = await login({ tenantId: tenantId.value, username: username.value, password: password.value })
    auth.setLogin({ tenantId: tenantId.value, username: username.value, accessToken: data.accessToken })
    await router.push('/dashboard')
  } catch (e: unknown) {
    error.value = toErrorMessage(e)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
}
.login-card {
  width: 420px;
  display: grid;
  gap: 10px;
}
label {
  display: grid;
  gap: 6px;
  font-size: 14px;
}
.err {
  color: #dc2626;
}
.hint {
  color: #6b7280;
  font-size: 12px;
}
</style>
