<!--
  【实验三 · 任务六】文档与合规（对应 DEMO：拉取内置 Markdown 说明；脱敏试算；操作审计列表）
  - 文档：GET /api/projects/:id/documents/:type/markdown
  - 脱敏：POST /api/compliance/desensitize（送模型前规则的本地演练）
  - 审计：GET /api/compliance/audit-logs
-->
<template>
  <div class="panel">
    <el-row :gutter="16">
      <el-col :span="12">
        <el-card header="自动文档（Markdown）">
          <el-space wrap>
            <el-button @click="loadMd('requirements')">需求说明书</el-button>
            <el-button @click="loadMd('database-selection')">数据库选型</el-button>
            <el-button @click="loadMd('tech-stack')">技术选型</el-button>
          </el-space>
          <el-input v-model="md" type="textarea" :rows="18" readonly style="margin-top: 12px" />
        </el-card>
      </el-col>
      <el-col :span="12">
        <el-card header="合规：脱敏试算 / 审计日志">
          <el-form label-position="top">
            <el-form-item label="脱敏试算（原文不上送模型前可先试运行）">
              <el-input v-model="raw" type="textarea" :rows="3" placeholder="可输入含手机、邮箱、身份证的样例" />
              <el-button style="margin-top: 8px" @click="tryDes">脱敏</el-button>
              <el-input v-model="desOut" type="textarea" :rows="3" readonly style="margin-top: 8px" />
            </el-form-item>
          </el-form>
          <el-button @click="loadAudit">刷新操作审计</el-button>
          <el-table :data="auditRows" size="small" style="margin-top: 8px">
            <el-table-column prop="action" label="动作" width="140" />
            <el-table-column prop="resourceType" label="资源" width="100" />
            <el-table-column prop="createdAt" label="时间" />
          </el-table>
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import http from '@/api/http'

const props = defineProps<{ projectId: number }>()
const md = ref('')
const raw = ref('联系人 13812345678 test@mail.com')
const desOut = ref('')
const auditRows = ref<any[]>([])

async function loadMd(type: string) {
  const { data } = await http.get(`/api/projects/${props.projectId}/documents/${type}/markdown`)
  md.value = data.markdown
}

async function tryDes() {
  const { data } = await http.post('/api/compliance/desensitize', {
    text: raw.value,
    sourceType: 'MANUAL_TEST',
  })
  desOut.value = data.desensitized + '\n命中: ' + JSON.stringify(data.hitTypes)
}

async function loadAudit() {
  const { data } = await http.get('/api/compliance/audit-logs', { params: { page: 0, size: 15 } })
  auditRows.value = data.content || []
}
</script>

<style scoped>
.panel {
  padding: 4px 0;
}
</style>
