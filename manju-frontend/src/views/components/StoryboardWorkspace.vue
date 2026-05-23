<template>
  <div class="storyboard-workspace">
    <!-- 工具栏 -->
    <div class="storyboard-toolbar">
      <div class="actions">
        <el-button type="primary" @click="previewVideo" :disabled="!hasVideos">
          预览所有子视频
        </el-button>
        <el-button type="success" @click="downloadVideos" :disabled="!hasVideos">
          合并下载
        </el-button>
      </div>
    </div>

    <!-- 分镜列表 -->
    <div class="storyboard-list">
      <el-card
        v-for="(story, idx) in localStoryboards"
        :key="story.id"
        class="storyboard-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <span>分镜 {{ idx + 1 }}</span>
            <el-button
              type="danger"
              size="small"
              icon="Delete"
              circle
              @click="removeStoryboard(idx)"
            />
          </div>
        </template>

        <div class="card-content">
          <!-- 分镜描述 -->
          <div class="block">
            <div class="block-title">分镜描述</div>
            <el-input
              v-model="story.description"
              type="textarea"
              :rows="6"
              placeholder="分镜描述"
            />
          </div>

          <!-- 涉及角色 -->
          <div class="block">
            <div class="block-title">涉及角色</div>
            <el-select
              v-model="story.characters"
              multiple
              filterable
              allow-create
              default-first-option
              placeholder="选择或创建角色"
            >
              <el-option
                v-for="char in characters"
                :key="char.name || char.id"
                :label="char.name || '未命名'"
                :value="char.name || '未命名'"
              />
            </el-select>
          </div>

          <!-- 场景生成 -->
          <div class="block">
            <div class="block-title">场景图</div>
            <div class="image-box">
              <el-image
                v-if="story.sceneImageUrl" 
                :src="story.sceneImageUrl" 
                :preview-src-list="[story.sceneImageUrl]"
                fit="cover"
                class="block-image" />
              <div v-else class="image-placeholder">未生成</div>
            </div>
            <el-input
              v-model="story.scenePrompt"
              type="textarea"
              :rows="6"
              placeholder="场景提示词"
              class="prompt-input"
            />
            <div class="button-group">
              <el-upload
                class="upload-btn"
                :show-file-list="false"
                :http-request="(options) => uploadScene(idx, options)"
              >
                <el-button size="small" type="default">本地上传</el-button>
              </el-upload>
              <el-button
                type="primary"
                @click="generateScene(idx)"
                :loading="sceneLoading[idx]"
                size="small"
              >
                生成场景 10分
              </el-button>
            </div>
          </div>

          <!-- 关键帧生成 -->
          <div class="block">
            <div class="block-title">关键帧</div>
            <div class="image-box">
              <el-image
               v-if="story.keyframeImageUrl" 
               :src="story.keyframeImageUrl" 
               :preview-src-list="[story.keyframeImageUrl]"
               fit="cover"
               class="block-image" />
              <div v-else class="image-placeholder">未生成</div>
            </div>
            <el-input
              v-model="story.keyframePrompt"
              type="textarea"
              :rows="6"
              placeholder="关键帧提示词"
              class="prompt-input"
            />
            <el-button
              type="primary"
              @click="generateKeyframe(idx)"
              :loading="keyframeLoading[idx]"
              size="small"
              :disabled="!story.sceneImageUrl"
            >
              生成关键帧 10分
            </el-button>
            <div class="hint" v-if="!story.sceneImageUrl">
              需场景图
            </div>
          </div>

          <!-- 视频生成 -->
          <div class="block">
            <div class="block-title">视频</div>
            <div class="image-box" >
              <video v-if="story.videoUrl" controls style="width:100%; height:100%;" >
                <source :src="story.videoUrl" type="video/mp4" />
              </video>
              <div v-else class="image-placeholder">未生成</div>
            </div>
            <el-input
              v-model="story.videoPrompt"
              type="textarea"
              :rows="6"
              placeholder="视频提示词（描述镜头运动、动态等）"
              class="prompt-input"
            />
            <div class="button-group">
              <el-button
                type="primary"
                @click="generateVideo(idx)"
                :loading="videoLoading[idx]"
                size="small"
                :disabled="!story.keyframeImageUrl || !story.videoPrompt.trim()"
              >
                生成视频 20分
              </el-button>
            </div>
            <div class="hint" v-if="!story.keyframeImageUrl">
              需先有关键帧
            </div>
            <div class="hint" v-else-if="!story.videoPrompt.trim()">
              请输入视频提示词
            </div>
          </div>
        </div>
      </el-card>
    </div>

    <div class="add-button-container">
      <el-button  @click="addStoryboard" class="add-storyboard-btn">+ 添加分镜</el-button>
    </div>

    <!-- 视频预览对话框 -->
    <el-dialog
      v-model="previewDialogVisible"
      title="视频预览"
      width="50%"
      :close-on-click-modal="false"
      @close="stopPreview"
    >
      <video
        ref="previewPlayerRef"
        controls
        style="width: 100%; height: auto;"
      ></video>
    </el-dialog>

    <!-- 失败提示弹窗 -->
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
import { ref, reactive, watch, onMounted, onBeforeUnmount, computed, nextTick } from 'vue'
import { generateScene as generateSceneApi } from '@/api/scene'
import { generateKeyframe as generateKeyframeApi } from '@/api/keyframe'
import { createVideoTask, queryVideoTask } from '@/api/video'
import { ElMessage } from 'element-plus'
import JSZip from 'jszip'
import { saveAs } from 'file-saver'

