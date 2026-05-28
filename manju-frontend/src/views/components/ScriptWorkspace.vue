<template>
  <div class="script-workspace" >
    <!-- 消息列表区域
     ref=xxx给该div起名字，JS 里可以用 messagesContainer.value 直接拿到这个 DOM 元素。
     用途是手动控制滚动位置（发完消息自动滚到底部）。 -->
    <div ref="messagesContainer" class="messages-container">
      <!-- 无消息时显示：空状态引导 -->
      <div v-if="!messages.length" class="empty-state">
        <div class="empty-icon">
          <i class="fa fa-magic"></i>
        </div>
        <div class="empty-title">剧本创意助手</div>
        <div class="empty-desc">根据你的灵感或故事梗概生成剧本</div>
      </div>

      <!-- 有消息时显示：消息列表 -->
      <!-- <template> 是 Vue 的虚拟容器，不会渲染成真实 DOM 元素，只是用来包裹多个子元素并挂 v-else  -->
      <template v-else>
        <!-- v-for 循环渲染每条消息，msg 是当前消息对象，idx 是索引。
        :key="idx" 给每个循环项一个唯一标识，Vue 用它追踪哪个元素需要更新，不加会有警告。
        :class="['message', msg.role]" 动态绑定 class，最终渲染成 class="message user" 或 class="message assistant"，
        CSS 用这两个类控制消息靠左还是靠右显示。 -->
        <div
          v-for="(msg, idx) in messages"
          :key="idx"
          :class="['message', msg.role]"
        >
          <!-- 用户消息 -->
          <template v-if="msg.role === 'user'">
            <div class="message-content user-bubble">
              {{ msg.content }}
            </div>
          </template>

          <!-- AI 消息 -->
          <template v-else>
            <div class="message-card">
              <div class="message-header">
                <i class="fa fa-robot"></i>
                <span>AI 助手</span>
              </div>
              <div class="message-content assistant-content">
                {{ msg.content }}
              </div>
              <!-- 消息操作按钮 
               :disabled="loading" 在 AI 正在响应时禁用按钮 -->
              <div class="message-actions">
                <button
                  class="action-btn"
                  :disabled="loading"
                  @click="regenerate(idx)"
                >
                  <i class="fa fa-refresh"></i>
                  <span>重新生成</span>
                </button>
                <button class="action-btn" @click="copyContent(msg.content)">
                  <i class="fa fa-copy"></i>
                  <span>复制</span>
                </button>
              </div>
            </div>
          </template>
        </div>
      </template>

      <!-- 加载中指示器 v-if="loading" 只在请求进行中显示。
       正文换成了 typing-indicator，三个空 <span> 通过 CSS 动画实现三个点依次跳动的效果，视觉上表示 AI 正在输入中。-->
      <div v-if="loading" class="message assistant">
        <div class="message-card">
          <div class="message-header">
            <i class="fa fa-robot"></i>
            <span>AI 助手</span>
          </div>
          <div class="typing-indicator">
            <span></span>
            <span></span>
            <span></span>
          </div>
        </div>
      </div>
    </div>

    <!-- 输入区域 -->
    <div class="chat-input-area">
      <div class="input-wrapper">
        <!-- 
          多行输入框（可以自动变高）
          @input="adjustTextareaHeight" → 输入时自动调整高度
          @keydown.ctrl.enter="send" → Ctrl+回车发送
          @keydown.meta.enter="send" → Mac Command+回车发送
        -->
        <textarea
          ref="textareaRef"
          v-model="prompt"
          class="chat-textarea"
          placeholder="请输入你的创意想法..."
          rows="1"
          @input="adjustTextareaHeight"
          @keydown.ctrl.enter="send"
          @keydown.meta.enter="send"
        ></textarea>
        <!-- 两种情况会禁用：输入框为空（或只有空格），或者正在等待 AI 响应。 -->
        <button
          class="send-btn"
          :disabled="!prompt.trim() || loading"
          @click="send"
        >
          <i class="fa fa-paper-plane"></i>
        </button>
      </div>
      <div class="input-hint">Ctrl + Enter 发送</div>
    </div>

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
// nextTick = 等 DOM 渲染完，再执行我给你的代码，因为Vue 更新页面不是实时的，是异步的。
import { ref, onMounted, nextTick, watch } from 'vue'
import { generateScript } from '@/api/script'
import { loadScriptMessages, saveScriptMessages } from '@/utils/storage'
import { ElMessage } from 'element-plus'

