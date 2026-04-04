<template>
  <div class="wrap">
    <el-card class="card" shadow="hover">
      <template #header>
        <div class="head">
          <span class="title">金融科研 AI 辅助平台</span>
          <el-tag type="info" size="small">MVP</el-tag>
        </div>
      </template>
      <el-tabs v-model="tab">
        <el-tab-pane label="登录" name="in">
          <el-form @submit.prevent="onLogin">
            <el-form-item label="用户名">
              <el-input v-model="user" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="pass" type="password" show-password autocomplete="current-password" />
            </el-form-item>
            <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">登录</el-button>
          </el-form>
        </el-tab-pane>
        <el-tab-pane label="注册" name="up">
          <el-form @submit.prevent="onReg">
            <el-form-item label="用户名">
              <el-input v-model="user" autocomplete="username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="pass" type="password" show-password autocomplete="new-password" />
            </el-form-item>
            <el-form-item label="显示名">
              <el-input v-model="disp" />
            </el-form-item>
            <el-button type="primary" :loading="loading" native-type="submit" style="width: 100%">注册并登录</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const auth = useAuthStore()
const tab = ref('in')
const user = ref('demo')
const pass = ref('demo123456')
const disp = ref('演示用户')
const loading = ref(false)

async function onLogin() {
  loading.value = true
  try {
    await auth.login(user.value, pass.value)
    ElMessage.success('欢迎回来')
    router.push('/')
  } finally {
    loading.value = false
  }
}

async function onReg() {
  loading.value = true
  try {
    await auth.register(user.value, pass.value, disp.value)
    ElMessage.success('注册成功')
    router.push('/')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 与 fin-theme 侧栏藏青系一致 */
  background: linear-gradient(135deg, #0f172a 0%, #1e3a5f 45%, #334155 100%);
}
.card {
  width: 420px;
}
.head {
  display: flex;
  align-items: center;
  gap: 8px;
}
.title {
  font-weight: 600;
  font-size: 16px;
}
</style>
