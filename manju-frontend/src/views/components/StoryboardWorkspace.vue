<template>
  <div class="storyboard-workspace">
    <!-- 工具栏 -->
    <div class="storyboard-toolbar">
      <div class="actions">
        <el-button type="primary" @click="previewAllVideos" :disabled="!hasVideos">
          预览所有子视频
        </el-button>
        <el-button type="success" @click="downloadAllVideos" :disabled="!hasVideos">
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
                :http-request="(options) => handleSceneUpload(idx, options)"
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
import { ref, watch, onMounted, inject, computed, nextTick, onBeforeUnmount } from 'vue'  
import { ElMessage } from 'element-plus'
import JSZip from 'jszip'
import { saveAs } from 'file-saver'
import { generateScene as generateSceneApi } from '@/api/scene'
import { generateKeyframe as generateKeyframeApi } from '@/api/keyframe'
import { createVideoTask, queryVideoTask } from '@/api/video'
import { loadLocalStoryboards, saveLocalStoryboards } from '@/utils/storage'

const debounce = (fn, delay) => {  
  let timer = null  
  return (...args) => {  
    clearTimeout(timer)  
    timer = setTimeout(() => fn(...args), delay)  
  }  
}

const props = defineProps({
  storyboards: { type: Array, default: () => [] },
  characters: { type: Array, default: () => [] },  // 角色列表，用于下拉选择
  characterImages: { type: Object, default: () => ({}) },
  styleDeclaration: { type: String, default: '' } 
})

const emit = defineEmits(['keyframe-generated', 'video-generated', 'update:storyboards'])
const refreshPoints = inject('refreshPoints')
const refreshRecentHistory = inject('refreshRecentHistory')

// 本地分镜数据
const localStoryboards = ref([])
const sceneLoading = ref({})
const keyframeLoading = ref({})
const videoLoading = ref({})
// 失败弹窗相关变量
const showErrorModal = ref(false)
const errorMessage = ref('')
// 预览相关变量
const previewDialogVisible = ref(false)
const previewPlayerRef = ref(null)
let previewIndex = 0
let previewVideoList = []
let isPreviewing = false   // 防止重复调用
let isUnmounted = false
// 视频轮询interval引用，用于组件卸载时清理
const pollIntervals = {}

const generateId = () => Date.now() + '-' + Math.random().toString(36).substr(2, 6)

// 组件卸载时清理所有interval
onBeforeUnmount(() => {  
  isUnmounted = true   // 新增  
  Object.values(pollIntervals).forEach(timeoutId => {  
    if (timeoutId) clearTimeout(timeoutId)  
  })  
})

// 防递归标志
let isInitializingFromProps = false

// 初始化数据：优先从 props 读取（拆解新剧本），否则从 localStorage 读取
// 初始化函数
const initData = () => {
  // 防止递归
  if (isInitializingFromProps) return
  
  // 如果有 props.storyboards（拆解新剧本传入），优先使用
  if (props.storyboards && props.storyboards.length > 0) {
    isInitializingFromProps = true
    localStoryboards.value = props.storyboards.map(s => ({
      id: generateId(),
      description: s.description || '',
      characters: s.characters || [],
      scenePrompt: s.scenePrompt || '',
      sceneImageUrl: '',
      keyframePrompt: s.keyframePrompt || s.detailedDescription || '',
      keyframeImageUrl: '',
      videoPrompt: s.videoPrompt || s.keyframePrompt || s.detailedDescription || '',
      videoUrl: ''
    }))
    saveLocalStoryboards(localStoryboards.value) // 立即保存
    // 下一个 tick 清除标志
    setTimeout(() => { isInitializingFromProps = false }, 0)
  } else {
    // 否则从本地缓存读取
    const stored = loadLocalStoryboards()
    if (stored && stored.length > 0) {
      localStoryboards.value = stored
    } else {
      // 默认新建一个空白分镜
      localStoryboards.value = [{
        id: generateId(),
        description: '',
        characters: [],
        scenePrompt: '',
        sceneImageUrl: '',
        keyframePrompt: '',
        keyframeImageUrl: '',
        videoPrompt: '',
        videoUrl: ''
      }]
      saveLocalStoryboards(localStoryboards.value)
    }
  }
}

// 监听本地分镜变化 - 只保存到localStorage，不向上emit（防止编辑时数据被覆盖）
const saveDebounced = debounce((val) => saveLocalStoryboards(val), 500)  
watch(localStoryboards, (newVal) => {  
  saveDebounced(newVal)  
}, { deep: true })

