<template>
  <div class="parse-workspace">
    <div class="input-area">
      <el-input
        v-model="script"
        type="textarea"
        :rows="6"
        placeholder="请粘贴你的单集剧本正文..."
        @keydown.ctrl.enter="parse"
      />
      <el-button class="btn-gradient btn-parse" @click="parse" :loading="loading" style="margin-top: 10px">
        拆解 5分
      </el-button>
    </div>

    <!-- 失败提示弹窗（必须手动关闭） -->
     <el-dialog
      v-model="showErrorModal"
      title="操作失败"
      width="400px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
    >
      <div style="font-size: 14px; padding: 10px 0;">
        {{ errorMessage }}
      </div>
      <template #footer>
        <div style="text-align: right;">
          <el-button type="primary" @click="showErrorModal = false">
            确定
          </el-button>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, inject } from 'vue'
import { parseScript } from '@/api/script'
import { ElMessage } from 'element-plus'

const script = ref('')
const loading = ref(false)
const emit = defineEmits(['parsed'])
const refreshPoints = inject('refreshPoints')

// 失败弹窗相关变量
const showErrorModal = ref(false)
const errorMessage = ref('')

const parse = async () => {
  if (!script.value.trim() || loading.value) return  // 防止重复提交
  loading.value = true
  try {
    const res = await parseScript(script.value)
    if (res.data.code === 200) {
      emit('parsed', res.data.data)
      script.value = ''   // 新增：成功后清空输入框，防止误操作重复拆解
      ElMessage.success('拆解成功')
      refreshPoints()
    } else {
      errorMessage.value = res.data.msg || '拆解失败，请稍后重试'
      showErrorModal.value = true
    }
  } catch (err) {
    errorMessage.value = '拆解失败，请检查网络后重试'
    showErrorModal.value = true
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.parse-workspace {
  display: flex;
  flex-direction: column;
  gap: 20px;
}
.input-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  width: 100%;
}

.btn-gradient {
  align-self: flex-end;
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  border-radius: 999px !important;
  padding: 10px 28px !important;
  transition: all 0.2s ease !important;
}
/* 拆解模块 - 蓝青主题 */
.btn-parse {
  background: linear-gradient(135deg, #3b82f6, #22d3ee) !important;
  box-shadow: 0 4px 12px rgba(59,130,246,0.3) !important;
}
.btn-parse:hover {
  transform: scale(1.04) !important;
  box-shadow: 0 6px 20px rgba(59,130,246,0.4) !important;
}
</style>
