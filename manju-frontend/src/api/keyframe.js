import request from './user'

export const generateKeyframe = (storyboardDescription, characterImageUrls, sceneImageUrl) => {
  return request.post('/keyframe/generate', { storyboardDescription, characterImageUrls, sceneImageUrl })
}
