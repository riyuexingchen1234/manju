// 导入默认导出的 request 实例
import request from './user'    // 复用之前配置好的 axios 实例

// 获取最近N条历史记录（用于悬停卡片）
export const getRecentHistory = (limit = 5) => {
    return request.get('/history/recent',{params:{limit}})
}

// 获取全部历史记录（分页，用于弹窗）
export const getHistoryList = (page = 1 ,size = 20) => {
    return request.get("history/list",{params:{page,size}})
}
