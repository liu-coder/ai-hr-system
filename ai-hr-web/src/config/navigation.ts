export type AppNavItem = {
  path: string
  title: string
  subtitle: string
}

export const APP_NAV_ITEMS: AppNavItem[] = [
  { path: '/dashboard', title: '运营总览', subtitle: '健康、租户、用户、入口看板' },
  { path: '/attendance', title: '考勤中心', subtitle: '异常查询、区间分析、明细核验' },
  { path: '/salary', title: '薪酬中心', subtitle: '预览任务、条目明细、核对流程' },
  { path: '/ai', title: 'AI 助理', subtitle: '对话问答、证据摘要、追问补全' }
]
