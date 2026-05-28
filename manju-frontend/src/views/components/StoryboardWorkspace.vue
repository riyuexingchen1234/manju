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
      <!-- 循环渲染本地的分镜数据（localStoryboards），每个分镜对应一个卡片 -->
      <el-card
        v-for="(story, idx) in localStoryboards"
        :key="story.id"
        class="storyboard-card"
        shadow="hover"
      >
        <template #header>
          <div class="card-header">
            <!-- idx数组下标从0开始，用 +1 显示给用户的序号：分镜1 -->
            <!-- 显示给用户的是idx+1，实际还是idx 所以移除时是idx -->
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

          <!-- 涉及角色
          multiple 开启多选模式，v-model 绑定的值必须是数组（对应 story.characters 数组），可以同时选中多个角色。
          filterable 开启搜索过滤，输入文字会自动匹配下拉选项，适合角色较多的场景。
          allow-create 允许手动创建新选项：输入框里输入不存在的角色名，按下回车即可新增，不用提前在数据源里配置。
          default-first-option 配合搜索 / 回车使用：按下回车时，自动选中筛选结果里的第一个选项，提升操作体验。-->
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
              <!-- el-option 取值规则 label 是页面展示文本，value 是双向绑定到 story.characters 的真实值，这里统一绑定角色名。 -->
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
              <!-- v-if 控制这个 el-image 组件要不要被创建，它不会把值传给组件内部。
              :src 是在告诉 el-image 组件去哪里加载图片，没有这个属性，组件被创建出来也不知道该显示什么。
              preview-src-list 开启大图预览功能，点击图片会弹出全屏预览窗口。必须传数组，单张图片就写成 [图片地址]。 -->
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
            <div class="button-row">
              <!-- show-file-list="false" 隐藏组件默认的上传文件列表，只保留按钮，界面更简洁。
              http-request 覆盖组件默认的上传请求逻辑，接管文件上传全过程：
              组件会自动把文件、文件信息封装成 options 对象传给回调
              手动把 options 和当前分镜下标 idx 一并传给自定义方法 uploadScene
              不再走 Element Plus 自带的接口配置，完全由自己写 JS 处理文件。 -->
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
              <!-- <video> 原生视频标签，controls 是原生属性：显示播放、暂停、进度条、音量等原生控制栏。
              <source> 子标签 :src 绑定视频地址 type="video/mp4" 声明视频格式，帮助浏览器解析资源。 -->
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
            <el-button
              type="primary"
              @click="generateVideo(idx)"
              :loading="videoLoading[idx]"
              size="small"
              :disabled="!story.keyframeImageUrl || !story.videoPrompt.trim()"
              >
                生成视频 20分
            </el-button>
            
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
    <!-- 添加分镜按钮 -->
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
  storyboards: { type: Array, default: () => [] },
  characters: { type: Array, default: () => [] },   // 角色列表（前端选择角色用）
  characterImages: { type: Object, default: () => ({}) }, // 角色图片（生成关键帧用）
  styleDeclaration: { type: String, default: '' }
})

// ========== Emits ==========
const emit = defineEmits(['update-storyboards', 'generated'])

// ========== 响应式数据 ==========
const localStoryboards = ref([])
const showErrorModal = ref(false)
const errorMessage = ref('')

const previewDialogVisible = ref(false)   // 视频预览弹窗
const previewPlayerRef = ref(null)        // 视频播放器DOM
const currentPreviewIndex = ref(0)        // 当前播放第几个
// 用对象存每个分镜的 loading，避免所有按钮一起 loading。
const sceneLoading = ref({})
const keyframeLoading = ref({})
const videoLoading = ref({})
// 工具函数 兼容提取imageUrl
const extractImageUrl = (data) => typeof data === 'string' ? data : data?.imageUrl || data

const createEmptyStoryboard = () => ({
  id: Date.now(),
  description: '',
  scenePrompt: '',
  // AI 返回的原始字段 只读的原始存档，不暴露给用户编辑，不单独显示在界面上，只作为内部备份存在分镜数据里。
  detailedDescription: '',    
  videoPrompt: '',
  // 复制了一份detailedDescription，用户可以修改的工作副本，显示在分镜卡片的关键帧区域输入框里
  keyframePrompt: '',
  characters: [],
  sceneImageUrl: '',
  keyframeImageUrl: '',
  videoUrl: ''
})
// 视频轮询定时器集合 普通对象，不是响应式的。
// 存储每个分镜的轮询定时器，key是storyId，value是setInterval返回的定时器id。
// 不需要响应式是因为这个数据不需要驱动界面更新，只是内部管理用。
// 不用数组是因为想关闭单个定时器需要遍历数组，对象可以直接 键值对快速查找
const pollTimers = {}

