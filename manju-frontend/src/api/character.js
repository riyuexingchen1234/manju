// 导入默认导出的 request 实例
import request from './user'    // 复用之前配置好的 axios 实例

export const generateCharacter = (characterName, characterPrompt, styleDeclaration = '') => {
  return request.post('/character/generate', { characterName, characterPrompt, styleDeclaration })
}


