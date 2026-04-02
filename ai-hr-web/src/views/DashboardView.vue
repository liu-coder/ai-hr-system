<template>
  <section class="page-grid">
    <el-row :gutter="12">
      <el-col :xs="24" :md="8"><el-card>网关地址：{{ baseUrl }}</el-card></el-col>
      <el-col :xs="24" :md="8"><el-card>当前租户：{{ auth.tenantId }}</el-card></el-col>
      <el-col :xs="24" :md="8"><el-card>当前用户：{{ auth.username }}</el-card></el-col>
    </el-row>

    <el-row :gutter="12">
      <el-col :xs="24" :md="6">
        <el-card class="metric-card">
          <div class="metric-label">网关状态</div>
          <div class="metric-value">{{ healthStatus }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card class="metric-card">
          <div class="metric-label">AI 可用性</div>
          <div class="metric-value">{{ aiStatus }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card class="metric-card">
          <div class="metric-label">本月异常数</div>
          <div class="metric-value">{{ anomalyCount }}</div>
        </el-card>
      </el-col>
      <el-col :xs="24" :md="6">
        <el-card class="metric-card">
          <div class="metric-label">薪酬明细条数</div>
          <div class="metric-value">{{ salaryLineCount }}</div>
        </el-card>
      </el-col>
    </el-row>

    <el-card class="section-card">
      <template #header>运营总览</template>
      <el-space>
        <el-button type="primary" @click="refreshOverview" :loading="checking">刷新总览</el-button>
        <el-tag :type="healthTagType">{{ healthStatus }}</el-tag>
        <el-tag :type="aiStatus === '可用' ? 'success' : 'warning'">AI：{{ aiStatus }}</el-tag>
        <el-tag type="info">更新于：{{ updatedAt || '-' }}</el-tag>
      </el-space>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-top: 10px" />
    </el-card>

    <el-card>
      <template #header>快捷入口</template>
      <el-space wrap>
        <router-link to="/attendance"><el-button>进入考勤中心</el-button></router-link>
        <router-link to="/salary"><el-button>进入薪酬中心</el-button></router-link>
        <router-link to="/ai"><el-button type="primary" plain>进入 AI 助理</el-button></router-link>
      </el-space>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useAuthStore } from '../stores/auth'
import { askAi, getGatewayHealth, listAttendanceAnomalies, listSalaryLines } from '../services/api'
import { toErrorMessage } from '../utils/error'

const auth = useAuthStore()
const baseUrl = import.meta.env.VITE_API_BASE_URL || '/api'
const checking = ref(false)
const healthStatus = ref('UNKNOWN')
const aiStatus = ref('未知')
const anomalyCount = ref(0)
const salaryLineCount = ref(0)
const updatedAt = ref('')
const error = ref('')
const healthTagType = computed(() => (healthStatus.value === 'UP' ? 'success' : healthStatus.value === 'DOWN' ? 'danger' : 'info'))

async function refreshOverview() {
  checking.value = true
  error.value = ''
  try {
    const [health, anomalies, salaryLines, ai] = await Promise.all([
      getGatewayHealth(),
      listAttendanceAnomalies('emp-001', '2026-03-01', '2026-03-31'),
      listSalaryLines('scr-demo-tdemo-202603'),
      askAi('请简要回复“ok”，用于系统可用性检测')
    ])
    healthStatus.value = health.status || 'UNKNOWN'
    anomalyCount.value = anomalies.length
    salaryLineCount.value = salaryLines.length
    aiStatus.value = ai.llmAvailable ? '可用' : '不可用'
    updatedAt.value = new Date().toLocaleString('zh-CN', { hour12: false })
  } catch (e: unknown) {
    error.value = toErrorMessage(e)
  } finally {
    checking.value = false
  }
}

onMounted(() => {
  refreshOverview()
})
</script>

<style scoped>
.metric-card {
  min-height: 110px;
  display: grid;
  align-content: center;
  gap: 8px;
}

.metric-label {
  color: #64748b;
  font-size: 13px;
}

.metric-value {
  color: #0f172a;
  font-size: 28px;
  font-weight: 700;
}
</style>
