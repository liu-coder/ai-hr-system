<template>
  <el-card>
    <template #header>{{ title }}</template>
    <el-form inline>
      <slot />
      <el-form-item>
        <el-button type="primary" :loading="loading" @click="$emit('submit')">{{ submitText }}</el-button>
      </el-form-item>
    </el-form>
    <el-text v-if="hint" type="info">{{ hint }}</el-text>
  </el-card>
</template>

<script setup lang="ts">
defineProps<{
  title: string
  hint?: string
  loading?: boolean
  submitText?: string
}>()

defineEmits<{
  (e: 'submit'): void
}>()
</script>
