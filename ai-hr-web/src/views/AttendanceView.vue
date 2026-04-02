<template>
  <section class="page-grid">
    <FilterPanel title="考勤筛选" hint="筛选条件会同步到 URL，可直接复制链接分享当前视图。" :loading="loading" @submit="query">
      <el-form-item label="员工ID"><el-input v-model="employeeId" /></el-form-item>
      <el-form-item label="开始日期"><el-date-picker v-model="start" type="date" value-format="YYYY-MM-DD" /></el-form-item>
      <el-form-item label="结束日期"><el-date-picker v-model="end" type="date" value-format="YYYY-MM-DD" /></el-form-item>
    </FilterPanel>

    <el-card>
      <template #header>考勤异常结果</template>
      <el-alert v-if="error" :title="error" type="error" show-icon :closable="false" />
      <el-table v-else v-loading="loading" :data="pagedRows" border empty-text="当前条件下没有异常数据">
        <el-table-column label="员工ID">
          <template #default>{{ employeeId }}</template>
        </el-table-column>
        <el-table-column prop="workDate" label="日期" />
        <el-table-column prop="type" label="异常类型" />
        <el-table-column prop="severity" label="严重程度" />
        <el-table-column label="说明">
          <template #default="{ row }">
            {{ row.evidence?.summary || row.ruleHit?.message || '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="snapshotId" label="快照ID" min-width="140" show-overflow-tooltip />
      </el-table>
      <div class="table-footer">
        <el-tag type="info">异常总数：{{ rows.length }}</el-tag>
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="page"
          :page-size="pageSize"
          :total="rows.length"
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
import { listAttendanceAnomalies } from '../services/api'
import { toErrorMessage } from '../utils/error'

const FILTER_KEY = 'aihr-attendance-filter'
const route = useRoute()
const router = useRouter()

function getQueryValue(raw: unknown) {
  return typeof raw === 'string' ? raw : ''
}

const saved = (() => {
  try {
    const raw = localStorage.getItem(FILTER_KEY)
    return raw ? (JSON.parse(raw) as { employeeId?: string; start?: string; end?: string }) : {}
  } catch {
    return {}
  }
})()

const employeeId = ref(getQueryValue(route.query.employeeId) || saved.employeeId || 'emp-001')
const start = ref(getQueryValue(route.query.start) || saved.start || '2026-03-01')
const end = ref(getQueryValue(route.query.end) || saved.end || '2026-03-31')
const rows = ref<
  Array<{
    workDate?: string
    type?: string
    severity?: string
    snapshotId?: string
    evidence?: { summary?: string }
    ruleHit?: { message?: string }
  }>
>([])
const loading = ref(false)
const error = ref('')
const page = ref(Number(getQueryValue(route.query.page) || '1'))
const pageSize = 10

const pagedRows = computed(() => {
  const startIndex = (page.value - 1) * pageSize
  return rows.value.slice(startIndex, startIndex + pageSize)
})

async function query() {
  page.value = 1
  loading.value = true
  error.value = ''
  try {
    const data = await listAttendanceAnomalies(employeeId.value, start.value, end.value)
    rows.value = Array.isArray(data) ? data : []
  } catch (e: unknown) {
    error.value = toErrorMessage(e)
  } finally {
    loading.value = false
  }
}

function onPageChange(next: number) {
  page.value = next
}

watch([employeeId, start, end], () => {
  localStorage.setItem(
    FILTER_KEY,
    JSON.stringify({
      employeeId: employeeId.value,
      start: start.value,
      end: end.value
    })
  )
})

watch([employeeId, start, end, page], () => {
  router.replace({
    path: route.path,
    query: {
      employeeId: employeeId.value,
      start: start.value,
      end: end.value,
      page: String(page.value)
    }
  })
})

onMounted(() => {
  query()
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