// ========== 计算属性 ==========
// computed 计算属性，，基于现有数据计算出新值，自动根据依赖变化重新计算。
// .some(s => s.videoUrl) 数组方法：数组.some(项 => 条件)
// 循环数组每一项，对每一项执行写的条件，只要找到一个满足条件 → 立刻返回 true，全不满足返回false
const hasVideos = computed(() => {
  return localStoryboards.value.some(s => s.videoUrl)
})

// ========== 初始化与同步 ==========（单向数据流）
// 遍历父组件传来的所有分镜，每一个分镜都复制一份全新的对象，最后返回一个全新的数组,避免直接修改父组件数据 
// ... 扩展运算符 作用：把一个对象的所有属性 展开 复制一遍
const initFromProps = () => {
  if (props.storyboards && props.storyboards.length > 0) {
    localStoryboards.value = props.storyboards.map(s => ({ ...s }))
  }
}

onMounted(() => {
  initFromProps()
})

// 监听父组件分镜列表变化（如拆解剧本后传入新列表，分镜数量变、顺序变、全新一批都触发更新）。
// 用 id 拼接字符串比对，只有分镜组合真正变了才更新，防止循环触发。
// 只要任何一个分镜的 id 变了、顺序变了、数量变了 → 字符串就不一样！
// 父组件重新拆解 → 分镜变成 [4,5,6] 字符串变成 '4,5,6' 和本地 '1,2,3' 不一样 → 触发更新！
watch(() => props.storyboards, (newVal) => {
  if (newVal && newVal.length > 0) {
    const newIds = newVal.map(s => s.id).join(',')
    const currentIds = localStoryboards.value.map(s => s.id).join(',')
    if (newIds !== currentIds) {
      localStoryboards.value = newVal.map(s => ({ ...s }))
    }
  }
}, { deep: true })

// 本地分镜变化时，通过事件发送给父组件，父组件收到后，会更新它自己维护的那份数据。
watch(localStoryboards, (newVal) => {
  emit('update-storyboards', newVal.map(s => ({ ...s })))
}, { deep: true })

// 组件销毁时关掉所有轮询定时器。如果不清除，组件已经销毁了但定时器还在后台跑，
// 会继续发请求，还会尝试更新已不存在的组件数据，造成内存泄漏。
// Js原生方法Object.keys()取出对象所有的key，forEach方法逐个清除。
// onBeforeUnmount是 Vue 的生命周期钩子：组件即将被销毁、页面即将关闭 / 切换时，自动执行这里！
onBeforeUnmount(() => {
  // 1. 拿到所有存了定时器的 分镜ID（变成数组）
  Object.keys(pollTimers).forEach(storyId => {
    // 2. 根据ID找到对应的定时器，把它关掉！
    clearInterval(pollTimers[storyId])
    // 3. 关掉后，从对象里删掉这条记录（清理干净）
    delete pollTimers[storyId]
  })
})
// Object.keys() → 拿到所有 key（分镜 id） 
// forEach → 一个一个关
// clearInterval → 关闭定时器的固定写法

// ========== 方法 ==========
// 添加分镜
const addStoryboard = () => {
  localStoryboards.value.push(createEmptyStoryboard())
}

