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
import { ref } from 'vue'
import { parseScript } from '@/api/script'
import { ElMessage } from 'element-plus'

// ========== Props ==========
const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
})

// ========== Emits ==========
const emit = defineEmits(['parsed'])

// ========== 响应式数据 ==========
const script = ref('')
const loading = ref(false)
const showErrorModal = ref(false)
const errorMessage = ref('')

// ========== 方法 ==========

// 拆解剧本
const parse = async () => {
  const content = script.value.trim()
  if (!content) {
    ElMessage.warning('请输入剧本内容')
    return
  }

  if (content.length < 50) {
    ElMessage.warning('剧本内容太短，请至少输入50字')
    return
  }

  loading.value = true
  try {
    const res = await parseScript(content)
    if (res.data.code === 200) {
      const parsedData = res.data.data
      // parsedData 结构：{ styleDeclaration, characters: [{name, description, characterPrompt}], storyboards: [{description, scenePrompt, detailedDescription, videoPrompt, characters}] }
      
      // 为每个 storyboard 补充必要字段
      const storyboards = (parsedData.storyboards || []).map((sb, index) => ({
        id: Date.now() + index,       // 生成唯一ID
        description: sb.description || '',
        scenePrompt: sb.scenePrompt || '',
        detailedDescription: sb.detailedDescription || '',
        videoPrompt: sb.videoPrompt || '',
        keyframePrompt: sb.detailedDescription || '',  // 关键帧提示词复用详细描述
        characters: sb.characters || [],
        sceneImageUrl: '',            // 待生成
        keyframeImageUrl: '',         // 待生成
        videoUrl: ''                  // 待生成
      }))

      // 通知 Home.vue
      emit('parsed', {
        characters: parsedData.characters || [],
        storyboards: storyboards,
        styleDeclaration: parsedData.styleDeclaration || ''
      })

      ElMessage.success('拆解成功')
    } else {
      showError(res.data.msg || '拆解失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '网络错误，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    loading.value = false
  }
}

// 显示错误弹窗（需手动关闭）
const showError = (msg) => {
  errorMessage.value = msg
  showErrorModal.value = true
}

// 暴露给父组件：允许 Home.vue 通过 ref 设置 script 内容
defineExpose({
  setScript: (text) => {
    script.value = text
  }
})
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
