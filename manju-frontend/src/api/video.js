import request from './user'

export const createVideoTask = (keyframeImageUrl, description) => {
  return request.post('/video/generate', { keyframeImageUrl, description })
}

export const queryVideoTask = (taskId) => {
  return request.get(`/video/task/${taskId}`)
}