// 删除分镜
const removeStoryboard = (idx) => {
  const story = localStoryboards.value[idx]
  // 清除该分镜可能正在运行的视频轮询
  // 先保证分镜存在，并且有 id，再保证这个分镜有正在运行的定时器
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
  // 如果用户没选角色，就用空数组代替
  const charUrls = (story.characters || [])
    // Js数组原生方法map：遍历数组的每一键，返回一个值组成的新数组
    .map(name => props.characterImages[name])
    // Js数组原生方法filter：遍历数组，只保留满足条件的元素
    // 过滤掉所有空的未定义等情况的false的无效值
    .filter(Boolean)
  keyframeLoading.value[idx] = true
  try {
    const res = await generateKeyframeApi(
      // 生成关键帧时的降级逻辑
      // 优先用用户可能修改过的 keyframePrompt，如果为空就退回原始的 detailedDescription，都没有才传空字符串。
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

// 轮询方法（每15秒一次，最多80次，即20分钟）
// idx = 分镜在数组里的下标
// taskId = 后端返回的视频任务ID
const startPolling = (idx, taskId) => {
  // 1. 根据下标，拿到当前这个分镜对象
  const story = localStoryboards.value[idx]
  // 2. 从分镜对象里，拿到它的唯一ID id: Date.now()
  const storyId = story?.id
  if(!storyId) return
  if (pollTimers[storyId]) {
    clearInterval(pollTimers[storyId])
    delete pollTimers[storyId]
  }
  // 超时计数器：每个定时器自己独立的计数器
  let pollCount = 0
  const MAX_POLL = 80
  // 启动定时器，得到编号
  // 定时器timer是一个编号，浏览器内部有一个全局定时器列表，每调用一次 setInterval
  // 浏览器创建一个新的定时器，给这个定时器分配一个唯一的数字编号，例如 timer = 101
  // 浏览器提供的全局原生方法setInterval和setTimeout
  // setInterval(fn,ms) 每隔ms毫秒重复执行一次fn，返回定时器编号（数字），停止方法clearInterval(timer)
  // setTimeout(fn,ms) 等待ms毫秒后只执行一次fn，返回定时器编号（数字），停止方法clearTimeout(timer)
  const timer = setInterval(async () => {
    // 每次查询，计数器+1
    pollCount++
    if (pollCount > MAX_POLL) {
      clearInterval(timer)
      // delete 只能删除对象的属性，不能删除普通变量，所以不能写 delete timer
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
          // Js原生方法findIndex遍历整个分镜数组，找到第一个id=storyId的分镜，返回它的下标
          // 如果遍历完整个数组都没找到符合条件的，固定返回 -1
          const targetIdx = localStoryboards.value.findIndex(s => s.id === storyId)
          if (targetIdx !== -1) {
            // 创建新的分镜对象替换数组中的分镜
            // 复制原来分镜的所有属性，更新视频地址，兼容不同返回字段
            const updated = { ...localStoryboards.value[targetIdx], videoUrl: result.videoUrl || result.video_url }
            // splice 从哪个位置开始，删除几个元素，插入什么新元素
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
  // 把定时器编号存进定时器集合
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
  // 找到所有有视频的分镜的下标
  const videoIndices = localStoryboards.value
    // s 数组里的每一个分镜对象 i 这个分镜在数组里的位置（下标）
    .map((s, i) => s.videoUrl ? i : -1)
    .filter(i => i !== -1)

  if (videoIndices.length === 0) {
    ElMessage.warning('没有可预览的视频')
    return
  }

  previewDialogVisible.value = true
  await nextTick()                    // 等待对话框和视频元素渲染
  currentPreviewIndex.value = 0
  playVideoAtIndex(videoIndices[0])   // 从第一个视频开始播放
}
// 播放指定下标的视频方法
const playVideoAtIndex = (idx) => {
  const story = localStoryboards.value[idx]
  // 安全判断：分镜有视频 且 视频元素存在
  if (!story?.videoUrl || !previewPlayerRef.value) return
  // video元素原生方法
  previewPlayerRef.value.src = story.videoUrl // 设置视频地址
  previewPlayerRef.value.load()               // 重新加载视频
  previewPlayerRef.value.play()               // 开始播放 

  // 监听播放结束事件，自动切下一个
  // onended 事件监听，当视频播放到最后一秒时，自动触发这个函数。
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
    previewPlayerRef.value.pause()         // 暂停播放
    previewPlayerRef.value.src = ''        // 清空视频地址
  }
}

// 合并下载所有视频（单个视频失败时跳过，其余正常打包）
const downloadVideos = async () => {
  // 收集所有有视频的分镜
  const videos = localStoryboards.value
    // 所有分镜 → 转换成 {url, index} 对象
    .map((s, i) => ({ url: s.videoUrl, index: i + 1 }))
    // 只保留有视频的对象
    .filter(v => v.url)

  if (videos.length === 0) {
    ElMessage.warning('没有可下载的视频')
    return
  }

  ElMessage.info(`正在打包 ${videos.length} 个视频，请稍候...`)

  const zip = new JSZip()
  const failedList = []
  // 逐个下载视频，添加到ZIP包
  // for...of 用来一个一个取出数组里的每一项，每次循环把当前项赋值给 video 变量
  for (const video of videos) {
    try {
      // fetch(video.url)：向服务器发送一个 HTTP 请求 下载单个视频
      const response = await fetch(video.url)
      // 判断HTTP请求是否成功
      if (!response.ok) {
        throw new Error(`HTTP ${response.status}`)
      }
      // blob 是浏览器里的二进制大对象，用来存储文件、图片、视频等二进制数据。
      // response.blob()：把服务器返回的二进制数据转换成 blob 对象
      const blob = await response.blob()  
      // zip.file(..., blob)：把 blob 对象添加到 ZIP 包中
      zip.file(`分镜${video.index}_视频.mp4`, blob)
    } catch (err) {
      console.error(`分镜${video.index} 下载失败:`, err)
      failedList.push(video.index)    // 单个失败，记录下来，继续下载下一个
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
    // 生成ZIP文件
    const zipBlob = await zip.generateAsync({ type: 'blob' })
    // 触发浏览器下载
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
  flex-wrap: wrap;   /* 自动换行 */
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
  aspect-ratio: 16 / 9;     /* 通用宽高比 */
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
}
.add-button-container {
  margin-top: 20px;
  text-align: center;
}

.button-row {
  display: flex;
  justify-content: space-between;
}
</style>