// 监听 props.storyboards 变化（拆解新剧本时覆盖本地缓存）
watch(() => props.storyboards, (newVal) => {
  if (newVal && newVal.length > 0) {
    isInitializingFromProps = true
    localStoryboards.value = newVal.map(s => ({
      id: generateId(),
      description: s.description || '',
      characters: s.characters || [],
      scenePrompt: s.scenePrompt || '',
      sceneImageUrl: '',
      keyframePrompt: s.keyframePrompt || s.detailedDescription || '',
      keyframeImageUrl: '',
      videoPrompt: s.videoPrompt || s.keyframePrompt || s.detailedDescription || '',
      videoUrl: ''
    }))
    saveLocalStoryboards(localStoryboards.value)
    setTimeout(() => { isInitializingFromProps = false }, 0)
  }
}, { deep: true })

// 添加分镜
const addStoryboard = async () => { 
  localStoryboards.value.push({
    id: generateId(),
    description: '',
    characters: [],
    scenePrompt: '',
    sceneImageUrl: '',
    keyframePrompt: '',
    keyframeImageUrl: '',
    videoPrompt: '',
    videoUrl: ''
  })
  await nextTick()   // 等 Vue 更新 DOM，不再猜测 100ms  
  const lastCard = document.querySelector('.storyboard-card:last-child')  
  lastCard?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

// 删除分镜
const removeStoryboard = (index) => {
  localStoryboards.value.splice(index, 1)
  if (localStoryboards.value.length === 0) addStoryboard()
}

// 上传场景图（临时）
const handleSceneUpload = (index, options) => {
  const file = options.file
  if (!file) return
  const blobUrl = URL.createObjectURL(file)
  localStoryboards.value[index].sceneImageUrl = blobUrl
  ElMessage.success('场景图已临时加载，刷新后失效')
}

/**
 * 生成场景图
 * 作用：根据分镜中的场景描述提示词，调用AI绘图接口生成场景图片
 * 生成成功后直接赋值给当前分镜，并刷新用户积分
 * 
 * @param index - 当前分镜在数组中的下标（定位哪一个分镜）
 */
const generateScene = async (index) => {
  // 1. 根据下标获取当前要生成场景的分镜数据
  const story = localStoryboards.value[index]

  // 2. 前端校验：场景提示词不能为空，为空则提示并终止执行
  if (!story.scenePrompt.trim()) {
    errorMessage.value = '请输入场景提示词'
    showErrorModal.value = true
    return
  }

  // 3. 开启当前分镜的loading状态，防止用户重复点击
  sceneLoading.value[index] = true

  try {
    // 4. 调用后端API，传入场景提示词 + 风格声明，请求生成场景图
    const res = await generateSceneApi(story.scenePrompt, props.styleDeclaration)

    // 5. 判断接口返回状态：200 表示生成成功
    if (res.data.code === 200) {
      // 6. 取出返回的图片地址（兼容对象/字符串两种格式）
      let imageUrl = res.data.data
      if (typeof imageUrl === 'object' && imageUrl.imageUrl !== undefined) {
        imageUrl = imageUrl.imageUrl
      }

      // 7. 将生成好的图片URL赋值给当前分镜，页面自动渲染
      story.sceneImageUrl = imageUrl

      // 8. 提示用户生成成功
      ElMessage.success('场景图生成成功')

      // 9. 调用注入的方法，刷新用户积分（生成图片消耗积分）
      refreshPoints()
      refreshRecentHistory?.()
    } else {
      // 10. 后端返回失败，提示错误信息
      errorMessage.value = res.data.msg || '生成失败，请稍后重试'
      showErrorModal.value = true
    }
  } catch (err) {
    // 11. 网络异常或请求失败
    errorMessage.value = '生成失败，请检查网络后重试'
    showErrorModal.value = true
  } finally {
    // 12. 无论成功失败，最终都会关闭当前分镜的loading状态
    sceneLoading.value[index] = false
  }
}

// 生成关键帧
const generateKeyframe = async (index) => {
  const story = localStoryboards.value[index]
  if (!story.keyframePrompt.trim()) {
    errorMessage.value = '请输入关键帧提示词'
    showErrorModal.value = true
    return
  }
  if (!story.sceneImageUrl) {
    errorMessage.value = '请先生成场景图'
    showErrorModal.value = true
    return
  }
  // 收集所有选中角色的图片URL（支持多角色关键帧生成）
  const characterImageUrls = story.characters
    .map(name => props.characterImages[name])
    .filter(Boolean)
  if (story.characters.length > 0 && characterImageUrls.length === 0) {
    errorMessage.value = '请先生成角色图片'
    showErrorModal.value = true
    return
  }

  keyframeLoading.value[index] = true
  try {
    const res = await generateKeyframeApi(story.keyframePrompt, characterImageUrls, story.sceneImageUrl)
    if (res.data.code === 200) {
      let imageUrl = res.data.data
      if (typeof imageUrl === 'object' && imageUrl.imageUrl !== undefined) {
        imageUrl = imageUrl.imageUrl
      }
      story.keyframeImageUrl = imageUrl
      ElMessage.success('关键帧生成成功')
      emit('keyframe-generated', { index, imageUrl: story.keyframeImageUrl })
      refreshPoints()
      refreshRecentHistory?.()
    } else {
      errorMessage.value = res.data.msg || '生成失败，请稍后重试'
      showErrorModal.value = true
    }
  } catch (err) {
    errorMessage.value = '生成失败，请检查网络后重试'
    showErrorModal.value = true
  } finally {
    keyframeLoading.value[index] = false
  }
}

// 上传测试视频（仅用于本地测试，不调用后端）
const handleVideoUpload = (index, options) => {
  const file = options.file
  if (!file) return
  if (!file.type.startsWith('video/')) {
    errorMessage.value = '请选择视频文件'
    showErrorModal.value = true
    return
  }
  // 创建临时 URL
  const blobUrl = URL.createObjectURL(file)
  // 更新当前分镜的视频 URL
  localStoryboards.value[index].videoUrl = blobUrl
  ElMessage.success('测试视频已加载（刷新页面后失效）')
}

/**
 * 生成视频函数
 * 作用：根据分镜的关键帧图片和视频提示词，调用后端API发起视频生成任务，并轮询查询任务状态直到完成
 * 流程：
 * 1. 校验前置条件（是否有关键帧、提示词是否为空）
 * 2. 开启加载状态（防止重复点击）
 * 3. 调用后端API创建视频生成任务，获取任务ID
 * 4. 开启定时器轮询查询任务状态（每15秒查一次）
 * 5. 任务成功：赋值视频URL、关闭轮询、提示成功、刷新积分
 * 6. 任务失败：关闭轮询、提示错误
 * @param index 分镜在数组中的索引（用于定位当前是哪个分镜在生成视频）
 */
const generateVideo = async (index) => {
  // 1. 从响应式数组中获取当前分镜的数据
  const story = localStoryboards.value[index]
  // 2. 前置校验1：检查当前分镜是否已生成关键帧图片
  if (!story.keyframeImageUrl) {
    errorMessage.value = '请先生成关键帧'
    showErrorModal.value = true
    return
  }
  // 3. 前置校验2：检查视频提示词是否为空（trim()去除首尾空格）
  if (!story.videoPrompt.trim()) {
    errorMessage.value = '请输入视频提示词'
    showErrorModal.value = true
    return
  }
  // 4. 开启当前分镜的加载状态（用于控制按钮loading效果，防止重复点击）
  videoLoading.value[index] = true

   try {
    // 5. 调用后端API：创建视频生成任务
    // 传入参数：关键帧图片URL、视频提示词
    const res = await createVideoTask(story.keyframeImageUrl, story.videoPrompt)
    // 6. 判断任务创建是否成功（后端统一返回code=200表示成功）
    if (res.data.code === 200) {
      // 7. 从返回结果中获取任务ID（后续轮询需要用这个ID查询状态）
      const taskId = res.data.data.taskId
      // 8. 提示用户：视频已开始生成，需要等待
      ElMessage.info('视频生成中，请稍候...')
      // 轮询：每15秒查询一次，最多80次（约20分钟）
      let pollCount = 0
      const MAX_POLL_COUNT = 80
      const poll = async () => {  
        pollCount++  
        if (pollCount >= MAX_POLL_COUNT) {  
          delete pollIntervals[index]  
          videoLoading.value[index] = false  
          ElMessage.error('视频生成超时，请稍后重试')  
          return  
        }  
        try {  
          const result = await queryVideoTask(taskId)  
          if (result.data.code === 200) {  
            const status = result.data.data.status  
            if (status === 'SUCCEEDED') {  
              story.videoUrl = result.data.data.videoUrl  
              delete pollIntervals[index]  
              ElMessage.success('视频生成成功')  
              videoLoading.value[index] = false  
              refreshPoints()  
              refreshRecentHistory?.()  
              return  // 成功，停止轮询  
            } else if (status === 'FAILED') {  
              delete pollIntervals[index]  
              ElMessage.error('视频生成失败：' + (result.data.data.error || '未知错误'))  
              videoLoading.value[index] = false  
              return  // 失败，停止轮询  
            }  
          }  
        } catch (err) {  
          console.error('轮询出错', err)  
        }  
        // PENDING/RUNNING 或请求出错：等上一次完成后再安排下一次  
        pollIntervals[index] = setTimeout(poll, 15000)  
      }  
      pollIntervals[index] = setTimeout(poll, 15000)
    } else {
      // 16. 任务创建失败：提示后端返回的错误信息
      errorMessage.value = res.data.msg || '发起视频生成失败'
      showErrorModal.value = true
      // 17. 关闭当前分镜的加载状态
      videoLoading.value[index] = false
    }
  } catch (err) {
    // 18. 发起视频生成请求时出错（如网络错误）：提示用户
    errorMessage.value = '发起视频生成失败，请检查网络后重试'
    showErrorModal.value = true
    // 19. 关闭当前分镜的加载状态
    videoLoading.value[index] = false
  }
}

// 预览所有视频
const hasVideos = computed(() => localStoryboards.value.some(s => s.videoUrl))



// 停止预览
const stopPreview = () => {
  if (previewPlayerRef.value) {
    previewPlayerRef.value.pause()
    previewPlayerRef.value.src = ''
    // 移除事件监听，避免残留
    previewPlayerRef.value.onended = null
    previewPlayerRef.value.onerror = null
  }
  previewDialogVisible.value = false
  isPreviewing = false
}

// 播放单个视频
const playPreviewVideo = () => {  
  if (isUnmounted) return    
  if (!previewPlayerRef.value) {  
    setTimeout(playPreviewVideo, 100)  
    return  
  } 
  if (previewIndex >= previewVideoList.length) {
    // 播放完成：重置到第一个视频，暂停，保持弹窗打开
    if (previewVideoList.length > 0) {
      const firstVideoUrl = previewVideoList[0]
      // 如果当前播放器的 src 不是第一个视频，则重新加载
      if (previewPlayerRef.value.src !== firstVideoUrl) {
        previewPlayerRef.value.src = firstVideoUrl
        // 等待元数据加载完成后，将当前时间设为 0 并暂停
        previewPlayerRef.value.onloadedmetadata = () => {
          previewPlayerRef.value.currentTime = 0
          previewPlayerRef.value.pause()
        }
      } else {
        // 已经是第一个视频，只需重置时间和暂停
        previewPlayerRef.value.currentTime = 0
        previewPlayerRef.value.pause()
      }
    }
    // 重置播放状态，但不关闭弹窗
    isPreviewing = false
    previewIndex = 0
    // 不清空 previewVideoList，以便再次点击播放
    ElMessage.success('播放完成')
    return  
  }
  // 正常播放逻辑
  const currentUrl = previewVideoList[previewIndex]
  previewPlayerRef.value.src = currentUrl
  previewPlayerRef.value.play()
  previewPlayerRef.value.onended = () => {
    previewIndex++
    playPreviewVideo()
  }
  previewPlayerRef.value.onerror = () => {
    ElMessage.error(`视频 ${previewIndex+1} 加载失败，跳过`)
    previewIndex++
    playPreviewVideo()
  }
}

// 预览所有视频
const previewAllVideos = async () => {
  // 防止重复调用
  if (isPreviewing) {
    ElMessage.warning('正在播放中，请稍后')
    return
  }
  const videoUrls = localStoryboards.value.map(s => s.videoUrl).filter(Boolean)
  if (!videoUrls.length) {
    ElMessage.warning('暂无视频可预览')
    return
  }
  // 如果已有对话框打开，先关闭
  if (previewDialogVisible.value) {
    stopPreview()
    await nextTick()
  }
  previewVideoList = videoUrls
  previewIndex = 0
  previewDialogVisible.value = true
  isPreviewing = true
  // 等待 DOM 更新，确保播放器元素存在
  await nextTick()
  playPreviewVideo()
}

// 下载所有视频
const downloadAllVideos = async () => {
  const videoItems = localStoryboards.value
    .map((s, idx) => ({ url: s.videoUrl, idx }))
    .filter(item => item.url)
  if (!videoItems.length){
    errorMessage.value = '没有可下载的视频'
    showErrorModal.value = true
    return
  }

  const zip = new JSZip()
  let successCount = 0
  for (const item of videoItems) {
    try {
      const res = await fetch(item.url)
      if (!res.ok) throw new Error('下载失败')
      const blob = await res.blob()
      zip.file(`分镜${item.idx+1}_视频.mp4`, blob)
      successCount++
    } catch (err) {
      console.error(`下载分镜${item.idx+1}视频失败:`, err)
      ElMessage.warning(`分镜${item.idx+1}视频下载失败，已跳过`)
    }
  }
  if (successCount === 0) {
    errorMessage.value = '没有成功下载任何视频'
    showErrorModal.value = true
    return
  }
  const content = await zip.generateAsync({ type: 'blob' })
  saveAs(content, `漫剧分镜视频_${Date.now()}.zip`)
  ElMessage.success(`已打包 ${successCount} 个视频文件`)
}

onMounted(() => {
  initData()
})
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
