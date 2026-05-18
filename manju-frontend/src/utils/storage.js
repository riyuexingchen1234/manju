const STORAGE_KEYS = {
    SCRIPT_MESSAGES: 'manju_script_messages',
    CHARACTERS: 'manju_characters',
    CHARACTER_IMAGES: 'manju_character_images',
    LOCAL_STORYBOARDS: 'manju_local_storyboards'
}

export const saveScriptMessages = (messages) => {
    localStorage.setItem(STORAGE_KEYS.SCRIPT_MESSAGES,JSON.stringify(messages))
}
export const loadScriptMessages = () => {
    const data = localStorage.getItem(STORAGE_KEYS.SCRIPT_MESSAGES)
    return data ? JSON.parse(data) : []
}

export const saveCharacters = (characters) => {
    localStorage.setItem(STORAGE_KEYS.CHARACTERS, JSON.stringify(characters))
}
export const loadCharacters = () =>{
    const data = localStorage.getItem(STORAGE_KEYS.CHARACTERS)
    return data ? JSON.parse(data) : []
}

export const saveCharacterImages = (images) => {
  localStorage.setItem(STORAGE_KEYS.CHARACTER_IMAGES, JSON.stringify(images))
}
export const loadCharacterImages = () => {
  const data = localStorage.getItem(STORAGE_KEYS.CHARACTER_IMAGES)
  return data ? JSON.parse(data) : {}
}

export const saveLocalStoryboards = (storyboards) => {
  localStorage.setItem(STORAGE_KEYS.LOCAL_STORYBOARDS, JSON.stringify(storyboards))
}
export const loadLocalStoryboards = () => {
  const data = localStorage.getItem(STORAGE_KEYS.LOCAL_STORYBOARDS)
  return data ? JSON.parse(data) : []
}

// 退出登录时清除所有缓存
export const clearAllStorage = () => {
    Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key))
}