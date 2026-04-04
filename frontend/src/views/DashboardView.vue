<!--
  【实验三 · 任务一】仪表盘：新建金融科研项目
  - 操作：点击「新建金融科研项目」→ 填名称/简介 → 创建后进入工作台。
  - 接口：GET /api/projects（列表）、POST /api/projects（创建）。
  【实验三 · 任务七（续）】回到本页后重新拉取列表，卡片上的进度条/阶段会反映服务端汇总结果；
  也可点工具栏「刷新项目列表」手动同步（与项目内「刷新完成度」配合：先在工作台点刷新，再回此处看列表）。
-->
<template>
  <div class="fin-app">
    <aside class="fin-sidebar">
      <div class="fin-logo">
        <div class="fin-logo-mark">金融研<span>AI</span></div>
        <div class="fin-logo-sub">FINANCIAL RESEARCH LAB</div>
      </div>
      <nav class="fin-nav">
        <div class="fin-nav-title">工作台</div>
        <div class="fin-nav-item active">
          <span class="fin-nav-ico">🏠</span>
          我的科研项目
        </div>
      </nav>
      <div class="fin-sidebar-foot">
        <div class="user-line">
          <span class="avatar">{{ avatarLetter }}</span>
          <div>
            <div class="uname">{{ auth.username || '用户' }}</div>
            <div class="urole">金融科研协作</div>
          </div>
        </div>
      </div>
    </aside>

    <div class="fin-main">
      <header class="fin-topbar">
        <div class="fin-bc">
          <span>工作台</span>
          <span>›</span>
          <strong>项目总览</strong>
        </div>
        <div class="fin-top-actions">
          <el-tag size="small" effect="plain">本地/云端均可部署</el-tag>
          <el-button size="small" text type="primary" @click="logout">退出</el-button>
        </div>
      </header>

      <main class="fin-content">
        <div class="fin-dash-stats">
          <div class="fin-stat-card">
            <div class="fin-stat-num">{{ projects.length }}</div>
            <div class="fin-stat-label">项目总数</div>
          </div>
          <div class="fin-stat-card">
            <div class="fin-stat-num">{{ inProgress }}</div>
            <div class="fin-stat-label">进行中</div>
          </div>
          <div class="fin-stat-card">
            <div class="fin-stat-num">{{ nearDone }}</div>
            <div class="fin-stat-label">接近结项(≥90%)</div>
          </div>
          <div class="fin-stat-card">
            <div class="fin-stat-num">—</div>
            <div class="fin-stat-label">团队 Git 协作</div>
          </div>
        </div>

        <div class="toolbar">
          <h2 class="section-h">项目列表</h2>
          <el-space>
            <el-button :loading="loadingList" @click="load">刷新项目列表</el-button>
            <el-button type="primary" :icon="Plus" @click="openCreate">新建金融科研项目</el-button>
          </el-space>
        </div>

        <el-row :gutter="16">
          <el-col v-for="p in projects" :key="p.id" :xs="24" :sm="12" :md="8" :lg="6">
            <el-card class="pcard" shadow="hover" @click="goProject(p.id)">
              <div class="pname">{{ p.name }}</div>
              <el-progress :percentage="p.progressPercent" :stroke-width="10" style="margin-top: 12px" />
              <div class="meta">
                <el-tag size="small">{{ p.stage || '进行中' }}</el-tag>
                <span class="muted">{{ fmt(p.createdAt) }}</span>
              </div>
            </el-card>
          </el-col>
        </el-row>

        <el-dialog v-model="dlg" title="新建金融科研项目" width="480px">
          <el-form>
            <el-form-item label="项目名称">
              <el-input v-model="newName" placeholder="如：注册制改革与资产定价" />
            </el-form-item>
            <el-form-item label="简介">
              <el-input v-model="newDesc" type="textarea" rows="3" />
            </el-form-item>
          </el-form>
          <template #footer>
            <el-button @click="dlg = false">取消</el-button>
            <el-button type="primary" :loading="saving" @click="createProject">创建</el-button>
          </template>
        </el-dialog>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Plus } from '@element-plus/icons-vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'

const auth = useAuthStore()
const router = useRouter()
const projects = ref<any[]>([])
const dlg = ref(false)
const newName = ref('资本市场定价效率研究')
const newDesc = ref('')
const saving = ref(false)
/** 任务七：手动刷新列表时用 */
const loadingList = ref(false)

const avatarLetter = computed(() => (auth.username || 'U').slice(0, 1).toUpperCase())

const inProgress = computed(() => projects.value.filter((p) => (p.progressPercent || 0) < 100).length)
const nearDone = computed(() => projects.value.filter((p) => (p.progressPercent || 0) >= 90).length)

function fmt(s: string) {
  if (!s) return ''
  return s.slice(0, 10)
}

/** 拉取当前用户全部项目（含 progressPercent / stage），进入页面与点击「刷新项目列表」时调用 */
async function load() {
  loadingList.value = true
  try {
    const { data } = await http.get('/api/projects')
    projects.value = data
  } finally {
    loadingList.value = false
  }
}

function goProject(id: number) {
  router.push('/project/' + id)
}

function openCreate() {
  dlg.value = true
}

async function createProject() {
  saving.value = true
  try {
    const { data } = await http.post('/api/projects', { name: newName.value, description: newDesc.value })
    dlg.value = false
    await load()
    goProject(data.id)
  } finally {
    saving.value = false
  }
}

function logout() {
  auth.logout()
  router.push('/login')
}

onMounted(load)
</script>

<style scoped>
.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}
.section-h {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: var(--fin-ink, #0f172a);
}
.pcard {
  margin-bottom: 16px;
  cursor: pointer;
}
.pname {
  font-weight: 600;
  font-size: 15px;
}
.meta {
  margin-top: 10px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.muted {
  color: #909399;
  font-size: 12px;
}
.user-line {
  display: flex;
  align-items: center;
  gap: 10px;
}
.avatar {
  width: 32px;
  height: 32px;
  border-radius: 50%;
  background: linear-gradient(135deg, #3b82f6, #1e3a5f);
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  font-weight: 600;
}
.uname {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.85);
}
.urole {
  font-size: 10px;
  color: rgba(255, 255, 255, 0.35);
}
</style>
