<template>
  <section class="page-grid">
    <el-card>
      <template #header>AI 业务问答</template>
      <el-form>
        <el-form-item label="问题">
          <el-input v-model="message" type="textarea" :rows="4" />
        </el-form-item>
        <div class="preset-list">
          <el-tag
            v-for="preset in presets"
            :key="preset"
            class="preset-item"
            effect="plain"
            @click="message = preset"
          >
            {{ preset }}
          </el-tag>
        </div>
        <el-space>
          <el-button type="primary" :loading="loading" @click="send">发送</el-button>
          <el-button plain @click="resetSession">重置会话</el-button>
          <el-tag type="info">会话ID：{{ sessionId || '未创建' }}</el-tag>
        </el-space>
      </el-form>
    </el-card>

    <el-card>
      <template #header>对话与证据</template>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
      <div v-else class="chat-list">
        <el-empty v-if="records.length === 0" description="发送问题后，这里会展示模型回复与工具证据" />
        <div v-for="item in records" :key="item.id" class="chat-item" :class="item.role">
          <div class="chat-item__meta">
            <span>{{ item.role === 'user' ? '你' : 'AI 助理' }}</span>
            <span>{{ item.timestamp }}</span>
          </div>
          <div class="chat-item__main">{{ item.message }}</div>
          <div v-if="item.evidenceSummary || item.fallbackReason || item.routeLabel || item.toolName" class="chat-item__sub">
            <div v-if="item.routeLabel">路由：{{ item.routeLabel }}</div>
            <div v-if="item.toolName">工具：{{ item.toolName }}</div>
            <div v-if="item.evidenceSummary">证据摘要：{{ item.evidenceSummary }}</div>
            <div v-if="item.fallbackReason">说明：{{ item.fallbackReason }}</div>
          </div>
          <div class="chat-item__actions">
            <el-button link size="small" @click="copyRecord(item)">复制</el-button>
          </div>
        </div>
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { askAi } from '../services/api'
import { toErrorMessage } from '../utils/error'

type ChatRecord = {
  id: string
  role: 'user' | 'assistant'
  message: string
  timestamp: string
  evidenceSummary?: string
  fallbackReason?: string
  routeLabel?: string
  toolName?: string
}

const message = ref('请分析员工近30天考勤异常趋势')
const sessionId = ref('')
const records = ref<ChatRecord[]>([])
const loading = ref(false)
const error = ref('')
const presets = [
  '请分析本月考勤异常趋势并给出管理建议',
  '请总结当前薪酬明细中的主要成本项',
  '请根据异常类型给出本周排班优化建议',
  '请输出一段适合群公告的人事提醒文案'
]

async function send() {
  if (!message.value.trim()) return
  loading.value = true
  error.value = ''
  const now = new Date().toLocaleTimeString('zh-CN', { hour12: false })
  records.value.push({
    id: `${Date.now()}-q`,
    role: 'user',
    message: message.value.trim(),
    timestamp: now
  })
  try {
    const data = await askAi(message.value.trim(), sessionId.value || undefined)
    if (data.sessionId) {
      sessionId.value = data.sessionId
    }
    records.value.push({
      id: `${Date.now()}-a`,
      role: 'assistant',
      message: data.llmResult || data.fallbackReason || '已收到请求，但无可展示文本',
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false }),
      evidenceSummary: data.evidenceSummary || undefined,
      fallbackReason: data.fallbackReason || undefined,
      routeLabel: data.route?.intentLabel || data.route?.route || undefined,
      toolName: data.toolCall?.toolName || undefined
    })
    message.value = ''
  } catch (e: unknown) {
    records.value.push({
      id: `${Date.now()}-err`,
      role: 'assistant',
      message: '调用失败，请检查网关与认证状态后重试。',
      timestamp: new Date().toLocaleTimeString('zh-CN', { hour12: false })
    })
    error.value = toErrorMessage(e)
  } finally {
    loading.value = false
  }
}

function resetSession() {
  sessionId.value = ''
  records.value = []
  error.value = ''
}

async function copyRecord(item: ChatRecord) {
  const text = `[${item.role === 'user' ? '你' : 'AI'} ${item.timestamp}] ${item.message}`
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success('已复制到剪贴板')
  } catch {
    ElMessage.error('复制失败，请检查浏览器权限')
  }
}
</script>

<style scoped>
.preset-list {
  margin-bottom: 10px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.preset-item {
  cursor: pointer;
}

.chat-list {
  display: grid;
  gap: 10px;
}

.chat-item {
  border-radius: 10px;
  border: 1px solid #dbe5f2;
  background: #f9fbff;
  padding: 12px;
  display: grid;
  gap: 6px;
}

.chat-item.user {
  border-color: #cde0ff;
  background: #edf4ff;
}

.chat-item__meta {
  font-size: 12px;
  color: #64748b;
  display: flex;
  justify-content: space-between;
}

.chat-item__main {
  font-size: 14px;
  line-height: 1.6;
  color: #1f2937;
  white-space: pre-wrap;
}

.chat-item__sub {
  border-top: 1px dashed #d0dae8;
  padding-top: 6px;
  color: #475569;
  font-size: 12px;
  display: grid;
  gap: 4px;
}

.chat-item__actions {
  display: flex;
  justify-content: flex-end;
}
</style>
