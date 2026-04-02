import { createRouter, createWebHistory } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import LoginView from '../views/LoginView.vue'
import DashboardView from '../views/DashboardView.vue'
import AttendanceView from '../views/AttendanceView.vue'
import SalaryView from '../views/SalaryView.vue'
import AiView from '../views/AiView.vue'
import AppLayout from '../components/AppLayout.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/login', name: 'login', component: LoginView, meta: { title: '登录' } },
    {
      path: '/',
      component: AppLayout,
      meta: { requiresAuth: true },
      children: [
        { path: '', redirect: '/dashboard' },
        { path: 'dashboard', name: 'dashboard', component: DashboardView, meta: { title: '运营总览', requiresAuth: true } },
        { path: 'attendance', name: 'attendance', component: AttendanceView, meta: { title: '考勤中心', requiresAuth: true } },
        { path: 'salary', name: 'salary', component: SalaryView, meta: { title: '薪酬中心', requiresAuth: true } },
        { path: 'ai', name: 'ai', component: AiView, meta: { title: 'AI 助理', requiresAuth: true } }
      ]
    }
  ]
})

router.beforeEach((to) => {
  const auth = useAuthStore()
  const requiresAuth = to.matched.some((record) => Boolean(record.meta.requiresAuth))

  if (requiresAuth && !auth.isAuthenticated) {
    return { name: 'login' }
  }

  if (to.name === 'login' && auth.isAuthenticated) {
    return { name: 'dashboard' }
  }

  return true
})

router.afterEach((to) => {
  const title = (to.meta.title as string | undefined) ?? 'AI-HR Console'
  document.title = `${title} | AI-HR`
})

export default router
