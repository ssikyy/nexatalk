<!---- AI 管理 ---->
<template>
  <div class="ai-admin">
    <!-- 配置表单 -->
    <el-card class="config-card">
      <template #header>
        <div class="card-header">
          <span class="card-title">AI 配置管理</span>
          <el-button type="primary" :loading="saving" @click="saveConfig">
            <el-icon><Check /></el-icon>
            保存配置
          </el-button>
        </div>
      </template>

      <el-form :model="config" label-width="120px" class="config-form">
        <el-form-item label="开启 AI 功能">
          <el-switch v-model="config.enabled" />
          <div class="form-tip">关闭后 AI 相关功能将降级返回原始内容</div>
        </el-form-item>

        <el-divider content-position="left">API 配置</el-divider>

        <el-form-item label="API Base URL">
          <el-input
            v-model="config.baseUrl"
            placeholder="https://api.openai.com/v1"
            clearable
          />
          <div class="form-tip">兼容 OpenAI 规范的 API 地址，如通义千问、讯飞星火等</div>
        </el-form-item>

        <el-form-item label="API Key">
          <el-input
            v-model="config.apiKey"
            placeholder="请输入 API Key"
            show-password
            clearable
          />
          <div class="form-tip">用于调用 AI 接口的密钥</div>
        </el-form-item>

        <el-form-item label="模型名称">
          <el-input
            v-model="config.model"
            placeholder="qwen3.5-plus"
            clearable
          />
          <div class="form-tip">当前默认：qwen3.5-plus</div>
        </el-form-item>

        <el-form-item label="超时时间(ms)">
          <el-input-number
            v-model="config.timeoutMs"
            :min="5000"
            :max="120000"
            :step="5000"
          />
          <div class="form-tip">API 请求超时时间，默认 30000ms</div>
        </el-form-item>

        <el-divider content-position="left">功能测试</el-divider>

        <el-form-item label="测试润色功能">
          <div class="test-section">
            <el-input
              v-model="testContent"
              type="textarea"
              :rows="3"
              placeholder="请输入需要润色的文本..."
            />
            <el-button
              type="success"
              :loading="testingPolish"
              :disabled="!config.enabled || !testContent"
              @click="testPolish"
            >
              测试润色
            </el-button>
          </div>
          <div v-if="polishResult" class="test-result">
            <div class="result-label">润色结果：</div>
            <div class="result-content">{{ polishResult }}</div>
          </div>
        </el-form-item>

        <el-form-item label="测试 AI 对话">
          <div class="test-section">
            <el-input
              v-model="chatMessage"
              type="textarea"
              :rows="2"
              placeholder="请输入对话内容..."
            />
            <el-button
              type="success"
              :loading="testingChat"
              :disabled="!config.enabled || !chatMessage"
              @click="testChat"
            >
              发送对话
            </el-button>
          </div>
          <div v-if="chatResult" class="test-result">
            <div class="result-label">AI 回复：</div>
            <div class="result-content">{{ chatResult }}</div>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- API 配置说明 -->
    <el-card class="help-card">
      <template #header>
        <span class="card-title">常用 API 配置参考</span>
      </template>
      <el-table :data="helpData" stripe>
        <el-table-column prop="provider" label="服务商" />
        <el-table-column prop="baseUrl" label="Base URL" min-width="280" />
        <el-table-column prop="model" label="模型示例" />
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage } from 'element-plus'
import { Check } from '@element-plus/icons-vue'
import { getAiConfig, updateAiConfig, testPolishApi, testChatApi } from '@/api/ai'

const saving = ref(false)
const testingPolish = ref(false)
const testingChat = ref(false)

const config = ref({
  enabled: false,
  baseUrl: '',
  apiKey: '',
  model: '',
  timeoutMs: 30000
})

const testContent = ref('')
const polishResult = ref('')
const chatMessage = ref('')
const chatResult = ref('')

