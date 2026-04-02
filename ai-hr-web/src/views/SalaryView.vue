<template>
  <section class="page-grid">
    <FilterPanel title="薪酬预览任务" hint="筛选条件会同步到 URL，可直接复制链接分享当前视图。" :loading="previewLoading" submit-text="发起预览" @submit="preview">
      <el-form-item label="员工ID"><el-input v-model="employeeId" /></el-form-item>
      <el-form-item label="计薪周期"><el-input v-model="payPeriod" /></el-form-item>
    </FilterPanel>
    <el-card>
      <el-space>
        <el-tag>taskId: {{ taskId || '-' }}</el-tag>
        <el-tag :type="taskId ? 'success' : 'info'">{{ taskId ? '任务已提交' : '未提交任务' }}</el-tag>
      </el-space>
    </el-card>

    <el-card>
      <template #header>薪酬条目明细</template>
      <el-space>
        <el-input v-model="calcRunId" placeholder="calcRunId" style="width: 320px;" />
        <el-button :loading="linesLoading" @click="queryLines">查询明细</el-button>
      </el-space>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" style="margin-top: 10px;" />
      <el-table v-else v-loading="linesLoading" :data="pagedRows" border style="margin-top: 10px;" empty-text="暂无明细">
        <el-table-column prop="employeeId" label="员工ID" />
        <el-table-column prop="itemCode" label="项目编码" />
        <el-table-column prop="itemName" label="项目名称" />
        <el-table-column prop="amountCents" label="金额(分)" />
        <el-table-column prop="currency" label="币种" />
      </el-table>
      <div class="table-footer">
        <el-tag type="info">明细总数：{{ lineRows.length }}</el-tag>
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :total="lineRows.length"
          @current-change="onPageChange"
        />
      </div>
    </el-card>
  </section>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import FilterPanel from '../components/FilterPanel.vue'
import { listSalaryLines, previewSalary } from '../services/api'
import { toErrorMessage } from '../utils/error'

const FILTER_KEY = 'aihr-salary-filter'
const route = useRoute()
const router = useRouter()

function getQueryValue(raw: unknown) {
  return typeof raw === 'string' ? raw : ''
}

const saved = (() => {
  try {
    const raw = localStorage.getItem(FILTER_KEY)
    return raw ? (JSON.parse(raw) as { employeeId?: string; payPeriod?: string; calcRunId?: string }) : {}
  } catch {
    return {}
  }
})()

const employeeId = ref(getQueryValue(route.query.employeeId) || saved.employeeId || 'emp-001')
const payPeriod = ref(getQueryValue(route.query.payPeriod) || saved.payPeriod || '2026-03')
const taskId = ref('')
const calcRunId = ref(getQueryValue(route.query.calcRunId) || saved.calcRunId || 'scr-demo-tdemo-202603')
const lineRows = ref<
  Array<{
    employeeId?: string
    itemCode?: string
    itemName?: string
    amountCents?: number
    currency?: string
  }>
>([])
const error = ref('')
const previewLoading = ref(false)
const linesLoading = ref(false)
const page = ref(Number(getQueryValue(route.query.page) || '1'))
const pageSize = 8

const pagedRows = computed(() => {
  const startIndex = (page.value - 1) * pageSize
  return lineRows.value.slice(startIndex, startIndex + pageSize)
})

async function preview() {
  previewLoading.value = true
  try {
    error.value = ''
    const data = await previewSalary({
      employeeId: employeeId.value,
      payPeriod: payPeriod.value,
      input: { basicSalary: 1500000, attendanceBonus: 200000 }
    })
    taskId.value = data.taskId
  } catch (e: unknown) {
    error.value = toErrorMessage(e)
  } finally {
    previewLoading.value = false
  }
}

async function queryLines() {
  linesLoading.value = true
  page.value = 1
  try {
    error.value = ''
    const data = await listSalaryLines(calcRunId.value)
    lineRows.value = Array.isArray(data) ? data : []
  } catch (e: unknown) {
    error.value = toErrorMessage(e)
  } finally {
    linesLoading.value = false
  }
}

function onPageChange(next: number) {
  page.value = next
}

watch([employeeId, payPeriod, calcRunId], () => {
  localStorage.setItem(
    FILTER_KEY,
    JSON.stringify({
      employeeId: employeeId.value,
      payPeriod: payPeriod.value,
      calcRunId: calcRunId.value
    })
  )
})

watch([employeeId, payPeriod, calcRunId, page], () => {
  router.replace({
    path: route.path,
    query: {
      employeeId: employeeId.value,
      payPeriod: payPeriod.value,
      calcRunId: calcRunId.value,
      page: String(page.value)
    }
  })
})

onMounted(() => {
  queryLines()
})
</script>

<style scoped>
.table-footer {
  margin-top: 12px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  flex-wrap: wrap;
}
</style>
