<template>
  <div class="login-container">
    <button class="back-home" @click="goHome">
      <i class="fa fa-arrow-left"></i> 返回首页
    </button>

    <div class="login-card">
      <h1 class="login-title gradient-text">创建账号</h1>
      <p class="login-subtitle">创建您的 Manju 账户开始创作</p>

      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            class="login-input"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="确认密码"
            class="login-input"
          />
        </el-form-item>

        <el-button
          class="btn-gradient btn-login"
          @click="handleRegister"
          :loading="loading"
        >
          注册
        </el-button>

        <div class="register-tip">
          已有账号？<el-link type="primary" underline="never" @click="goLogin">立即登录</el-link>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { register, getPoints } from '@/api/user'

const router = useRouter()
const formRef = ref()
const loading = ref(false)
const form = ref({
  username: '',
  password: '',
  confirmPassword: ''
})

const rules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 20, message: '用户名长度需在3-20个字符', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, message: '请确认密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== form.value.password) {
          callback(new Error('两次密码不一致'))
        } else {
          callback()
        }
      },
      trigger: 'blur'
    }
  ]
}

// 初始化：已登录用户自动跳转首页
onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const res = await getPoints()
      if (res.data.code === 200) {
        router.push('/')
      }
    } catch {
      localStorage.removeItem('user')
    }
  }
})

const handleRegister = async () => {
  try {
    await formRef.value.validate()
  } catch (e) {
    return
  }

  loading.value = true
  try {
    const res = await register(form.value.username, form.value.password)
    if (res.data.code === 200) {
      ElMessage.success('注册成功，请登录')
      setTimeout(() => router.push('/login'), 1500)
    } else {
      ElMessage.error(res.data.msg || '注册失败')
    }
  } catch (error) {
    const msg = error?.response?.data?.msg || error.message || '注册失败，请重试'
    ElMessage.error(msg)
  } finally {
    loading.value = false
  }
}

const goHome = () => {
  router.push('/')
}

const goLogin = () => {
  router.push('/login')
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

/* 输入框样式 - 系统性：直接样式化 wrapper */
.login-input :deep(.el-input__wrapper) {
  border: 1px solid #e5e7eb !important;
  border-radius: 8px !important;
  padding: 0 12px !important;
  height: 44px !important;
  box-shadow: none !important;
  transition: border-color 0.2s, box-shadow 0.2s !important;
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
