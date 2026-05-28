<template>
  <div class="character-workspace">
    <div class="character-grid">
      <!-- 循环：遍历 localCharacters 数组，有几个角色就生成几张卡片
      char  = 当前这张卡片的角色数据（名字、提示词等）
      idx   = 当前卡片的序号（0、1、2...）
      key="idx" Vue 循环必须加 key，用来区分每个卡片，提高性能
      shadow="hover" 鼠标移到卡片上才显示阴影 -->
      <el-card
        v-for="(char, idx) in localCharacters"
        :key="idx"
        class="character-card"
        shadow="hover"  
      >
        <!-- el-card规定写法，卡片的头部插槽，专门存放卡片标题内容
        v-model="char.name" 双向绑定：输入框内容和角色名字同步变化 -->
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
              circle
              class="delete-btn-sm"
              @click="removeCharacter(idx)"
            />
          </div>
        </template>

        <div class="card-content">
          <!-- 图片展示区 -->
          <!-- props.characterImages 父组件通过 props 传进来的对象，键是角色名（char.name），值是该角色已生成的图片 URL（字符串）或null/undefined（表示尚未生成）。
          props.characterImages[char.name] 取出当前角色（char）对应的图片 URL。如果该角色还没有图片，则结果是 undefined（或 null）。
          逻辑与（&&）：先判断 props.characterImages 本身是否存在（防止父组件忘记传 props 或传了null），再判断该角色的图片 URL 是否存在。只有两者都为真值时，整个表达式才为 true。
          只有表达式为 true 时才渲染 <el-image>；否则不渲染该元素，转而渲染 v-else块（即“未生成”占位符）。
          动态绑定 :src（即 v-bind:src），把角色对应的图片 URL 设为 <el-image> 的来源。由于 v-if已经保证此时该 URL 必定存在，<el-image> 能安全地加载图片。 -->
          <!-- :preview-src-list 点击图片可以放大预览
          fit="cover" 图片裁剪模式，不变形、填满盒子 -->
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
            <!-- show-file-list=false，上传组件就不会渲染默认的文件列表（即不显示已上传文件的名称、进度条、删除按钮等）
            :http-request 当用户上传文件，我们将options对象连带char一起转发给uploadImage方法，方法内执行是否图片等校验 -->
            <el-upload
              class="upload-btn"
              :show-file-list="false"
              :http-request="(options) => uploadImage(char, options)"
            >
              <el-button size="small" type="default">本地上传</el-button>
            </el-upload>
            <!-- 生成角色图按钮 -->
            <!-- 给每个卡片单独设置 loading -->
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
    <!-- 点击弹窗（遮罩）外不关闭、按ESC不关闭 -->
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
// ref 创建响应式基本值，reactive 创建响应式对象，watch 监听数据变化，onMounted 在组件挂载后执行。
import { ref, reactive, watch, onMounted } from 'vue'
import { generateCharacter } from '@/api/character'
import { ElMessage } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

// Props 声明从父组件（Home.vue）接收哪些数据
// 并给每个字段设置默认值，防止父组件没传时报错。
const props = defineProps({
  // 从父组件传入的角色列表（数组），每项为 { name, characterPrompt }
  characters: {
    type: Array,
    // 默认值必须是工厂函数，避免所有实例共享同一个引用
    default: () => []
  },
  // 角色名 -> 图片 URL 的映射对象，用于展示已生成的角色图
  // "李雷":  "https://cdn.xxx.com/image/001.jpg"
  // 对象的 key 是角色名，value 是那个角色的图片地址
  characterImages: {
    type: Object,
    default: () => ({})
  },
  styleDeclaration: {
    type: String,
    default: ''
  },
  // 当前登录用户对象（可能为空），用于权限或个性化逻辑，暂时未用
  user: {
    type: Object,
    default: () => ({})
  }
})

// Emits 声明这个组件会向父组件发出两种事件：
// character-generated：某个角色的图片生成好了，带上角色名和图片URL
// update-characters：角色列表被编辑了（改名/改提示词），通知父组件同步
const emit = defineEmits(['character-generated', 'update-characters'])

// ========== 响应式数据 ==========
// 内部维护的角色列表（可编辑名字、提示词）
// 不用props.characters的原因是props是只读的。
const localCharacters = ref([])

// 每个卡片的加载状态
// 用对象记录每张卡片的加载状态，key 是卡片索引 idx，值是 true/false。
// 用 reactive 而不是 ref 是因为它是一个动态 key 的对象，reactive 能直接追踪属性的增删。
// 从而在模板中绑定 :loading
const loadingStates = reactive({})

// 错误弹窗的显示和文本内容
const showErrorModal = ref(false)
const errorMessage = ref('')

