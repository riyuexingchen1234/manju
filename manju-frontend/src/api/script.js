// 导入默认导出的 request 实例
import request from './user'    // 复用之前配置好的 axios 实例

// 调用工具接口 剧本生成（支持多轮对话）
export const generateScript = (messages) => {
  return request.post('/script/generate', { messages })
}

// 拆解剧本
export const parseScript = (userScript) => {
  return request.post('/script/parse', { userScript })
}