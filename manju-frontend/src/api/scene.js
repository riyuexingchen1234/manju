import request from './user'

export const generateScene = (scenePrompt, styleDeclaration = '') => {
  return request.post('/scene/generate', { scenePrompt, styleDeclaration })
}