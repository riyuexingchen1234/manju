<template>
  <div class="login-container">
    <!-- 返回首页 -->
    <button class="back-home" @click="goHome">
      <i class="fa fa-arrow-left"></i> 返回首页
    </button>

    <div class="login-card">
      <h1 class="login-title gradient-text">欢迎登录</h1>
      <p class="login-subtitle">登录您的 Manju 账户继续创作</p>

      <el-form :model="form" label-width="0">
        <el-form-item>
          <el-input
            v-model="form.username"
            placeholder="邮箱 / 用户名"
            class="login-input"
          />
        </el-form-item>
        <el-form-item>
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            class="login-input"
            @keyup.enter="handleLogin"
          />
        </el-form-item>

        <div class="form-options">
          <el-checkbox v-model="rememberMe">记住我</el-checkbox>
          <el-link type="primary" :underline="false" class="forgot-link" @click="showForgotPassword = true">忘记密码？</el-link>
        </div>

        <el-button
          class="btn-gradient btn-login"
          @click="handleLogin"
          :loading="loading"
        >
          登录
        </el-button>

        <div class="register-tip">
          还没有账号？<el-link type="primary" :underline="false" @click="$router.push('/register')">立即注册</el-link>
        </div>
      </el-form>
    </div>
  </div>
  <el-dialog
    v-model="showForgotPassword"
    title = "忘记密码"
    width = "300px"
    :close-on-click-modal = "true"
  >
    <p style="text-align: center; color: #666; margin: 0; ">请联系管理员，QQ：36173800</p>
  </el-dialog>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { login } from '@/api/user'
import { ElMessage } from 'element-plus'

const router = useRouter()

// 表单数据
const form = ref({
  username: '',
  password: ''
})

const loading = ref(false)
const rememberMe = ref(false)
const showForgotPassword = ref(false)

// 初始化：如果之前有记住的用户名，自动回填
onMounted(() => {
  const savedUser = localStorage.getItem('remember_username')
  if (savedUser) {
    form.value.username = savedUser
    rememberMe.value = true
  }
})

const goHome = () => {
  router.push('/')
}

const handleLogin = async () => {
  if (!form.value.username || !form.value.password) {
    ElMessage.warning('请输入用户名和密码')
    return
  }

  // 记住用户名（仅在勾选时）
  if (rememberMe.value) {
    localStorage.setItem('remember_username', form.value.username)
  } else {
    localStorage.removeItem('remember_username')
  }

  loading.value = true
  try {
    const res = await login(form.value.username, form.value.password)
    if (res.data.code === 200) {
      ElMessage.success('登录成功')
      localStorage.setItem('user', JSON.stringify(res.data.data))
      router.push('/home')
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (err) {
    ElMessage.error('网络错误，请稍后重试')
    console.error(err)
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
/* 渐变文字 */
.gradient-text {
  background: linear-gradient(135deg, #8b5cf6, #ec4899);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}

/* 渐变按钮 - 与 Home 一致 */
.btn-gradient {
  border: none !important;
  color: white !important;
  font-weight: 600 !important;
  border-radius: 999px !important;
  padding: 12px 32px !important;
  transition: all 0.2s ease !important;
  background: linear-gradient(135deg, #8b5cf6, #ec4899) !important;
  box-shadow: 0 4px 12px rgba(139, 92, 246, 0.3) !important;
}
.btn-gradient:hover {
  transform: scale(1.04) !important;
  box-shadow: 0 6px 20px rgba(139, 92, 246, 0.4) !important;
}

/* 登录容器 */
.login-container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  background: #f9fafb;
  position: relative;
}

/* 返回首页按钮 */
.back-home {
  position: absolute;
  top: 24px;
  left: 24px;
  background: none;
  border: none;
  color: #6b7280;
  font-size: 14px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 12px;
  border-radius: 8px;
  transition: background 0.2s;
}
.back-home:hover {
  background: #f3f4f6;
  color: #8b5cf6;
}

/* 登录卡片 */
.login-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.08);
}

/* 标题 */
.login-title {
  font-size: 28px;
  font-weight: 800;
  text-align: center;
  margin-bottom: 8px;
}
.login-subtitle {
  text-align: center;
  color: #6b7280;
  margin-bottom: 32px;
}

/* 输入框样式 */
.login-input :deep(.el-input__wrapper) {
  border: 1px solid #e5e7eb !important;
  border-radius: 8px !important;
  padding: 0 12px !important;
  height: 44px !important;
  box-shadow: none !important;
  transition: border-color 0.2s,box-shadow 0.2s !important;
}
.login-input :deep(.el-input__wrapper:focus-within),
.login-input :deep(.el-input.is-focused .el-input__wrapper) {
  border-color: #8b5cf6 !important;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1) !important;
}
.login-input :deep(.el-input__inner) {
  border: none !important;
  height: 100% !important;
  padding: 0 !important;
  border-radius: 0 !important;
}

/* 表单选项：记住我 + 忘记密码 */
.form-options {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 24px;
  font-size: 14px;
}
.forgot-link {
  font-size: 14px;
}

/* 登录按钮 */
.btn-login {
  width: 100%;
  margin-bottom: 20px;
}

/* 注册入口 */
.register-tip {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}
</style>
