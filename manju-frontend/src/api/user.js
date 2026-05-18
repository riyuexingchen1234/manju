import axios from "axios"

// 创建 axios 实例，配置基础 URL，并携带凭证（Cookie）
const request = axios.create({  
    baseURL: '/api',  
    

    timeout: 500000,    //等待AI响应时长
    withCredentials: true   // 关键：允许携带 Cookie
})
// 登录接口
export const login = (username, password) =>{
    return request.post('/user/login', null,{
        params: {username, password}
    })
}
// 查询积分
export const getPoints = () => {
  return request.get('/user/points')
}
// 登出接口
export const logout = () => {
  return request.post('/user/logout')
}

// 用户注册接口
 export const register = (username, password) => {
  return request.post('/user/register', { username, password })
 }

// 默认导出 request 实例，供其他模块使用
export default request
