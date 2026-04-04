<!--
  【实验三 · 任务三】数据采集与处理（对应 DEMO：EDC 保存、模拟录入、CSV 上传、清洗/一键处理/描述统计）
  - EDC：GET/POST /api/projects/:id/edc/forms，模拟录入 POST .../edc/forms/:formId/simulate-row
  - 数据集：上传 POST .../datasets，清洗分析/一键处理/描述统计见本页按钮绑定的 API
  折叠区「CSMAR / AkShare」为内置说明，帮助字段命名与真实数据源对齐，非调用外部付费接口。
-->
<template>
  <div>
    <div class="fin-page-title">数据与处理</div>
    <p class="fin-page-desc">
      EDC 设计可与 CSMAR、AkShare 字段对齐；清洗与统计结果用于论文「数据与方法」章节闭环。
    </p>

    <el-collapse v-model="openDocs" class="doc-collapse">
      <el-collapse-item title="内置参考：CSMAR 数据字典与常用字段（摘录）" name="csmar">
        <div class="doc-block">
          <p>
            <strong>说明：</strong>CSMAR（国泰安）提供上市公司财务、治理、行情等结构化数据。本模块「CSMAR
            字段」列用于在导出/合并时与官方字段名对齐，便于复现与团队协作。
          </p>
          <ul>
            <li><code>Stkcd</code> — 证券代码；<code>Trddt</code> — 交易日期</li>
            <li><code>Dnvaltrd</code> — 日个股交易金额；<code>Dretwd</code> — 考虑现金红利的日收益率</li>
            <li><code>ShortName</code> — 证券简称；年报库中常见 <code>Accper</code> 会计期间</li>
          </ul>
          <p class="muted">完整字典请使用贵校/机构购买的 CSMAR 官方文档；此处仅为界面内置提示。</p>
        </div>
      </el-collapse-item>
      <el-collapse-item title="内置参考：AkShare 接口与字段（摘录）" name="akshare">
        <div class="doc-block">
          <p>
            <strong>说明：</strong>AkShare 为 Python 开源金融数据接口，适合补充宏观、期货、部分实时行情。
            建议在<strong>云服务器</strong>安装 Python 3.10+ 后调用（见下方「统计分析环境」）。
          </p>
          <ul>
            <li>示例：<code>ak.stock_zh_a_hist()</code> — A 股历史行情；关注列如 <code>日期</code>、<code>收盘</code></li>
            <li>示例：<code>ak.macro_china_*</code> — 部分宏观序列（具体以 AkShare 文档为准）</li>
            <li>与 CSMAR 混用时，请统一代码格式、交易日历与货币单位</li>
          </ul>
          <p class="muted">详见 <a href="https://akshare.akfamily.xyz" target="_blank" rel="noopener">AkShare 文档</a>。</p>
        </div>
      </el-collapse-item>
    </el-collapse>

    <el-row :gutter="16" style="margin-top: 8px">
      <el-col :xs="24" :lg="14">
        <el-card shadow="never" header="EDC 表单设计（拖拽字段；映射 CSMAR / AkShare）">
          <el-space wrap>
            <el-button type="primary" :loading="saving" @click="saveForm">保存表单</el-button>
            <el-button @click="addField">添加字段</el-button>
          </el-space>
          <p class="hint">分享采集链接（公开，无需登录）：<code>{{ shareUrl }}</code></p>
          <p class="hint">
            <strong>伪数据：</strong>保存表单后点「模拟录入一条」，后端按当前表头生成一行<strong>虚构数值</strong>用于联调（非真实行情）。
          </p>
          <draggable v-model="fields" item-key="id" handle=".drag" class="fields">
            <template #item="{ element, index }">
              <div class="field-row">
                <el-icon class="drag" style="cursor: move"><Rank /></el-icon>
                <el-input v-model="element.name" placeholder="字段名（中文）" style="width: 130px" />
                <el-select v-model="element.type" style="width: 96px">
                  <el-option label="文本" value="text" />
                  <el-option label="数字" value="number" />
                  <el-option label="日期" value="date" />
                </el-select>
                <el-input v-model="element.csmarField" placeholder="CSMAR 字段" style="width: 120px" />
                <el-input v-model="element.akshareField" placeholder="AkShare 列/键" style="width: 120px" />
                <el-button text type="danger" @click="fields.splice(index, 1)">删</el-button>
              </div>
            </template>
          </draggable>
          <el-divider />
          <el-space wrap>
            <el-button :disabled="!currentFormId" type="success" plain @click="simulate">模拟录入一条（伪数据）</el-button>
            <el-button @click="loadForms">刷新表单列表</el-button>
          </el-space>
          <el-table :data="forms" size="small" style="margin-top: 12px" @row-click="selectForm">
            <el-table-column prop="name" label="名称" />
            <el-table-column prop="shareToken" label="Token" show-overflow-tooltip />
          </el-table>
        </el-card>
      </el-col>
      <el-col :xs="24" :lg="10">
        <el-card shadow="never" header="数据上传 / 清洗 / 描述统计">
          <el-upload :action="dsUrl" :headers="headers" name="file" :show-file-list="false" :on-success="onDs">
            <el-button type="primary">上传 CSV / Excel</el-button>
          </el-upload>
          <el-table :data="datasets" size="small" style="margin-top: 8px">
            <el-table-column prop="fileName" label="文件" />
            <el-table-column label="操作" width="220">
              <template #default="{ row }">
                <el-button link type="primary" @click="cleanAnalyze(row)">清洗分析</el-button>
                <el-button link @click="cleanApply(row)">一键处理</el-button>
                <el-button link @click="desc(row)">描述统计</el-button>
              </template>
            </el-table-column>
          </el-table>
          <el-divider />
          <div v-if="chartDom" ref="chartRef" class="chart" />
          <el-input v-model="statJson" type="textarea" :rows="8" readonly placeholder="统计/清洗报告 JSON" />
        </el-card>

        <el-card shadow="never" class="mt" header="统计分析环境（云服务器，可选）">
          <p class="hint">
            团队可在云主机（Ubuntu 22.04 等）部署 Python + R + Jupyter，与本平台导出的 CSV 对接；下列为推荐环境变量与示例命令（仅文档，不自动执行）。
          </p>
          <el-input :model-value="cloudEnvSnippet" type="textarea" :rows="12" readonly class="mono" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { computed, nextTick, onMounted, ref, watch } from 'vue'
