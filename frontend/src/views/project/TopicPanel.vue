<!--
  【实验三 · 任务二】选题与文献（对应 DEMO：上传 PDF → AI 解读；科研/热点/时政选题）
  - 文献：POST 上传 /api/projects/:id/literatures，解读 POST .../literatures/:litId/analyze
  - 选题：POST .../topics/generate | timely | current-affairs
  说明：未配置 ANTHROPIC_API_KEY 时接口仍成功，内容为演示占位，用于验收界面闭环。
-->
<template>
  <div>
    <div class="fin-page-title">文献与选题</div>
    <p class="fin-page-desc">
      覆盖顶刊 PDF 解读、金融学实证选题与政策/时政结合的高频数据选题设计（输出可与 EDC 表头对齐）。
    </p>

    <el-tabs v-model="subTab" type="border-card" class="topic-tabs">
      <!-- —— 文献解读 —— -->
      <el-tab-pane label="文献解读" name="lit">
        <el-row :gutter="16">
          <el-col :xs="24" :md="11">
            <el-card shadow="never" header="文献 PDF 与要点">
              <el-upload
                :action="uploadUrl"
                :headers="headers"
                name="file"
                :show-file-list="false"
                :on-success="onUploaded"
                accept=".pdf"
              >
                <el-button type="primary">上传金融类 PDF</el-button>
              </el-upload>
              <p class="hint">支持公司金融、资产定价、宏观金融等中英文文献；上传后点击「AI 解读」。</p>
              <el-table :data="lits" size="small" style="margin-top: 12px">
                <el-table-column prop="fileName" label="文件" />
                <el-table-column prop="status" label="状态" width="100" />
                <el-table-column label="操作" width="120">
                  <template #default="{ row }">
                    <el-button link type="primary" @click="analyze(row)">AI 解读</el-button>
                  </template>
                </el-table-column>
              </el-table>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="13">
            <el-card shadow="never" header="结构化解读结果（JSON，可对接下游选题）">
              <el-input
                v-model="analysisView"
                type="textarea"
                :rows="16"
                readonly
                placeholder="解读结果将显示于此"
              />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- —— 科研选题 —— -->
      <el-tab-pane label="科研选题" name="topic">
        <el-row :gutter="16">
          <el-col :xs="24" :md="10">
            <el-card shadow="never" header="研究方向与热点">
              <el-form label-position="top">
                <el-form-item label="研究方向（生成 3–5 个可落地选题）">
                  <el-input v-model="direction" placeholder="如：数字金融与企业全要素生产率" />
                  <el-button style="margin-top: 8px" type="primary" :loading="loadingGen" @click="genTopic">
                    生成选题
                  </el-button>
                </el-form-item>
                <el-form-item label="时效性主题（政策/市场语境）">
                  <el-input v-model="theme" placeholder="如：资本新规与银行风险承担" />
                  <el-button style="margin-top: 8px" :loading="loadingTimely" @click="timely">生成热点选题</el-button>
                </el-form-item>
              </el-form>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="14">
            <el-card shadow="never" header="AI 输出">
              <el-input v-model="topicOut" type="textarea" :rows="20" readonly placeholder="选题 JSON 输出" />
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>

      <!-- —— 时政选题 —— -->
      <el-tab-pane label="时政选题" name="affairs">
        <el-row :gutter="16">
          <el-col :xs="24" :md="10">
            <el-card shadow="never" header="政策焦点与数据频率">
              <el-form label-position="top">
                <el-form-item label="时政 / 政策与市场焦点">
                  <el-input
                    v-model="affairsTheme"
                    type="textarea"
                    :rows="3"
                    placeholder="例：全面注册制、北向资金、地方政府债务与城投债定价等"
                  />
                </el-form-item>
                <el-form-item label="数据更新频率（影响识别策略）">
                  <el-select v-model="affairsFreq" placeholder="选择频率" style="width: 100%">
                    <el-option label="日频（行情、高频事件研究）" value="日频" />
                    <el-option label="周频" value="周频" />
                    <el-option label="月频（财报/宏观）" value="月频" />
                    <el-option label="季频" value="季频" />
                    <el-option label="年度面板" value="年度" />
                  </el-select>
                </el-form-item>
                <el-form-item label="计划使用的数据字段（可与 CSMAR / AkShare 对齐）">
                  <el-input
                    v-model="affairsFields"
                    type="textarea"
                    :rows="4"
                    placeholder="例：股票收益率、Amihud、换手率、产权性质、融资约束 SA 指数……"
                  />
                </el-form-item>
                <el-form-item label="与 EDC / 手工采集衔接说明">
                  <el-input
                    v-model="affairsEdc"
                    type="textarea"
                    :rows="2"
                    placeholder="例：需在「数据与处理」中增加事件日、窗口期、政策哑变量等字段"
                  />
                </el-form-item>
                <el-button type="primary" :loading="loadingAffairs" style="width: 100%" @click="runAffairs">
                  生成时政实证选题
                </el-button>
                <el-alert
                  class="mt"
                  type="info"
                  :closable="false"
                  show-icon
                  title="建议在「数据与处理」模块保存 EDC 表单后，将此处生成的伪表头字段粘贴到表单设计。"
                />
              </el-form>
            </el-card>
          </el-col>
          <el-col :xs="24" :md="14">
            <el-card shadow="never" header="时政选题 AI 输出">
              <el-input v-model="affairsOut" type="textarea" :rows="14" readonly placeholder="JSON，含 edcPseudoHeaders 等" />
            </el-card>
            <el-card v-if="pseudoHeaders.length" shadow="never" class="mt" header="推荐伪数据表头（便于 EDC 录入）">
              <el-table :data="pseudoHeaders.map((h) => ({ h }))" size="small">
                <el-table-column prop="h" label="字段名" />
              </el-table>
            </el-card>
          </el-col>
        </el-row>
      </el-tab-pane>
    </el-tabs>
  </div>
