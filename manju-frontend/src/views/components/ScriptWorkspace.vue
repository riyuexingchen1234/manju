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
import { ref, inject, watch, nextTick } from 'vue'
import { generateScript } from '@/api/script'
import { ElMessage } from 'element-plus'
import { loadScriptMessages, saveScriptMessages } from '@/utils/storage'

// 从 localStorage 加载历史消息
const messages = ref(loadScriptMessages())

// 监听消息变化自动保存
watch(messages, (newVal) => saveScriptMessages(newVal), { deep: true })

// 输入框相关
const prompt = ref('')
const textareaRef = ref(null)
const messagesContainer = ref(null)

// 加载状态
const loading = ref(false)

// 事件
const emit = defineEmits(['script-generated'])
const refreshPoints = inject('refreshPoints')

// 错误弹窗
const showErrorModal = ref(false)
const errorMessage = ref('')

/**
 * 自适应调整 textarea 高度
 */
const adjustTextareaHeight = () => {
  const textarea = textareaRef.value
  if (!textarea) return
  textarea.style.height = 'auto'
  const newHeight = Math.min(textarea.scrollHeight, 200)
  textarea.style.height = newHeight + 'px'
}

/**
 * 滚动到底部
 */
const scrollToBottom = () => {
  nextTick(() => {
    const container = messagesContainer.value
    if (container) {
      container.scrollTop = container.scrollHeight
    }
  })
}

/**
 * 发送消息 → 生成剧本
 */
const send = async () => {
  if (!prompt.value.trim() || loading.value) return

  // 构造用户消息
  const userMessage = { role: 'user', content: prompt.value }
  messages.value.push(userMessage)

  // 清空输入框并重置高度
  prompt.value = ''
  if (textareaRef.value) {
    textareaRef.value.style.height = 'auto'
  }

  // 滚动到底部
  scrollToBottom()

  // 开启加载状态
  loading.value = true

  try {
    const res = await generateScript(messages.value)

    if (res.data.code === 200) {
      let aiContent = res.data.data

      // 兼容格式：后端返回ScriptGenerateResponse对象，需提取script字段
      if (typeof aiContent === 'object' && aiContent.script !== undefined) {
        aiContent = aiContent.script
      }

      // 构造 AI 回复
      const assistantMessage = { role: 'assistant', content: aiContent }
      messages.value.push(assistantMessage)

      // 通知父组件
      emit('script-generated', aiContent)
      ElMessage.success('剧本生成成功')

      // 刷新积分
      refreshPoints()
    } else {
      errorMessage.value = res.data.msg || '生成失败，请稍后重试'
      showErrorModal.value = true
      messages.value.pop()
    }
  } catch (err) {
    errorMessage.value = '生成失败，请检查网络后重试'
    showErrorModal.value = true
    messages.value.pop()
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/**
 * 重新生成指定 AI 消息
 */
const regenerate = async (idx) => {
  if (loading.value) return

  // 找到对应的 AI 消息，删除它以及之后的消息
  // idx 是 assistant 消息的索引
  const assistantIndex = idx
  
  // 删除该 assistant 消息
  messages.value.splice(assistantIndex, 1)

  loading.value = true

  try {
    const res = await generateScript(messages.value)

    if (res.data.code === 200) {
      let aiContent = res.data.data

      // 兼容格式：后端返回ScriptGenerateResponse对象，需提取script字段
      if (typeof aiContent === 'object' && aiContent.script !== undefined) {
        aiContent = aiContent.script
      }

      const assistantMessage = { role: 'assistant', content: aiContent }
      messages.value.push(assistantMessage)

      emit('script-generated', aiContent)
      ElMessage.success('重新生成成功')
      refreshPoints()
    } else {
      errorMessage.value = res.data.msg || '重新生成失败，请稍后重试'
      showErrorModal.value = true
    }
  } catch (err) {
    errorMessage.value = '重新生成失败，请检查网络后重试'
    showErrorModal.value = true
  } finally {
    loading.value = false
    scrollToBottom()
  }
}

/**
 * 复制内容到剪贴板
 */
const copyContent = async (content) => {
  try {
    await navigator.clipboard.writeText(content)
    ElMessage.success('已复制到剪贴板')
  } catch (err) {
    // 降级方案
    const textarea = document.createElement('textarea')
    textarea.value = content
    textarea.style.position = 'fixed'
    textarea.style.opacity = '0'
    document.body.appendChild(textarea)
    textarea.select()
    document.execCommand('copy')
    document.body.removeChild(textarea)
    ElMessage.success('已复制到剪贴板')
  }
}
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
