<!--
  【实验三 · 任务五】投稿回修（对应 DEMO：粘贴审稿意见 → AI 拆解；单条 JSON → 修改建议；回复信；版本列表与回滚）
  - 解析：POST /api/projects/:id/revisions/parse（得到 revId）
  - 建议/回复信：POST .../revisions/:revId/suggest | .../response-letter
  - 版本：GET .../paper/versions，回滚 POST .../paper/rollback/:versionNo
-->
<template>
  <div class="panel">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="审稿意见解析">
          <el-input v-model="comments" type="textarea" :rows="12" placeholder="粘贴审稿人意见" />
          <el-button type="primary" style="margin-top: 8px" @click="parse">AI 拆解</el-button>
          <el-input v-model="parsed" type="textarea" :rows="10" readonly style="margin-top: 12px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="逐条修改建议与回复信">
          <el-input v-model="itemJson" type="textarea" :rows="6" placeholder='单条意见 JSON，如 {"id":1,"summary":"..."}' />
          <el-button style="margin-top: 8px" @click="suggest">生成修改建议</el-button>
          <el-input v-model="suggestOut" type="textarea" :rows="8" readonly style="margin-top: 8px" />
          <el-divider />
          <el-input v-model="editsJson" type="textarea" :rows="4" placeholder="修改摘要 JSON 数组（可选）" />
          <el-button type="primary" style="margin-top: 8px" @click="letter" :disabled="!revId">生成回复信</el-button>
          <el-input v-model="letterOut" type="textarea" :rows="10" readonly style="margin-top: 8px" />
        </el-card>
        <el-card header="版本" style="margin-top: 16px">
          <el-button @click="loadVers">加载论文版本列表</el-button>
          <el-table :data="versions" size="small" style="margin-top: 8px">
            <el-table-column prop="versionNo" label="版本" width="70" />
            <el-table-column prop="note" label="说明" />
            <el-table-column label="操作" width="100">
              <template #default="{ row }">
                <el-button link type="primary" @click="rollback(row.versionNo)">回滚</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { onMounted, ref } from 'vue'
import http from '@/api/http'
import { ElMessage } from 'element-plus'

const props = defineProps<{ projectId: number }>()
const comments = ref(
  '1. 识别策略较弱，可能存在遗漏变量。\n2. 稳健性检验不足。\n3. 写作结构需加强，结论过长。'
)
const parsed = ref('')
const revId = ref<number | null>(null)
const itemJson = ref('{"id":1,"category":"识别","summary":"识别策略较弱","severity":"高"}')
const suggestOut = ref('')
const editsJson = ref('[{"id":1,"done":"补充工具变量"}]')
const letterOut = ref('')
const versions = ref<any[]>([])
const paperContent = ref('')

async function loadPaper() {
  const { data } = await http.get(`/api/projects/${props.projectId}/paper`)
  paperContent.value = data.content || ''
}

async function parse() {
  const { data } = await http.post(`/api/projects/${props.projectId}/revisions/parse`, {
    comments: comments.value,
    paperId: null,
  })
  parsed.value = data.parsedJson
  revId.value = data.id
  ElMessage.success('已解析')
}

async function suggest() {
  if (!revId.value) {
    ElMessage.warning('请先解析审稿意见')
    return
  }
  const { data } = await http.post(`/api/projects/${props.projectId}/revisions/${revId.value}/suggest`, {
    paperContent: paperContent.value,
    commentItemJson: itemJson.value,
  })
  suggestOut.value = data.json
}

async function letter() {
  if (!revId.value) return
  const { data } = await http.post(`/api/projects/${props.projectId}/revisions/${revId.value}/response-letter`, {
    editsJson: editsJson.value,
  })
  letterOut.value = data.responseLetter || ''
  ElMessage.success('回复信已生成')
}

async function loadVers() {
  const { data } = await http.get(`/api/projects/${props.projectId}/paper/versions`)
  versions.value = data
}

async function rollback(v: number) {
  await http.post(`/api/projects/${props.projectId}/paper/rollback`, { versionNo: v })
  ElMessage.success('已回滚')
  loadPaper()
}

onMounted(loadPaper)
</script>

<style scoped>
.panel {
  padding: 4px 0;
}
</style>
