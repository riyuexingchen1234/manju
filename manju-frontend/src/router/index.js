import { createRouter, createWebHistory } from "vue-router"
import Login from "@/views/Login.vue"
import Home from '@/views/Home.vue'

const routes = [
    { path: '/', component: Home },
    { path: '/login', component: Login },
    { path: '/register', component: () => import('@/views/Register.vue') },
    { path: '/home', component: Home }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 路由守卫：不做任何拦截，全部放行
// 登录态恢复由 Home.vue 的 onMounted 负责
// 已登录用户访问 /login 可在 Login.vue 内部自行处理
router.beforeEach((to, from, next) => {
    next()
})

export default router
