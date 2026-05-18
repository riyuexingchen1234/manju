// 导入Vue Router核心函数：
// createRouter：用于创建路由实例
// createWebHistory：用于创建HTML5 History模式的路由（URL中没有#号，更美观）
import { createRouter, createWebHistory } from "vue-router"
import Login from "@/views/Login.vue"
import Home from '@/views/Home.vue'

// 定义路由配置数组
// 每个路由对象包含：path（URL路径）、component（对应的页面组件）
const routes = [
    { path: '/', component: Home },           // 根路径直接进入主页
    { path: '/login', component: Login },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/home', component: Home }        // 保留 /home 也可以访问
]
// 创建路由实例
const router = createRouter({
    history: createWebHistory(),    // 使用HTML5 History模式（需要后端配合支持，开发阶段Vite已自动支持）
    routes                       // 传入上面定义的路由配置数组
})

// 全局前置路由守卫
// 作用：在每次路由跳转前执行，用于控制页面访问权限（如：已登录用户不能再进登录页）
// 参数说明：
// - to：即将要进入的目标路由对象（要去哪）
// - from：当前导航正要离开的路由对象（从哪来）
// - next：必须调用的函数，用于放行或重定向（不调用next()，路由就不会跳转）
router.beforeEach((to, from, next) => {
    // 1. 从localStorage中获取用户信息（判断用户是否已登录）
    // 登录成功后，前端会把用户信息存入localStorage
    const user = localStorage.getItem('user')
    // 2. 权限判断逻辑：
    // 如果用户已登录 且 试图进入登录页 → 重定向到主页（防止已登录用户重复登录）
    if (user && to.path === '/login') {
        next('/home')   // 重定向到 /home
    } else {
        // 其他所有情况都放行：
        // - 未登录用户访问 / 或 /home：放行（支持未登录试用功能）
        // - 已登录用户访问 /home：放行
        // - 未登录用户访问 /login：放行
        next()
    }
})
// 导出路由实例（需要在 main.js 中引入并挂载到Vue应用上）
export default router