// ========== 初始化与同步 ==========
// 组件挂载时，把父组件传来的角色列表复制一份到 localCharacters。（仅在有数据时执行）
// 用 .map 是为了浅拷贝，避免直接引用 props 对象，保持本地可写副本的独立性。
const initFromProps = () => {
  if (props.characters && props.characters.length > 0) {
    localCharacters.value = props.characters.map(c => ({
      name: c.name || '',
      characterPrompt: c.characterPrompt || ''
    }))
  }
}
// 组件挂载后初始化一次
onMounted(() => {
  initFromProps()
})

// 监听：父组件传过来的 characters 数组
watch(
  // () =>：props 是外来数据，不能直接监听，必须包一层函数返回它！
  () => props.characters,   // 要监听的数据源：爸爸给的角色列表
  (newVal) => {             // newVal = 爸爸传过来的【新数据】
    // 先判断：爸爸传过来的数据不是空的
    if (newVal && newVal.length > 0) {
      // 把“爸爸的新数据”里所有名字拼成一串字符串
      const newNames = newVal.map(c => c.name).join(',')
      // 把“我本地的数据”里所有名字也拼成一串字符串 .map → JS 数组方法，循环处理数组
      const currentNames = localCharacters.value.map(c => c.name).join(',')
      // 只有 爸爸的数据 和 我的数据 不一样时，我才更新！
      if (newNames !== currentNames) {
        // 用爸爸的新数据，覆盖我的本地数据
        localCharacters.value = newVal.map(c => ({
          name: c.name || '',
          characterPrompt: c.characterPrompt || ''
        }))
      }
    }
  }, 
  { deep: true } // 深度监听：数组里面对象变了，也能监听到
  )  
/*  
监听爸爸（props）→ 必须带 () =>，监听本地（localCharacters）→ 直接写
watch(
  () => props.characters,   // 1. 监听谁
  (newVal) => {},           // 2. 变了做什么
  { deep: true }            // 3. 配置（可选）
)
*/
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

// 工具函数：统一处理后端返回的数据格式，兼容未登录字符串或已登录对象两种情况
// typeof 是 JS 的类型检测操作符，返回一个字符串描述类型。
// typeof "hello" 返回 string，typeof 123返回number，typeof {imageUrl:"xxx"}返回object
// data?.imageUrl 等价于 data == null ? undefined : data.imageUrl
// 作用是安全取属性。如果 data 是 null 或 undefined，不会报错，直接返回 undefined。如果不加 ? 直接写 data.imageUrl，当 data 是 null 时会抛出 TypeError: Cannot read properties of null。
// data?.imageUrl || data 先尝试取 data.imageUrl，如果它存在且不为空就用它；如果不存在（undefined）或为空，就回退用 data 本身。
const extractImageUrl = (data) => typeof data === 'string' ? data : data?.imageUrl || data

// 生成角色图 里面有await，函数前面必须加async
const generateImage = async (char, idx) => {
  // 校验：提示词、名字不能为空（trim去首尾空格）
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
    // 调用生成接口（等待后端返回）
    // await 等，停下来等拿到后端结果res后再往下if判断。
    const res = await generateCharacter(
      char.name,
      char.characterPrompt,
      props.styleDeclaration || ''
    )
    // 成功：通知 Home.vue 更新 characterImages 映射，显示图片
    if (res.data.code === 200) {
      const imageUrl = extractImageUrl(res.data.data)
      emit('character-generated', char.name, imageUrl)
      ElMessage.success('角色图生成成功')
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    // 兼容所有错误格式，弹出提示，代码不崩溃
    const msg = err?.response?.data?.msg || err.message || '生成失败，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    loadingStates[idx] = false
  }
}

// 本地上传角色图
const uploadImage = (char, options) => {
  // 把上传的 “图片文件” 拿出来，放到 file 变量里。
  const file = options.file
  // 如果用户没选文件，直接退出，不往下执行。
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
  // 这是浏览器自带的 “图片读取工具” 把电脑里的图片 → 变成网页能看的地址
  const reader = new FileReader()
  // 等图片读完了 → 再执行里面的代码。成功了执行这里。
  reader.onload = (e) => {
    // 拿到图片地址
    const imageUrl = e.target.result
    // 告诉父组件（Home）：这个角色的图片我上传好啦！父组件负责显示
    emit('character-generated', char.name, imageUrl)
    ElMessage.success('上传成功')
  }
  // 失败了执行这里。
  reader.onerror = () => {
    ElMessage.error('上传失败')
  }
  // 开始读取图片！不写这句，前面代码都不执行。
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
  /* 自动换行 */
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
  /* 弹性方向：纵向排列； */
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
  /* 隐藏溢出 */
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
  display: flex;   /* 让按钮并排 */
  gap: 8px;        /* 两个按钮之间留 8px 空隙 */
  margin-top: 8px;
}

.upload-btn, .btn-character {
  flex: 1;    /* 两个按钮平分宽度 */
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
/* .delete-btn-sm .el-icon {
  font-size: 10px !important;
} */

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