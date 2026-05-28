// 前端入口脚本，初始化 Vue 应用，配置路由和 UI 框架 Element Plus，
// 并挂载到 #app 根节点
import { createApp } from 'vue'
import App from './App.vue'
import router from './router'

const app = createApp(App)
app.use(router)
app.mount('#app')