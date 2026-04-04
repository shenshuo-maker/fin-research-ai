<!--
  【实验三 · 任务四】论文写作（对应 DEMO：填选题/识别/结果 → 生成初稿；段落改写、文献匹配、结果解读；导出）
  - 论文持久化：GET/PUT /api/projects/:id/paper；版本快照见 snapshot()
  - AI：生成/改写/匹配/解读等见 script 内 http.post 路径（PaperController 等）
-->
<template>
  <div class="panel">
    <el-row :gutter="16">
      <el-col :span="16">
        <!-- 任务四：正文编辑区，与右侧 AI 工具配合形成「方法+结果」到成稿的闭环 -->
        <el-card header="论文正文">
          <el-form inline>
            <el-form-item label="标题">
              <el-input v-model="title" style="width: 320px" />
            </el-form-item>
            <el-button type="primary" @click="savePaper">保存</el-button>
            <el-button @click="snapshot">版本快照</el-button>
          </el-form>
          <el-input v-model="content" type="textarea" :rows="22" placeholder="在此编辑或使用 AI 生成初稿" />
        </el-card>
      </el-col>
      <el-col :span="8">
        <!-- 任务四：结构化调用后端写作类接口，输出可粘贴回正文或随论文导出 -->
        <el-card header="AI 写作工具">
          <el-form label-position="top">
            <el-form-item label="生成初稿（选题+识别+结果）">
              <el-input v-model="gTitle" placeholder="选题" />
              <el-input v-model="gId" placeholder="识别策略" style="margin-top: 6px" />
              <el-input v-model="gRes" type="textarea" :rows="3" placeholder="实证结果摘要" style="margin-top: 6px" />
              <el-button type="primary" style="margin-top: 8px" @click="generate">生成</el-button>
            </el-form-item>
            <el-form-item label="段落改写">
              <el-input v-model="sel" type="textarea" :rows="4" placeholder="粘贴段落" />
              <el-radio-group v-model="mode" size="small" style="margin-top: 6px">
                <el-radio-button label="学术化" />
                <el-radio-button label="精简" />
                <el-radio-button label="扩写" />
                <el-radio-button label="润色" />
              </el-radio-group>
              <el-button style="margin-top: 8px" @click="rewrite">改写</el-button>
              <el-input v-model="rewritten" type="textarea" :rows="4" readonly style="margin-top: 8px" />
            </el-form-item>
            <el-form-item label="文献匹配">
              <el-input v-model="snippet" type="textarea" :rows="2" />
              <el-select v-model="cstyle" style="margin-top: 6px; width: 100%">
                <el-option label="GB/T 7714" value="GB/T 7714" />
                <el-option label="APA" value="APA" />
              </el-select>
              <el-button style="margin-top: 8px" @click="matchCit">匹配</el-button>
              <el-input v-model="citJson" type="textarea" :rows="5" readonly />
            </el-form-item>
            <el-form-item label="结果解读（粘贴统计输出）">
              <el-input v-model="statsText" type="textarea" :rows="3" />
              <el-button style="margin-top: 8px" @click="interpret">解读</el-button>
              <el-input v-model="interpretOut" type="textarea" :rows="4" readonly />
            </el-form-item>
          </el-form>
          <el-divider />
          <el-button @click="dlJson">导出 JSON</el-button>
          <el-button @click="dlXlsx">导出 XLSX</el-button>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const props = defineProps<{ projectId: number }>()
const auth = useAuthStore()
const paperId = ref<number | null>(null)
const title = ref('')
const content = ref('')
const gTitle = ref('数字金融与企业创新')
const gId = ref('双向固定效应 / DID')
const gRes = ref('核心解释变量在 1% 水平显著为正')
const sel = ref('')
const mode = ref('润色')
const rewritten = ref('')
const snippet = ref('')
const cstyle = ref('GB/T 7714')
const citJson = ref('')
const statsText = ref('')
const interpretOut = ref('')

async function load() {
  const { data } = await http.get(`/api/projects/${props.projectId}/paper`)
  paperId.value = data.id
  title.value = data.title || ''
  content.value = data.content || ''
}

async function savePaper() {
  await http.put(`/api/projects/${props.projectId}/paper`, { title: title.value, content: content.value })
  ElMessage.success('已保存')
}

async function snapshot() {
  await http.post(`/api/projects/${props.projectId}/paper/versions`, { note: '手动快照' })
  ElMessage.success('已创建版本')
}

async function generate() {
  const { data } = await http.post(`/api/projects/${props.projectId}/paper/generate`, {
    title: gTitle.value,
    identification: gId.value,
    results: gRes.value,
  })
  title.value = data.title
  content.value = data.content
  ElMessage.success('初稿已生成')
}

async function rewrite() {
  const { data } = await http.post(`/api/projects/${props.projectId}/paper/rewrite`, {
    paragraph: sel.value,
    mode: mode.value,
  })
  rewritten.value = data.text
}

async function matchCit() {
  const { data } = await http.post(`/api/projects/${props.projectId}/paper/citations`, {
    snippet: snippet.value,
    style: cstyle.value,
  })
  citJson.value = data.json
}

async function interpret() {
  const { data } = await http.post(`/api/projects/${props.projectId}/paper/interpret-stats`, {
    stats: statsText.value,
  })
  interpretOut.value = data.text
}

function authHeader() {
  return { Authorization: 'Bearer ' + auth.token }
}

async function dlJson() {
  const res = await fetch(`/api/projects/${props.projectId}/paper/export/json`, { headers: authHeader() })
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'paper.json'
  a.click()
}

async function dlXlsx() {
  const res = await fetch(`/api/projects/${props.projectId}/paper/export/xlsx`, { headers: authHeader() })
  const blob = await res.blob()
  const a = document.createElement('a')
  a.href = URL.createObjectURL(blob)
  a.download = 'paper.xlsx'
  a.click()
}

onMounted(load)
</script>

<style scoped>
.panel {
  padding: 4px 0;
}
</style>