import draggable from 'vuedraggable'
import { Rank } from '@element-plus/icons-vue'
import * as echarts from 'echarts'
import http from '@/api/http'
import { useAuthStore } from '@/stores/auth'
import { ElMessage } from 'element-plus'

const props = defineProps<{ projectId: number }>()
const auth = useAuthStore()

const openDocs = ref<string[]>(['csmar'])

/** 云环境说明模板（开发/运维可复制到服务器） */
const cloudEnvSnippet = `# Ubuntu 云服务器 — 金融统计分析示例环境
# Python 3.10+ 建议；以下命令需自行在 SSH 会话中执行

sudo apt update && sudo apt install -y python3-pip python3-venv r-base
python3 -m venv ~/finenv && source ~/finenv/bin/activate
pip install pandas numpy statsmodels matplotlib jupyter akshare openpyxl

# 可选：与团队约定统一数据目录
export FIN_DATA_ROOT=/var/data/finresearch
export JUPYTER_TOKEN=请改为强随机

# 启动 Jupyter（示例，生产请配置 systemd + HTTPS 反代）
# jupyter lab --ip=0.0.0.0 --port=8888 --no-browser

# 本平台 API 基址（前端开发时由 Vite 代理到后端）
# 浏览器访问前端 http://localhost:5173 ，后端默认 http://localhost:8080
`

const fields = ref<any[]>([])
const forms = ref<any[]>([])
const currentFormId = ref<number | null>(null)
const shareToken = ref('')
const saving = ref(false)
const datasets = ref<any[]>([])
const statJson = ref('')
const chartRef = ref<HTMLElement | null>(null)
const chartDom = ref(false)

let chart: echarts.ECharts | null = null

const shareUrl = computed(() => {
  const t = shareToken.value
  if (!t) return '（先保存表单）'
  return `${location.origin}/api/edc/public/${t}`
})

const dsUrl = computed(() => `/api/projects/${props.projectId}/datasets`)
const headers = computed(() => ({ Authorization: 'Bearer ' + auth.token }))

