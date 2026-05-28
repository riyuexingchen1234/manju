// 前端路由配置文件，使用 Vue Router 创建单页面应用的路由表
// 包含登录、注册、首页等基础页面的路径映射，并设置全局守卫（目前放行所有路由）
import { createRouter, createWebHistory } from "vue-router"
import Login from "@/views/Login.vue"
import Home from '@/views/Home.vue'

const routes = [
    // 根路径，默认展示 Home 组件
    { path: '/', component: Home },
    // 登录页，用于输入账号密码
    { path: '/login', component: Login },
    // 注册页，使用延迟加载便于等待首屏质量
    { path: '/register', component: () => import('@/views/Register.vue') },
    // 其他页面的别名，用于兼容旧链接
    { path: '/home', component: Home }
]
// 创建 Vue Router 实例，使用 HTML5 History（无 #）模式并挂载路由表 routes
const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：不做任何拦截，全部放行，所有路由都直接转发到目标组件
// 登录状态的重新获取由 Home.vue 的 onMounted 处理
// 已登录用户访问 /login 可在 Login.vue 内部自行处理
router.beforeEach((to, from, next) => {
    next()
})

export default router