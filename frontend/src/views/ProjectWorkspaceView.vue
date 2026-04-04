<!--
  项目内工作台：侧栏在【实验三 · 任务二～六】之间切换；底栏返回仪表盘对应【任务七】。
  - 任务二：文献与选题（TopicPanel）
  - 任务三：数据与处理（DataPanel）
  - 任务四：论文写作（WritingPanel）
  - 任务五：投稿回修（RevisionPanel）
  - 任务六：合规与文档（DocsPanel）
  【实验三 · 任务七】顶栏「刷新完成度」→ POST /api/projects/:id/progress/refresh，
  后端按文献/选题/EDC/数据集/统计/论文/回修等里程碑重算 progressPercent 与 stage。
-->
<template>
  <div class="fin-app">
    <aside class="fin-sidebar">
      <div class="fin-logo">
        <div class="fin-logo-mark">金融研<span>AI</span></div>
        <div class="fin-logo-sub">FINANCIAL RESEARCH LAB</div>
      </div>
      <nav class="fin-nav">
        <div class="fin-nav-title">本项目</div>
        <div
          v-for="item in navItems"
          :key="item.key"
          :class="['fin-nav-item', { active: module === item.key }]"
          @click="module = item.key"
        >
          <span class="fin-nav-ico">{{ item.icon }}</span>
          {{ item.label }}
        </div>
      </nav>
      <div class="fin-sidebar-foot">
        <el-button style="width: 100%; color: #93c5fd" text @click="router.push('/')">
          ← 返回项目列表
        </el-button>
      </div>
    </aside>

    <div class="fin-main">
      <header class="fin-topbar">
        <div class="fin-bc">
          <span>{{ breadcrumb[0] }}</span>
          <span>›</span>
          <strong>{{ breadcrumb[1] }}</strong>
        </div>
        <div class="fin-top-actions">
          <el-tag v-if="project" size="small" type="info">{{ project.progressPercent }}% 完成度</el-tag>
          <el-tag size="small" effect="plain">审计 / 脱敏</el-tag>
          <el-button size="small" @click="refreshProgress">刷新完成度</el-button>
        </div>
      </header>

      <main v-loading="!project" class="fin-content">
        <template v-if="project">
          <TopicPanel v-if="module === 'topic'" :project-id="project.id" />
          <DataPanel v-else-if="module === 'data'" :project-id="project.id" />
          <WritingPanel v-else-if="module === 'write'" :project-id="project.id" />
          <RevisionPanel v-else-if="module === 'rev'" :project-id="project.id" />
          <DocsPanel v-else-if="module === 'doc'" :project-id="project.id" />
        </template>
      </main>
    </div>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import http from '@/api/http'
import TopicPanel from './project/TopicPanel.vue'
import DataPanel from './project/DataPanel.vue'
import WritingPanel from './project/WritingPanel.vue'
import RevisionPanel from './project/RevisionPanel.vue'
import DocsPanel from './project/DocsPanel.vue'

const route = useRoute()
const router = useRouter()
const project = ref<any>(null)

/** 当前功能模块（与侧栏一一对应） */
const module = ref<'topic' | 'data' | 'write' | 'rev' | 'doc'>('topic')

const navItems = [
  { key: 'topic' as const, label: '文献与选题', icon: '📚' },
  { key: 'data' as const, label: '数据与处理', icon: '📊' },
  { key: 'write' as const, label: '论文写作', icon: '✍️' },
  { key: 'rev' as const, label: '投稿回修', icon: '✉️' },
  { key: 'doc' as const, label: '合规与文档', icon: '📋' },
]

const breadcrumb = computed(() => {
  const m = navItems.find((x) => x.key === module.value)
  return ['项目工作台', m?.label || '—']
})

async function load() {
  const id = Number(route.params.id)
  const { data } = await http.get('/api/projects/' + id)
  project.value = data
}

/** 任务七：触发服务端 ProjectProgressService.refresh，再拉取项目详情更新顶栏百分比 */
async function refreshProgress() {
  if (!project.value) return
  await http.post('/api/projects/' + project.value.id + '/progress/refresh')
  await load()
}

onMounted(load)
watch(() => route.params.id, load)
</script>

<style scoped>
/* 侧栏内按钮在 fin-theme 中已设浅色文字 */
</style>
