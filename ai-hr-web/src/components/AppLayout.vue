<template>
  <el-container class="shell">
    <el-aside width="250px" class="shell__aside">
      <div class="brand">
        <div class="brand__badge">AI</div>
        <div>
          <div class="brand__title">AI-HR Console</div>
          <div class="brand__subtitle">企业人事智能运营台</div>
        </div>
      </div>

      <el-menu
        :default-active="route.path"
        router
        class="shell__menu"
        background-color="transparent"
        text-color="var(--shell-aside-text)"
        active-text-color="var(--shell-aside-active)"
      >
        <el-menu-item v-for="item in navItems" :key="item.path" :index="item.path">
          <div class="menu-item">
            <div class="menu-item__title">{{ item.title }}</div>
            <div class="menu-item__subtitle">{{ item.subtitle }}</div>
          </div>
        </el-menu-item>
      </el-menu>

      <div class="tenant-panel">
        <div class="tenant-panel__label">当前租户</div>
        <div class="tenant-panel__value">{{ auth.tenantId || '-' }}</div>
        <div class="tenant-panel__label">登录用户</div>
        <div class="tenant-panel__value">{{ auth.username || '-' }}</div>
      </div>
    </el-aside>

    <el-container>
      <el-header class="shell__header">
        <div>
          <div class="shell__page-title">{{ currentTitle }}</div>
          <el-breadcrumb separator=">" class="shell__breadcrumb">
            <el-breadcrumb-item>AI-HR</el-breadcrumb-item>
            <el-breadcrumb-item>{{ currentTitle }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>

        <div class="shell__header-actions">
          <el-tag type="info">{{ auth.tenantId }}</el-tag>
          <el-button type="danger" plain @click="logout">退出登录</el-button>
        </div>
      </el-header>

      <el-main class="shell__main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from '../stores/auth'
import { APP_NAV_ITEMS } from '../config/navigation'

const navItems = APP_NAV_ITEMS
const auth = useAuthStore()
const router = useRouter()
const route = useRoute()

const currentTitle = computed(() => {
  const matched = navItems.find((item) => item.path === route.path)
  return matched?.title ?? '工作台'
})

function logout() {
  auth.logout()
  router.push('/login')
}
</script>

<style scoped>
.shell {
  min-height: 100vh;
  background: radial-gradient(circle at top left, #f6fbff 0%, #edf4ff 40%, #f5f7fa 100%);
}

.shell__aside {
  color: var(--shell-aside-text);
  border-right: 1px solid rgba(255, 255, 255, 0.08);
  background: linear-gradient(180deg, #10213f 0%, #0d1b32 55%, #091424 100%);
  display: flex;
  flex-direction: column;
  padding: 16px;
}

.brand {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.brand__badge {
  width: 40px;
  height: 40px;
  border-radius: 12px;
  display: grid;
  place-items: center;
  font-weight: 700;
  color: #0b1324;
  background: linear-gradient(145deg, #56f7cc 0%, #7ec6ff 100%);
}

.brand__title {
  font-size: 18px;
  font-weight: 700;
  color: #ffffff;
}

.brand__subtitle {
  font-size: 12px;
  color: rgba(212, 225, 246, 0.82);
}

.shell__menu {
  border-right: none;
  --el-menu-hover-bg-color: rgba(126, 198, 255, 0.12);
  --el-menu-item-height: 58px;
  --el-menu-sub-item-height: 58px;
}

.menu-item {
  display: flex;
  flex-direction: column;
  gap: 2px;
  line-height: 1.2;
}

.menu-item__title {
  font-size: 14px;
  font-weight: 600;
}

.menu-item__subtitle {
  font-size: 11px;
  opacity: 0.72;
}

.tenant-panel {
  margin-top: auto;
  border: 1px solid rgba(126, 198, 255, 0.18);
  border-radius: 12px;
  padding: 12px;
  background: rgba(255, 255, 255, 0.03);
  display: grid;
  gap: 4px;
}

.tenant-panel__label {
  font-size: 11px;
  color: rgba(212, 225, 246, 0.74);
}

.tenant-panel__value {
  font-size: 13px;
  font-weight: 600;
  color: #f8fbff;
  margin-bottom: 6px;
}

.shell__header {
  height: 72px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  background: rgba(255, 255, 255, 0.72);
  border-bottom: 1px solid #d7e5f5;
  backdrop-filter: blur(8px);
}

.shell__page-title {
  font-size: 22px;
  font-weight: 700;
  color: #1f2b3d;
}

.shell__breadcrumb {
  margin-top: 4px;
}

.shell__header-actions {
  display: flex;
  align-items: center;
  gap: 10px;
}

.shell__main {
  padding: 18px;
}

@media (max-width: 1024px) {
  .shell__aside {
    width: 220px !important;
  }

  .menu-item__subtitle {
    display: none;
  }
}

@media (max-width: 820px) {
  .shell__aside {
    display: none;
  }

  .shell__header {
    padding: 0 12px;
  }

  .shell__main {
    padding: 12px;
  }
}
</style>