</template>

<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const props = defineProps<{ projectId: number }>()
const auth = useAuthStore()

const subTab = ref<'lit' | 'topic' | 'affairs'>('lit')
const lits = ref<any[]>([])
const analysisView = ref('')
const direction = ref('数字金融与企业创新')
const theme = ref('宏观审慎与商业银行行为')
const topicOut = ref('')
const loadingGen = ref(false)
const loadingTimely = ref(false)

/** 时政选题 */
const affairsTheme = ref('资本市场注册制改革与新股定价效率')
const affairsFreq = ref('月频')
const affairsFields = ref('日收益率、累计超额收益、承销商声誉、发行市盈率、流动性')
const affairsEdc = ref('手工标注政策冲击窗口，与 CSMAR 日行情通过证券代码合并')
const affairsOut = ref('')
const loadingAffairs = ref(false)
/** 从返回 JSON 解析出的伪表头 */
const pseudoHeaders = ref<string[]>([])

const uploadUrl = computed(() => `/api/projects/${props.projectId}/literatures`)
const headers = computed(() => ({ Authorization: 'Bearer ' + auth.token }))

async function loadLits() {
  const { data } = await http.get(`/api/projects/${props.projectId}/literatures`)
  lits.value = data
}

function onUploaded(res: any) {
  if (res && res.code !== undefined && res.code !== 200) {
    ElMessage.error(res.message || '上传失败')
    return
  }
  ElMessage.success('上传成功')
  loadLits()
}

async function analyze(row: any) {
  const { data } = await http.post(`/api/projects/${props.projectId}/literatures/${row.id}/analyze`)
  analysisView.value = data.analysisJson || ''
  ElMessage.success('解读完成')
  loadLits()
}

async function genTopic() {
  loadingGen.value = true
  try {
    const { data } = await http.post(`/api/projects/${props.projectId}/topics/generate`, {
      direction: direction.value,
    })
    topicOut.value = data.outputJson
  } finally {
    loadingGen.value = false
  }
}

async function timely() {
  loadingTimely.value = true
  try {
    const { data } = await http.post(`/api/projects/${props.projectId}/topics/timely`, { theme: theme.value })
    topicOut.value = data.outputJson
  } finally {
    loadingTimely.value = false
  }
}

/** 解析 AI 返回 JSON 中的 edcPseudoHeaders */
function extractPseudoHeaders(jsonStr: string) {
  pseudoHeaders.value = []
  try {
    const arr = JSON.parse(jsonStr)
    if (!Array.isArray(arr) || !arr.length) return
    const first = arr[0]
    const raw = first.edcPseudoHeaders ?? first.edc_pseudo_headers
    if (Array.isArray(raw)) {
      pseudoHeaders.value = raw.map(String)
    }
  } catch {
    /* 非 JSON 或字段缺失则忽略 */
  }
}

async function runAffairs() {
  loadingAffairs.value = true
  try {
    const { data } = await http.post(`/api/projects/${props.projectId}/topics/current-affairs`, {
      theme: affairsTheme.value,
      frequency: affairsFreq.value,
      dataFields: affairsFields.value,
      edcHint: affairsEdc.value,
    })
    affairsOut.value = data.outputJson || ''
    extractPseudoHeaders(affairsOut.value)
    ElMessage.success('时政选题已生成')
  } finally {
    loadingAffairs.value = false
  }
}

watch(
  () => props.projectId,
  () => {
    loadLits()
  }
)

onMounted(loadLits)
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
  line-height: 1.5;
}
.mt {
  margin-top: 12px;
}
.topic-tabs :deep(.el-tabs__content) {
  padding: 16px;
  background: #fff;
}
</style>