// ========== Props ==========
const props = defineProps({
  storyboards: {
    type: Array,
    default: () => []
  },
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
const emit = defineEmits(['update-storyboards', 'generated'])

// ========== 响应式数据 ==========
const localStoryboards = ref([])
const showErrorModal = ref(false)

const errorMessage = ref('')

// 视频预览
const previewDialogVisible = ref(false)
const previewPlayerRef = ref(null)
const currentPreviewIndex = ref(0)
const sceneLoading = ref({})
const keyframeLoading = ref({})
const videoLoading = ref({})
const extractImageUrl = (data) => typeof data === 'string' ? data : data?.imageUrl || data
const createEmptyStoryboard = () => ({
  id: Date.now(),
  description: '',
  scenePrompt: '',
  detailedDescription: '',
  videoPrompt: '',
  keyframePrompt: '',
  characters: [],
  sceneImageUrl: '',
  keyframeImageUrl: '',
  videoUrl: ''
})
// 视频轮询定时器集合
const pollTimers = {}

// ========== 计算属性 ==========
const hasVideos = computed(() => {
  return localStoryboards.value.some(s => s.videoUrl)
})

// ========== 初始化与同步 ==========
const initFromProps = () => {
  if (props.storyboards && props.storyboards.length > 0) {
    localStoryboards.value = props.storyboards.map(s => ({ ...s }))
  }
}

onMounted(() => {
  initFromProps()
})

// 当 props.storyboards 引用变化时同步（如拆解后传入新分镜列表）
watch(() => props.storyboards, (newVal) => {
  if (newVal && newVal.length > 0) {
    const newIds = newVal.map(s => s.id).join(',')
    const currentIds = localStoryboards.value.map(s => s.id).join(',')
    if (newIds !== currentIds) {
      localStoryboards.value = newVal.map(s => ({ ...s }))
    }
  }
}, { deep: true })

// localStoryboards 变更时通知 Home.vue 写入 localStorage
watch(localStoryboards, (newVal) => {
  emit('update-storyboards', newVal.map(s => ({ ...s })))
}, { deep: true })

// 组件卸载时清除所有轮询定时器
onBeforeUnmount(() => {
  Object.keys(pollTimers).forEach(storyId => {
    clearInterval(pollTimers[storyId])
    delete pollTimers[storyId]
  })
})

// ========== 方法 ==========

// 添加分镜
const addStoryboard = () => {
  localStoryboards.value.push(createEmptyStoryboard())
}

// 删除分镜
const removeStoryboard = (idx) => {
  const story = localStoryboards.value[idx]
  // 清除该分镜可能正在运行的视频轮询
  if(story?.id && pollTimers[story.id]){
    clearInterval(pollTimers[story.id])
    delete pollTimers[story.id]
  }
  // 释放可能残留的 loading 状态
  if(videoLoading.value[idx] !== undefined){
    delete videoLoading.value[idx]
  }
  localStoryboards.value.splice(idx, 1)
}

// 生成场景图
const generateScene = async (idx) => {
  const story = localStoryboards.value[idx]
  if (!story.scenePrompt?.trim()) {
    ElMessage.warning('请先输入场景提示词')
    return
  }

  sceneLoading.value[idx] = true
  try {
    const res = await generateSceneApi(story.scenePrompt, props.styleDeclaration || '')
    if (res.data.code === 200) {
      const url = extractImageUrl(res.data.data)
      story.sceneImageUrl = url
      ElMessage.success('场景图生成成功')
      emit('generated')
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '生成失败，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    sceneLoading.value[idx] = false
  }
}

// 生成关键帧
const generateKeyframe = async (idx) => {
  const story = localStoryboards.value[idx]

  if (!story.sceneImageUrl) {
    ElMessage.warning('请先生成场景图')
    return
  }

  // 收集该分镜涉及角色的已生成图片
  const charUrls = (story.characters || [])
    .map(name => props.characterImages[name])
    .filter(Boolean)

  keyframeLoading.value[idx] = true
  try {
    const res = await generateKeyframeApi(
      story.keyframePrompt || story.detailedDescription || '',
      charUrls,
      story.sceneImageUrl
    )
    if (res.data.code === 200) {
      const url = extractImageUrl(res.data.data)
      story.keyframeImageUrl = url
      ElMessage.success('关键帧生成成功')
      emit('generated')
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '生成失败，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    keyframeLoading.value[idx] = false
  }
}

// 生成视频（异步）
const generateVideo = async (idx) => {
  const story = localStoryboards.value[idx]
  // 清除该分镜可能正在进行的旧轮询和旧视频
  if (story?.id && pollTimers[story.id]) {
    clearInterval(pollTimers[story.id])
    delete pollTimers[story.id]
  }

  if (!story.keyframeImageUrl) {
    ElMessage.warning('请先生成关键帧')
    return
  }
  
  videoLoading.value[idx] = true
  try {
    const res = await createVideoTask(story.keyframeImageUrl, story.videoPrompt)
    if (res.data.code === 200) {
      const data = res.data.data
      const taskId = data.taskId || data.task_id
      if (taskId) {
        startPolling(idx, taskId)
        ElMessage.info('视频生成任务已提交，正在后台生成...')
      }
    } else {
      showError(res.data.msg || '提交失败')
      videoLoading.value[idx] = false
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '提交失败，请稍后重试'
    showError(msg)
    console.error(err)
    videoLoading.value[idx] = false
  }
}

// 轮询视频任务结果（每15秒一次，最多80次，即20分钟）
const startPolling = (idx, taskId) => {
  const story = localStoryboards.value[idx]
  const storyId = story?.id
  if(!storyId) return

  if (pollTimers[storyId]) {
    clearInterval(pollTimers[storyId])
    delete pollTimers[storyId]
  }

  let pollCount = 0
  const MAX_POLL = 80

  const timer = setInterval(async () => {
    pollCount++
    if (pollCount > MAX_POLL) {
      clearInterval(timer)
      delete pollTimers[storyId]
      videoLoading.value[idx] = false
      showError('视频生成超时，请稍后重试')
      return
    }

    try {
      const res = await queryVideoTask(taskId)
      if (res.data.code === 200) {
        const result = res.data.data
        if (result.status === 'SUCCEEDED') {
          clearInterval(timer)
          delete pollTimers[storyId]
          // 通过 storyId 找到当前分镜位置（可能因删除/排序而改变）
          const targetIdx = localStoryboards.value.findIndex(s => s.id === storyId)
          if (targetIdx !== -1) {
            const updated = { ...localStoryboards.value[targetIdx], videoUrl: result.videoUrl || result.video_url }
            localStoryboards.value.splice(targetIdx, 1, updated)
          }

          videoLoading.value[idx] = false
          ElMessage.success('视频生成完成')
          emit('generated')
        } else if (result.status === 'FAILED') {
          clearInterval(timer)
          delete pollTimers[storyId]
          videoLoading.value[idx] = false
          showError(result.error || '视频生成失败')
        }
      }
    } catch (err) {
      console.error('查询视频任务失败:', err)
    }
  }, 15000)

  pollTimers[storyId] = timer
}

// 本地上传场景图
const uploadScene = (idx, options) => {
  const file = options.file
  if (!file) return

  if (!file.type.startsWith('image/')) {
    ElMessage.error('请上传图片文件')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    localStoryboards.value[idx].sceneImageUrl = e.target.result
    ElMessage.success('上传成功')
  }
  reader.onerror = () => {
    ElMessage.error('上传失败')
  }
  reader.readAsDataURL(file)
}

// 预览所有视频
const previewVideo = async () => {
  const videoIndices = localStoryboards.value
    .map((s, i) => s.videoUrl ? i : -1)
    .filter(i => i !== -1)

  if (videoIndices.length === 0) {
    ElMessage.warning('没有可预览的视频')
    return
  }

  previewDialogVisible.value = true
  await nextTick()                    // 等待对话框和视频元素渲染
  currentPreviewIndex.value = 0
  playVideoAtIndex(videoIndices[0])
}

const playVideoAtIndex = (idx) => {
  const story = localStoryboards.value[idx]
  if (!story?.videoUrl || !previewPlayerRef.value) return

  previewPlayerRef.value.src = story.videoUrl
  previewPlayerRef.value.load()
  previewPlayerRef.value.play()

  // 监听播放结束，自动切下一个
  previewPlayerRef.value.onended = () => {
    // 找下一个有视频的分镜
    let nextIdx = idx + 1
    while (nextIdx < localStoryboards.value.length) {
      if (localStoryboards.value[nextIdx].videoUrl) {
        currentPreviewIndex.value = nextIdx
        playVideoAtIndex(nextIdx)
        return
      }
      nextIdx++
    }
    // 播放完毕，清除监听，提示用户
    previewPlayerRef.value.onended = null
    ElMessage.success('所有视频播放完毕')
  }
}

// 停止预览
const stopPreview = () => {
  if (previewPlayerRef.value) {
    previewPlayerRef.value.onended = null  // 清除监听
    previewPlayerRef.value.pause()
    previewPlayerRef.value.src = ''
  }
}

// 合并下载所有视频（单个视频失败时跳过，其余正常打包）
const downloadVideos = async () => {
  const videos = localStoryboards.value
    .map((s, i) => ({ url: s.videoUrl, index: i + 1 }))
    .filter(v => v.url)

  if (videos.length === 0) {
    ElMessage.warning('没有可下载的视频')
    return
  }

  ElMessage.info(`正在打包 ${videos.length} 个视频，请稍候...`)

  const zip = new JSZip()
  const failedList = []

  for (const video of videos) {
    try {
      const response = await fetch(video.url)
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      const blob = await response.blob()
      zip.file(`分镜${video.index}_视频.mp4`, blob)
    } catch (err) {
      console.error(`分镜${video.index} 下载失败:`, err)
      failedList.push(video.index)
    }
  }

  // 如果全部失败
  if (failedList.length === videos.length) {
    ElMessage.error('所有视频下载失败，请检查网络')
    return
  }

  // 如果有部分失败，提示用户
  if (failedList.length > 0) {
    ElMessage.warning(`分镜 ${failedList.join('、')} 下载失败，其余已打包`)
  }

  try {
    const zipBlob = await zip.generateAsync({ type: 'blob' })
    saveAs(zipBlob, '漫剧分镜视频合集.zip')
    ElMessage.success('下载完成')
  } catch (err) {
    console.error('打包下载失败:', err)
    ElMessage.error('打包失败，请重试')
  }
}

// 显示错误弹窗
const showError = (msg) => {
  errorMessage.value = msg
  showErrorModal.value = true
}
</script>

<style scoped>
.storyboard-workspace {
  width: 100%;
}
.storyboard-list {
  display: flex;
  flex-direction: column;
  gap: 24px;
}
.storyboard-card {
  width: 100%;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.card-content {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  justify-content: space-between;
}
.block {
  flex: 1;
  min-width: 150px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}
.block-title {
  font-weight: 500;
  font-size: 14px;
  color: #606266;
  margin-bottom: 4px;
}
.image-box {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #f5f5f5;
  border-radius: 4px;
  display: flex;
  justify-content: center;
  align-items: center;
  overflow: hidden;
}
.block-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}
.image-placeholder {
  color: #999;
  font-size: 12px;
}
.prompt-input {
  margin-bottom: 4px;
}
.hint {
  font-size: 12px;
  color: #e6a23c;
  margin-top: 4px;
}
.add-button-container {
  margin-top: 20px;
  text-align: center;
}
.add-storyboard-btn{
  display:inline-flex;
  align-items: center;
  justify-content: center;
}
.button-group {
  display: flex;
  gap: 8px;
  margin-top: 8px;
}
.button-group .upload-btn,
.button-group .el-button {
  flex: 1;
  min-width: 0;
}

</style>
