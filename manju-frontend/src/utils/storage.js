// 本文件封装了对浏览器 localStorage 的统一操作
// 目的：把前端需要长期保存的临时数据（如脚本对话、角色信息、图片 URL 等）统一存取，
//   - 使用统一的 key 前缀避免冲突
//   - 自动进行 JSON 序列化/反序列化，使用时直接传入/返回 JavaScript 对象或数组
//   - 读取时捕获异常并在出错时返回空值，防止页面因 JSON 解析错误而崩溃
const STORAGE_KEYS = {
    SCRIPT_MESSAGES: 'manju_script_messages',
    CHARACTERS: 'manju_characters',
    CHARACTER_IMAGES: 'manju_character_images',
    LOCAL_STORYBOARDS: 'manju_local_storyboards',
    STYLE_DECLARATION: 'manju_style_declaration',
    SCENE_IMAGES: 'manju_scene_images'
}
// 存储脚本对话数据到 localStorage
// @param {Array} messages - 脚本对话的消息数组，每条通常包含 {role, content}
export const saveScriptMessages = (messages) => {
    localStorage.setItem(STORAGE_KEYS.SCRIPT_MESSAGES, JSON.stringify(messages))
}
// 读取脚本对话数据
// @return {Array} 返回已存储的消息数组，如果不存在或解析错误则返回空数组 []
export const loadScriptMessages = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.SCRIPT_MESSAGES)
        return data ? JSON.parse(data) : []
    } catch {
        return []
    }
}
// 存储角色数据（是一个角色对象数组）
// @param {Array} characters - 角色对象数组，每个应包含 name, prompt 等属性
export const saveCharacters = (characters) => {
    localStorage.setItem(STORAGE_KEYS.CHARACTERS, JSON.stringify(characters))
}
// 读取角色数据
// @return {Array} 返回存在的角色数组，同样如不存在或解析异常时返回 []
export const loadCharacters = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.CHARACTERS)
        return data ? JSON.parse(data) : []
    } catch {
        return []
    }
}
// 存储角色图片的映射（key 为角色 ID，value 为图片 URL）
export const saveCharacterImages = (images) => {
    localStorage.setItem(STORAGE_KEYS.CHARACTER_IMAGES, JSON.stringify(images))
}
// 读取角色图片映射，如不存在或解析异常时返回 {}
export const loadCharacterImages = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.CHARACTER_IMAGES)
        return data ? JSON.parse(data) : {}
    } catch {
        return {}
    }
}
// 存储本地的 storyboard 数据，每个 storyboard 是一个对象
// @param {Array} storyboards - storyboard 对象数组
export const saveLocalStoryboards = (storyboards) => {
    localStorage.setItem(STORAGE_KEYS.LOCAL_STORYBOARDS, JSON.stringify(storyboards))
}
// 读取本地 storyboard 数据
// @return {Array} 返回 storyboard 数组，如不存在或解析异常时返回 []
export const loadLocalStoryboards = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.LOCAL_STORYBOARDS)
        return data ? JSON.parse(data) : []
    } catch {
        return []
    }
}
// 存储全局风格声明
export const saveStyleDeclaration = (styleDeclaration) => {
    localStorage.setItem(STORAGE_KEYS.STYLE_DECLARATION, JSON.stringify(styleDeclaration))
}
// 读取当前风格定义
// @return {any} 返回存在的风格定义，如不存在或解析异常时返回 ''
export const loadStyleDeclaration = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.STYLE_DECLARATION)
        return data ? JSON.parse(data) : ''
    } catch {
        return ''
    }
}
// 存储场景图的映射
export const saveSceneImages = (images) => {
    localStorage.setItem(STORAGE_KEYS.SCENE_IMAGES, JSON.stringify(images))
}
// 读取场景图的映射，如不存在或解析异常时返回 {}
export const loadSceneImages = () => {
    try {
        const data = localStorage.getItem(STORAGE_KEYS.SCENE_IMAGES)
        return data ? JSON.parse(data) : {}
    } catch {
        return {}
    }
}

// 退出登录时清除所有缓存（保留 remember_username）
export const clearAllStorage = () => {
    Object.values(STORAGE_KEYS).forEach(key => localStorage.removeItem(key))
}