function addField() {
  fields.value.push({
    id: crypto.randomUUID(),
    name: '日收益率',
    type: 'number',
    csmarField: 'Dretwd',
    akshareField: 'pct_chg',
  })
}

async function loadForms() {
  const { data } = await http.get(`/api/projects/${props.projectId}/edc/forms`)
  forms.value = data
  if (data.length && !currentFormId.value) {
    selectForm(data[0])
  }
}

function selectForm(row: any) {
  currentFormId.value = row.id
  shareToken.value = row.shareToken
  try {
    const sch = JSON.parse(row.schemaJson || '[]')
    if (Array.isArray(sch) && sch.length > 0) {
      fields.value = sch
    }
  } catch {
    /* ignore */
  }
}

async function saveForm() {
  saving.value = true
  try {
    const schemaJson = JSON.stringify(fields.value)
    if (currentFormId.value) {
      await http.put(`/api/projects/${props.projectId}/edc/forms/${currentFormId.value}`, {
        name: '金融研究 EDC',
        schemaJson,
      })
    } else {
      const { data } = await http.post(`/api/projects/${props.projectId}/edc/forms`, {
        name: '金融研究 EDC',
        schemaJson,
      })
      currentFormId.value = data.id
      shareToken.value = data.shareToken
    }
    ElMessage.success('已保存')
    loadForms()
  } finally {
    saving.value = false
  }
}

async function simulate() {
  await http.post(`/api/projects/${props.projectId}/edc/forms/${currentFormId.value}/simulate-row`)
  ElMessage.success('已生成一行伪数据（后端随机）')
}

async function loadDs() {
  const { data } = await http.get(`/api/projects/${props.projectId}/datasets`)
  datasets.value = data
}

function onDs(res: any) {
  if (res && res.code !== undefined && res.code !== 200) {
    ElMessage.error(res.message || '上传失败')
    return
  }
  ElMessage.success('数据集已上传（加密存储）')
  loadDs()
}

async function cleanAnalyze(row: any) {
  const { data } = await http.post(`/api/projects/${props.projectId}/datasets/${row.id}/clean/analyze`)
  statJson.value = data.cleanReportJson || ''
}

async function cleanApply(row: any) {
  await http.post(`/api/projects/${props.projectId}/datasets/${row.id}/clean/apply`)
  ElMessage.success('已生成清洗后文件')
  loadDs()
}

async function desc(row: any) {
  const { data } = await http.post(`/api/projects/${props.projectId}/datasets/${row.id}/stats/descriptive`)
  statJson.value = data.resultJson
  chartDom.value = true
  await nextTick()
  renderChart(data.chartSpecJson)
}

function renderChart(specStr: string) {
  if (!chartRef.value) return
  try {
    const spec = JSON.parse(specStr || '[]')
    const first = spec[0]
    if (!first || first.type !== 'scatter') return
    if (!chart) chart = echarts.init(chartRef.value)
    const pts = first.data || []
    chart.setOption({
      title: { text: '散点（前两列）', left: 'center', textStyle: { fontSize: 12 } },
      xAxis: { scale: true },
      yAxis: { scale: true },
      series: [{ type: 'scatter', data: pts.map((p: any) => [p.x, p.y]) }],
    })
  } catch {
    /* ignore */
  }
}

watch(
  () => props.projectId,
  () => {
    loadForms()
    loadDs()
  }
)

onMounted(() => {
  addField()
  loadForms()
  loadDs()
})
</script>

<style scoped>
.hint {
  font-size: 12px;
  color: #64748b;
  margin-top: 8px;
  line-height: 1.55;
  word-break: break-all;
}
.fields {
  margin-top: 12px;
}
.field-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
  padding: 8px;
  background: #f8fafc;
  border-radius: 6px;
}
.chart {
  height: 220px;
  margin-bottom: 12px;
}
.mt {
  margin-top: 14px;
}
.doc-collapse {
  margin-bottom: 8px;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 0 12px;
  background: #fff;
}
.doc-block {
  font-size: 13px;
  line-height: 1.65;
  color: #334155;
}
.doc-block ul {
  margin: 8px 0 0 18px;
}
.doc-block .muted {
  color: #94a3b8;
  font-size: 12px;
  margin-top: 8px;
}
.mono :deep(textarea) {
  font-family: ui-monospace, monospace;
  font-size: 12px;
}
</style>
