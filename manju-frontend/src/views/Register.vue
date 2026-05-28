<template>
  <!-- 登录/注册页面的主容器，使用 Flex 布局居中显示 -->
  <div class="register-container">
    <!-- 返回首页的按钮，左上角显示 -->
    <button class="back-home" @click="goHome">
      <i class="fa fa-arrow-left"></i> 返回首页
    </button>
    <!-- 登录卡片，包含表单等内容 -->
    <div class="register-card">
      <h1 class="register-title gradient-text">创建账号</h1>
      <p class="register-subtitle">创建您的 Manju 账户开始创作</p>
      <!-- 使用 Element Plus 的表单组件，绑定模型 form 和校验规则 rules -->
      <el-form ref="formRef" :model="form" :rules="rules" label-width="0">
        <!-- 用户名输入框 -->
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            placeholder="用户名"
            class="register-input"
          />
        </el-form-item>
        <!-- 密码输入框 -->
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            placeholder="密码"
            class="register-input"
          />
        </el-form-item>
        <!-- 确认密码输入框 -->
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            type="password"
            show-password
            placeholder="确认密码"
            class="register-input"
          />
        </el-form-item>
        <!-- 注册按钮，点击触发 handleRegister，loading 状态控制按钮 loading -->
        <el-button
          class="btn-gradient btn-register"
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
// Vue 3 的 Composition API 导入
import { ref, onMounted } from 'vue'
// 用于页面跳转的路由钩子
import { useRouter } from 'vue-router'
// Element Plus 的消息提示组件
import { ElMessage } from 'element-plus'
// 后端 API：用户注册和获取积分（用于检查登录状态）
import { register, getPoints } from '@/api/user'
// 路由实例，用于编程式导航
const router = useRouter()
// 表单 ref，用于表单校验
const formRef = ref()
// 按钮 loading 状态
const loading = ref(false)
// 表单模型，绑定输入框的双向数据
const form = ref({
  username: '',
  password: '',
  confirmPassword: ''
})
// 表单校验规则
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
      // 自定义校验器：确认密码必须和密码一致
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
/**
 * 页面挂载时执行：
 * 检查 localStorage 是否已有登录用户信息，
 * 若有则尝试获取积分以验证登录状态，
 * 验证成功则自动跳转到首页。
 */
onMounted(async () => {
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      const res = await getPoints()
      if (res.data.code === 200) {
        router.push('/')
      }
    } catch {
      // 如果获取积分失败，清除本地用户信息
      localStorage.removeItem('user')
    }
  }
})
/**
 * 处理注册按钮点击事件：
 * 1. 表单校验；
 * 2. 调用后端注册接口；
 * 3. 根据返回结果提示成功或错误；
 * 4. 注册成功后跳转到登录页。
 */
const handleRegister = async () => {
  try {
    // 触发表单校验，若不通过则抛出异常
    await formRef.value.validate()
  } catch (e) {
    // 校验不通过，直接返回
    return
  }
  // 设置 loading 状态，防止重复点击
  loading.value = true
  try {
    // 调用注册 API，传入用户名和密码
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
    // 无论成功或失败，都结束 loading 状态
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

/* 注册容器：全屏居中布局 */
.register-container {
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

/* 注册卡片 */
.register-card {
  width: 100%;
  max-width: 420px;
  padding: 48px 40px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.08);
}

/* 标题 */
.register-title {
  font-size: 28px;
  font-weight: 800;
  text-align: center;
  margin-bottom: 8px;
}
.register-subtitle {
  text-align: center;
  color: #6b7280;
  margin-bottom: 32px;
}

/* 输入框样式：深度选择器修改 Element Plus 输入框内部结构 */
.register-input :deep(.el-input__wrapper) {
  border: 1px solid #e5e7eb !important;
  border-radius: 8px !important;
  padding: 0 12px !important;
  height: 44px !important;
  box-shadow: none !important;
  transition: border-color 0.2s, box-shadow 0.2s !important;
}
.register-input :deep(.el-input__wrapper:focus-within),
.register-input :deep(.el-input.is-focused .el-input__wrapper) {
  border-color: #8b5cf6 !important;
  box-shadow: 0 0 0 3px rgba(139, 92, 246, 0.1) !important;
}
.register-input :deep(.el-input__inner) {
  border: none !important;
  height: 100% !important;
  padding: 0 !important;
  border-radius: 0 !important;
}

/* 注册按钮 */
.btn-register {
  width: 100%;
  margin-bottom: 20px;
}

/* 登录入口提示文字 */
.register-tip {
  text-align: center;
  font-size: 14px;
  color: #6b7280;
}
</style>