const helpData = ref([
  {
    provider: 'OpenAI 官方',
    baseUrl: 'https://api.openai.com/v1',
    model: 'gpt-4o-mini, gpt-4o'
  },
  {
    provider: '阿里云 DashScope',
    baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1',
    model: 'qwen3.5-plus, qwen-plus'
  },
  {
    provider: '阿里云 DashScope (国际版)',
    baseUrl: 'https://dashscope-intl.aliyuncs.com/compatible-mode/v1',
    model: 'qwen3.5-plus'
  },
  {
    provider: 'Moonshot (月之暗面)',
    baseUrl: 'https://api.moonshot.cn/v1',
    model: 'moonshot-v1-8k'
  },
  {
    provider: '智谱 AI',
    baseUrl: 'https://open.bigmodel.cn/api/paas/v4',
    model: 'glm-4'
  }
])

// 加载配置
const loadConfig = async () => {
  try {
    const res = await getAiConfig()
    if (res.data) {
      config.value = {
        enabled: res.data.enabled || false,
        baseUrl: res.data.baseUrl || '',
        apiKey: res.data.apiKey || '',
        model: res.data.model || '',
        timeoutMs: res.data.timeoutMs || 30000
      }
    }
  } catch (error) {
    console.error('加载 AI 配置失败:', error)
  }
}

// 保存配置
const saveConfig = async () => {
  saving.value = true
  try {
    const configList = [
      { key: 'ai_enabled', value: config.value.enabled ? 'true' : 'false', description: '是否开启 AI 功能' },
      { key: 'ai_base_url', value: config.value.baseUrl, description: 'AI API Base URL' },
      { key: 'ai_api_key', value: config.value.apiKey, description: 'AI API Key' },
      { key: 'ai_model', value: config.value.model, description: 'AI 模型名称' },
      { key: 'ai_timeout_ms', value: String(config.value.timeoutMs), description: 'AI 请求超时时间' }
    ]
    await updateAiConfig(configList)
    ElMessage.success('配置保存成功')
  } catch (error) {
    ElMessage.error('配置保存失败: ' + (error.message || '未知错误'))
  } finally {
    saving.value = false
  }
}

// 测试润色功能
const testPolish = async () => {
  testingPolish.value = true
  polishResult.value = ''
  try {
    const res = await testPolishApi({ content: testContent.value })
    polishResult.value = res.data || ''
  } catch (error) {
    ElMessage.error('测试失败: ' + (error.message || '未知错误'))
    polishResult.value = '测试失败，请检查配置是否正确'
  } finally {
    testingPolish.value = false
  }
}

// 测试 AI 对话
const testChat = async () => {
  testingChat.value = true
  chatResult.value = ''
  try {
    const res = await testChatApi({ message: chatMessage.value })
    chatResult.value = res.data || ''
  } catch (error) {
    ElMessage.error('测试失败: ' + (error.message || '未知错误'))
    chatResult.value = '测试失败，请检查配置是否正确'
  } finally {
    testingChat.value = false
  }
}

onMounted(() => {
  loadConfig()
})
</script>

<style scoped>
.ai-admin {
  padding: 0;
}

.config-card {
  margin-bottom: 20px;
}

.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.card-title {
  font-size: 16px;
  font-weight: 600;
}

.config-form {
  max-width: 800px;
}

.form-tip {
  font-size: 12px;
  color: #909399;
  margin-top: 4px;
}

.test-section {
  display: flex;
  gap: 12px;
  align-items: flex-start;
}

.test-section .el-input,
.test-section .el-textarea {
  flex: 1;
}

.test-result {
  margin-top: 12px;
  padding: 12px;
  background: #f5f7fa;
  border-radius: 4px;
}

.result-label {
  font-size: 12px;
  color: #909399;
  margin-bottom: 8px;
}

.result-content {
  font-size: 14px;
  color: #303133;
  white-space: pre-wrap;
  word-break: break-word;
}

.help-card .card-title {
  font-size: 16px;
  font-weight: 600;
}
</style>
