<template>
  <div class="script-workspace" >
    <!-- 消息列表区域 -->
    <div ref="messagesContainer" class="messages-container">
      <!-- 空状态引导 -->
      <div v-if="!messages.length" class="empty-state">
        <div class="empty-icon">
          <i class="fa fa-magic"></i>
        </div>
        <div class="empty-title">剧本创意助手</div>
        <div class="empty-desc">根据你的灵感或故事梗概生成剧本</div>
      </div>

      <!-- 消息列表 -->
      <template v-else>
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
              <!-- 消息操作按钮 -->
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

      <!-- 加载中指示器 -->
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
import { ref, onMounted, nextTick, watch } from 'vue'
import { generateScript } from '@/api/script'
import { loadScriptMessages, saveScriptMessages } from '@/utils/storage'
import { ElMessage } from 'element-plus'

// ========== Props ==========
const props = defineProps({
  user: {
    type: Object,
    default: () => ({})
  }
})

// ========== Emits ==========
const emit = defineEmits(['script-generated'])

// ========== 响应式数据 ==========
const messages = ref([])           // [{ role: 'user'|'assistant', content: '' }]
const prompt = ref('')             // 输入框内容
const loading = ref(false)
const showErrorModal = ref(false)
const errorMessage = ref('')

// DOM 引用
const textareaRef = ref(null)
const messagesContainer = ref(null)

// ========== 初始化 ==========
onMounted(() => {
  messages.value = loadScriptMessages()
})

// messages 变更时自动保存到 localStorage
watch(messages, (val) => {
  saveScriptMessages(val)
}, { deep: true })

// ========== 方法 ==========

// 发送消息
const send = async () => {
  const content = prompt.value.trim()
  if (!content || loading.value) return

  // 添加用户消息
  messages.value.push({ role: 'user', content })

  // 清空输入框
  prompt.value = ''
  nextTick(() => adjustTextareaHeight())

  // 滚动到底部
  await nextTick()
  scrollToBottom()

  loading.value = true
  try {
    const res = await generateScript(messages.value)
    if (res.data.code === 200) {
      // DeepSeek 返回的可能是字符串或对象，统一取内容
      const aiContent = typeof res.data.data === 'string'
        ? res.data.data
        : res.data.data?.script || res.data.data?.content || JSON.stringify(res.data.data)

      messages.value.push({ role: 'assistant', content: aiContent })

      // 通知 Home.vue（用于历史记录）
      emit('script-generated', aiContent)
    } else {
      showError(res.data.msg || '生成失败')
    }
  } catch (err) {
    const msg = err?.response?.data?.msg || err.message || '网络错误，请稍后重试'
    showError(msg)
    console.error(err)
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
  navigator.clipboard.writeText(content).then(() => {
    ElMessage.success('已复制')
  }).catch(() => {
    ElMessage.error('复制失败')
  })
}

// 自适应 textarea 高度
const adjustTextareaHeight = () => {
  const el = textareaRef.value
  if (!el) return
  el.style.height = 'auto'
  el.style.height = Math.min(el.scrollHeight, 200) + 'px'
}

// 滚动到底部
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

// 暴露给父组件的方法（可选）
defineExpose({
  messages
})
</script>

<style scoped>
.script-workspace {
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 440px;
}

/* ========== 消息列表区域 ========== */
.messages-container {
  flex: 1;
  overflow-y: auto;
  padding: 24px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  scroll-behavior: smooth;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  text-align: center;
  color: #9ca3af;
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
  word-wrap: break-word;
  white-space: pre-wrap;
}

/* AI 卡片 */
.message-card {
  max-width: 85%;
  width: 100%;
  background: white;
  border-radius: 16px;
  border: 1px solid #f3f4f6;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  overflow: hidden;
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
  white-space: pre-wrap;
  word-wrap: break-word;
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
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 4px 10px;
  border: 1px solid #e5e7eb;
  border-radius: 6px;
  background: white;
  color: #6b7280;
  font-size: 12px;
  cursor: pointer;
  transition: all 0.15s ease;
}

.action-btn:hover {
  background: #f9fafb;
  border-color: #d1d5db;
  color: #374151;
}

.action-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 打字动画 */
.typing-indicator {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 16px;
}

.typing-indicator span {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: linear-gradient(135deg, #ec4899, #fb923c);
  animation: typing 1.4s infinite ease-in-out both;
}

.typing-indicator span:nth-child(1) {
  animation-delay: -0.32s;
}

.typing-indicator span:nth-child(2) {
  animation-delay: -0.16s;
}

@keyframes typing {
  0%, 80%, 100% {
    transform: scale(0.6);
    opacity: 0.4;
  }
  40% {
    transform: scale(1);
    opacity: 1;
  }
}

/* ========== 输入区域 ========== */
.chat-input-area {
  flex-shrink: 0;
  padding: 16px 24px 20px;
  border-top: 1px solid #f3f4f6;
  background: white;
}

.input-wrapper {
  display: flex;
  align-items: flex-end;
  gap: 10px;
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
  outline: none;
  background: transparent;
  font-size: 14px;
  line-height: 1.5;
  color: #374151;
  resize: none;
  min-height: 24px;
  max-height: 200px;
  font-family: inherit;
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

/* ========== 响应式 ========== */
@media (max-width: 768px) {
  .messages-container {
    padding: 12px;
  }
  
  .user-bubble {
    max-width: 90%;
  }
  
  .message-card {
    max-width: 95%;
  }
  
  .chat-input-area {
    padding: 10px 12px 14px;
  }
}
</style>