// Emits 声明会向父组件发出 script-generated 事件，AI 回复成功后触发，通知 Home.vue 刷新历史记录和积分。
const emit = defineEmits(['script-generated'])

// ========== 响应式数据 ==========
const messages = ref([])           // [{ role: 'user'|'assistant', content: '' }]
const prompt = ref('')             // 输入框内容
const loading = ref(false)
const showErrorModal = ref(false)
const errorMessage = ref('')

// 输入框 用于调整高度
const textareaRef = ref(null)
// 消息容器（用于滚动到底部）
const messagesContainer = ref(null)

// ========== 初始化 ==========
// 组件挂载完成后，从 localStorage 读取上次的对话记录恢复进来，用户刷新页面后仍能看到历史对话。
onMounted(() => {
  messages.value = loadScriptMessages()
})

// 监听 messages 数组，任何变化都自动保存到 localStorage。
// { deep: true } 是因为数组内部对象的属性变化默认监听不到，加了 deep 才能感知到内容修改。
watch(messages, (val) => {
  saveScriptMessages(val)
}, { deep: true })

// ========== 方法 ==========
// 发送消息 取输入框内容并去掉首尾空格。
// 如果是空字符串，或者正在等待上一次请求，直接返回不执行。防止发空消息或重复发送。
const send = async () => {
  const content = prompt.value.trim()
  if (!content || loading.value) return
  // 添加用户消息 把用户消息立刻追加进列表，界面上用户消息气泡立即出现，不需要等后端响应，体验更流畅。
  messages.value.push({ role: 'user', content })
  // 清空输入框 用 nextTick 等 DOM 更新后再调整 textarea 高度
  // 因为 prompt 变成空字符串后，DOM 还没来得及更新，直接调整高度拿到的还是旧的内容高度。
  prompt.value = ''
  nextTick(() => adjustTextareaHeight())

  // 滚动到底部 等 DOM 把新消息渲染出来之后，再滚动到底部。
  // 如果不等 nextTick 直接滚，新消息还没渲染，scrollHeight 还是旧值，滚不到最新消息的位置。
  await nextTick()
  scrollToBottom()
  // 开启 loading 状态，把完整对话历史发给后端。
  // 传全量历史是为了让 DeepSeek 理解上下文，实现多轮对话。
  loading.value = true
  try {
    const res = await generateScript(messages.value)
    if (res.data.code === 200) {
      // 后端返回的可能是字符串或对象，兼容逻辑统一取内容
      // 是字符串直接用，是对象则依次尝试取 script、content 字段，都没有就把整个对象转成 JSON 字符串兜底。
      const aiContent = typeof res.data.data === 'string'
        ? res.data.data
        : res.data.data?.script || res.data.data?.content || JSON.stringify(res.data.data)
      // 把 AI 回复追加进消息列表，同时通知父组件（用于刷新积分和历史记录）。
      messages.value.push({ role: 'assistant', content: aiContent })
      emit('script-generated', aiContent)
      // 失败分两种：后端返回了 code 非 200（业务失败），或者请求本身抛出异常（网络错误、超时）。
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '网络错误，请稍后重试'
    showError(msg)
    console.error(err)
  // finally 无论成功失败都执行：关闭 loading，等 DOM 更新后滚到底部。
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 重新生成：删除该 AI 及其后的所有消息，基于剩余对话重新生成
const regenerate = async (idx) => {
  if (loading.value) return
  // 删除 idx 及之后的所有消息（包括后续的用户和 AI 消息）
  messages.value.splice(idx)

  loading.value = true
  try {
    const res = await generateScript(messages.value)
    if (res.data.code === 200) {
      const aiContent = typeof res.data.data === 'string'
        ? res.data.data
        : res.data.data?.script || res.data.data?.content || JSON.stringify(res.data.data)

      messages.value.push({ role: 'assistant', content: aiContent })
      emit('script-generated', aiContent)
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '生成失败，请稍后重试'
    showError(msg)
    console.error(err)
  } finally {
    loading.value = false
    await nextTick()
    scrollToBottom()
  }
}

// 复制内容到剪贴板
const copyContent = (content) => {
  // navigator.clipboard 是浏览器提供的剪贴板 API，writeText 返回 Promise异步对象。
  // 写then的原因是 复制是异步操作，需要等待完成
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 自适应 textarea 高度 
// 先把高度重置为 auto回到最小高度，再把高度设置为内容实际高度el.scrollHeight = 内容有多高，就多高
// 但最高不超过 200px Math.min(高度, 200)
// 这样输入内容增多时自动变高，最多长到 200px 后不再增加（超出部分可滚动）。必须先设 auto 再读 scrollHeight，否则高度会只增不减。
const adjustTextareaHeight = () => {
  const el = textareaRef.value      // 拿到输入框DOM
  if (!el) return                   // 不存在就退出
  el.style.height = 'auto'          // 先重置高度
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

// 滚动到底部
// scrollTop 是当前滚动位置，设为 scrollHeight（内容总高度）就是滚到最底部。
const scrollToBottom = () => {
  const el = messagesContainer.value
  if (!el) return
  el.scrollTop = el.scrollHeight
}

// 显示错误弹窗（需手动关闭）
const showError = (msg) => {
  errorMessage.value = msg
  showErrorModal.value = true
}
</script>

<style scoped>
.script-workspace {
  display: flex;
  flex-direction: column;   /* 弹性方向：纵向排列； */
  height: 100%;
  min-height: 440px;
}

/* ========== 消息列表区域 ========== */
.messages-container {
  flex: 1;               /* 占满剩余高度（最重要） */
  overflow-y: auto;      /* 超出自动出现滚动条 */
  padding: 24px;         /* 内边距（内容不贴边） */
  display: flex;         /* 弹性布局 */
  flex-direction: column;/* 消息从上到下竖着排 */
  gap: 16px;             /* 每条消息之间的间距 */
  scroll-behavior: smooth; /* 滚动时平滑动画 */
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 60px 20px;
  text-align: center;
}

.empty-icon {
  width: 64px;
  height: 64px;
  border-radius: 24px;
  background: linear-gradient(135deg, #fce7f3, #fef3c7);
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 20px;
}

.empty-icon i {
  font-size: 28px;
  color: #ec4899;
}

.empty-title {
  font-size: 20px;
  font-weight: 600;
  color: #374151;
  margin-bottom: 8px;
}

.empty-desc {
  font-size: 14px;
  color: #9ca3af;
  max-width: 320px;
  line-height: 1.6;
}

/* 消息项 */
.message {
  display: flex;
  width: 100%;
}

.message.user {
  justify-content: flex-end;
}

.message.assistant {
  justify-content: flex-start;
}

/* 用户气泡 */
.user-bubble {
  max-width: 80%;
  padding: 12px 18px;
  border-radius: 20px 20px 4px 20px;
  background: linear-gradient(135deg, #ec4899, #fb923c);
  color: white;
  font-size: 14px;
  line-height: 1.6;
  word-wrap: break-word;  /* 长单词/长串自动换行，不撑破气泡 */
  white-space: pre-wrap;  /* 保留你输入的空格、换行，正常显示 */
}

/* AI 卡片 */
.message-card {
  max-width: 85%;
  width: 100%;
  background: white;
  border-radius: 16px;
  border: 1px solid #f3f4f6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;    /* 溢出隐藏 */
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 12px 16px 8px;
  font-size: 13px;
  font-weight: 500;
  color: #6b7280;
}

.message-header i {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ec4899, #fb923c);
  color: white;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
}

.assistant-content {
  padding: 8px 16px 16px;
  font-size: 14px;
  line-height: 1.7;
  color: #374151;
  white-space: pre-wrap;    /* 保留AI输出的空格、换行，正常显示 */
  word-wrap: break-word;     /* 长单词/长串自动换行，不撑破气泡 */
}

/* 消息操作按钮 */
.message-actions {
  display: flex;
  gap: 8px;
  padding: 0 16px 12px;
  border-top: 1px solid #f9fafb;
  padding-top: 10px;
}

.action-btn {
  /* inline：不独占一行，能和别的按钮并排;flex：内部能用弹性布局 
  让重新生成 / 复制两个按钮并排; 同时内部图标和文字轻松居中 */
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;    /* 鼠标放上去 → 变成「小手图标」 */
  transition: all 0.15s ease;  /* 所有样式变化，用 0.15 秒平滑过渡 */
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #374151;
}

.action-btn:disabled {
  opacity: 0.5;        /* 按钮变灰、半透明 */
  cursor: not-allowed; /* 鼠标变成 ❌ 禁止图标 */
}

/* 打字动画 */
.typing-indicator {
  display: flex;   /* 三个点横着排 */
  align-items: center; /* 垂直居中 */
  gap: 6px;        /* 点和点之间距离 6px */
  padding: 16px;   /* 内边距 */
}

.typing-indicator span {
  width: 8px;       /* 宽 */
  height: 8px;      /* 高 */
  border-radius: 50%; /* 变圆形 ✅ */
  background: linear-gradient(135deg, #ec4899, #fb923c); /* 渐变色 */
  /* 跳动动画 ✅ 时长1.4s infinite无限循环 
  ease-in-out：动画先慢后快再慢，跳动更柔和自然；
  both：动画开始 / 结束都沿用关键帧样式，避免闪烁； */
  animation: typing 1.4s infinite ease-in-out both; 
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s; /* 第1个点，提前0.32秒开始 */
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s; /* 第2个点，提前0.16秒开始 */
}
/* 第三个点：默认 0s，正常时间启动 */
@keyframes typing {
  /* 0% 开始、80%、100% 结束：统一状态 */
  0%, 80%, 100% {
    transform: scale(0.6);   /* 缩放到原尺寸的 0.6 倍 */
    opacity: 0.4;            /* 透明度 40%，偏淡 */
  }
  /* 40% 时间点：动画高潮 */
  40% {
    transform: scale(1);     /* 恢复原始大小 1倍 */
    opacity: 1;              /* 完全不透明，最亮 */
  }
}

/* ========== 输入区域 ========== */
.chat-input-area {
  flex-shrink: 0;         /* 👈 不准压缩输入区域！保持固定高度！ */
  padding: 16px 24px 16px;
  border-top: 1px solid #f3f4f6;
  background: white;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  /* gap: 10px; */
  background: #f9fafb;
  border: 1px solid #e5e7eb;
  border-radius: 16px;
  padding: 10px 14px;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.input-wrapper:focus-within {
  border-color: #fb923c;
  box-shadow: 0 0 0 3px rgba(251, 146, 60, 0.08);
}

.chat-textarea {
  flex: 1;
  border: none;
  outline: none;      /* 无聚焦高亮框 */
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  color: #374151;
  resize: none;       /* 禁止手动拖拽放大 */
  min-height: 24px;
  max-height: 200px;
  font-family: inherit;     /* 字体继承父元素 */
}

.chat-textarea::placeholder {
  color: #9ca3af;
}

.send-btn {
  width: 36px;
  height: 36px;
  border-radius: 10px;
  border: none;
  background: linear-gradient(135deg, #ec4899, #fb923c);
  color: white;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: all 0.2s ease;
  flex-shrink: 0;
}

.send-btn:hover:not(:disabled) {
  transform: scale(1.05);
  box-shadow: 0 4px 12px rgba(236, 72, 153, 0.3);
}

.send-btn:disabled {
  background: #e5e7eb;
  cursor: not-allowed;
  opacity: 0.6;
}

.input-hint {
  text-align: center;
  font-size: 11px;
  color: #d1d5db;
  margin-top: 6px;
}
</style>
