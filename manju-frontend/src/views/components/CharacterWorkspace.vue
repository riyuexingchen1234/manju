<template>
  <div class="character-workspace">
    <div class="character-grid">
      <el-card
        v-for="(char, idx) in localCharacters"
        :key="idx"
        class="character-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <el-input
              v-model="char.name"
              placeholder="角色名"
              size="small"
              style="width: 120px"
            />
            <el-button
              type="danger"
              size="small"
              icon="Delete"
              circle
              class="delete-btn-sm"
              @click="removeCharacter(idx)"
            />
          </div>
        </template>

        <div class="card-content">
          <!-- 图片展示区 -->
          <div class="image-area">
            <el-image
              v-if="props.characterImages && props.characterImages[char.name]"
              :src="props.characterImages[char.name]"
              :preview-src-list="[props.characterImages[char.name]]"  
              fit="cover"
              class="character-image"
            />
            <div v-else class="image-placeholder">
              <span>未生成</span>
            </div>
          </div>

          <!-- 提示词输入框 -->
          <el-input
            v-model="char.characterPrompt"
            type="textarea"
            :rows="4"
            placeholder="请输入角色提示词"
            class="prompt-input"
          />

          <!-- 按钮组 -->
          <div class="button-group">
            <!-- 上传角色图按钮 -->
            <el-upload
              class="upload-btn"
              :show-file-list="false"
              :http-request="(options) => uploadImage(char, options)"
            >
              <el-button size="small" type="default">本地上传</el-button>
            </el-upload>
            <!-- 生成角色图按钮 -->
            <el-button
              class="btn-gradient btn-character"   
              @click="generateImage(char, idx)"  
              :loading="loadingStates[idx]"  
              size="small" 
            >
              生成 10分
            </el-button>
          </div>
        </div>
      </el-card>

      <!-- 添加新角色卡片 -->
      <el-card class="character-card add-card" shadow="hover" @click="addCharacter">
        <div class="add-content">
          <el-icon :size="40"><Plus /></el-icon>
          <span>添加新角色</span>
        </div>
      </el-card>
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
        <el-button type="primary" @click="showErrorModal = false">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { generateCharacter } from '@/api/character'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// ========== Props ==========
const props = defineProps({
  characters: {
    type: Array,
    default: () => []
  },
  characterImages: {
    type: Object,
    default: () => ({})
  },
  styleDeclaration: {
    type: String,
    default: ''
  },
  user: {
    type: Object,
    default: () => ({})
  }
})

// ========== Emits ==========
const emit = defineEmits(['character-generated', 'update-characters'])

// ========== 响应式数据 ==========
// 内部维护的角色列表（可编辑名字、提示词）
const localCharacters = ref([])

// 每个卡片的加载状态
const loadingStates = reactive({})

// 错误弹窗
const showErrorModal = ref(false)
const extractImageUrl = (data) => typeof data === 'string' ? data : data?.imageUrl || data
const errorMessage = ref('')

// ========== 初始化与同步 ==========
// 从 props.characters 初始化
const initFromProps = () => {
  if (props.characters && props.characters.length > 0) {
    localCharacters.value = props.characters.map(c => ({
      name: c.name || '',
      characterPrompt: c.characterPrompt || ''
    }))
  }
}

onMounted(() => {
  initFromProps()
})

// 当 props.characters 引用变化时同步（如拆解后传入新角色列表）
watch(() => props.characters, (newVal) => {
  if (newVal && newVal.length > 0) {
    // 仅当引用变化时更新（避免与内部修改形成循环）
    const newNames = newVal.map(c => c.name).join(',')
    const currentNames = localCharacters.value.map(c => c.name).join(',')
    if (newNames !== currentNames) {
      localCharacters.value = newVal.map(c => ({
        name: c.name || '',
        characterPrompt: c.characterPrompt || ''
      }))
    }
  }
}, { deep: true })

// 当 localCharacters 变更时通知 Home.vue
watch(localCharacters, (newVal) => {
  emit('update-characters', newVal.map(c => ({
    name: c.name,
    characterPrompt: c.characterPrompt
  })))
}, { deep: true })

// ========== 方法 ==========

// 添加新角色
const addCharacter = () => {
  localCharacters.value.push({
    name: '',
    characterPrompt: ''
  })
}

// 删除角色
const removeCharacter = (idx) => {
  localCharacters.value.splice(idx, 1)
}

// 生成角色图
const generateImage = async (char, idx) => {
  if (!char.characterPrompt.trim()) {
    ElMessage.warning('请先输入角色提示词')
    return
  }

  if (!char.name.trim()) {
    ElMessage.warning('请先输入角色名')
    return
  }

  loadingStates[idx] = true
  try {
    const res = await generateCharacter(
      char.name,
      char.characterPrompt,
      props.styleDeclaration || ''
    )
    if (res.data.code === 200) {
      const imageUrl = extractImageUrl(res.data.data)

      // 通知 Home.vue 更新 characterImages 映射
      emit('character-generated', char.name, imageUrl)
      ElMessage.success('角色图生成成功')
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '生成失败，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    loadingStates[idx] = false
  }
}

// 本地上传角色图
const uploadImage = (char, options) => {
  const file = options.file
  if (!file) return

  // 校验文件类型
  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }
  
  if (!char.name.trim()) {
    ElMessage.warning('请先输入角色名')
    return
  }

  // 转为 base64 或 object URL
  const reader = new FileReader()
  reader.onload = (e) => {
    const imageUrl = e.target.result
    emit('character-generated', char.name, imageUrl)
    ElMessage.success('上传成功')
  }
  reader.onerror = () => {
    ElMessage.error('上传失败')
  }
  reader.readAsDataURL(file)
}

// 显示错误弹窗
const showError = (msg) => {
  errorMessage.value = msg
  showErrorModal.value = true
}
</script>

<style scoped>
.character-workspace {
  padding: 10px 0;
}
.character-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
}
.character-card {
  width: 250px;
  margin-bottom: 10px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-content {
  display: flex;
  flex-direction: column;
  gap: 12px;
}
.image-area {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #f5f5f5;
  border-radius: 8px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.character-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  color: #999;
}
.prompt-input {
  width: 100%;
}
.button-group {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.upload-btn {
  display: inline-block;
}
.button-group .upload-btn,
.button-group .el-button {
  flex: 1;
  min-width: 0; /* 防止溢出 */
}
.add-card {
  cursor: pointer;
  display: flex;
  justify-content: center;
  align-items: center;
}
.add-content {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  gap: 12px;
  color: #409eff;
}

.delete-btn-sm{
  width: 18px !important;
  height: 18px !important;
  padding: 0 !important;
}
.delete-btn-sm .el-icon {
  font-size: 10px !important;
}

.btn-gradient {
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  border-radius: 999px !important;
  padding: none !important;
  transition: all 0.2s ease !important;
}
/* 角色模块 - 绿青主题 */
.btn-character {
  background: linear-gradient(135deg, #22c55e, #14b8a6) !important;
  box-shadow: 0 4px 12px rgba(34,197,94,0.3) !important;
}
.btn-character:hover {
  transform: scale(1.04) !important;
  box-shadow: 0 6px 20px rgba(34,197,94,0.4) !important;
}
</style>
