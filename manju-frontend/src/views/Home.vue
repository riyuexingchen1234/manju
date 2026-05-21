<template>
  <div class="app-root">
    <!-- ===== 顶部毛玻璃导航栏 ===== -->
    <nav class="navbar">
      <div class="navbar-container">
        <div class="navbar-left">
          <div class="logo-box">
            <div class="logo-icon">
              <i class="fa fa-magic"></i>
            </div>
            <span class="logo-text"><span class="logo-accent">漫剧创作平台</span></span>
          </div>
          <div class="nav-links">
            <button
              v-for="item in navItems"
              :key="item.id"
              class="nav-btn"
              :class="{
                'nav-hover-script': item.id === 'script',
                'nav-hover-parse': item.id === 'parse',
                'nav-hover-character': item.id === 'character',
                'nav-hover-storyboard': item.id === 'storyboard',
                'nav-active': activeNav === item.id
              }"
              @click="scrollTo(item.id)"
            >{{ item.label }}</button>
          </div>
        </div>
        <div class="navbar-right">
          <button class="btn-inspire" @click="openInspire" :loading="inspireLoading">
            <i class="fa fa-lightbulb-o"></i> 灵感助手
          </button>
          <template v-if="user.id">
            <div class="user-menu" ref="userMenuRef">
            <!-- 用户图标（首字母） -->
             <div class="user-avatar" @click.stop="toggleUserCard">{{ username.charAt(0) }}</div>
             <!-- 点击卡片 -->
              <div v-show="showUserCard" class="user-card" @click.stop>
                <div class="user-card-header">
                  <span class="user-card-name">{{ username }}</span>
                  <button class="user-card-logout" @click="handleLogout">退出</button>
                </div>
                <div class="user-card-points">
                  <i class="fa fa-diamond"></i> 积分: {{ points }}
                </div>
                <div class="user-card-divider"></div>
                <div class="user-card-history-title">最近使用</div>
                <div v-if="recentHistory.length === 0" class="user-card-empty">暂无记录</div>
                <div v-else class="user-card-history-list">
                  <div
                    v-for="item in recentHistory"
                    :key="item.id"
                    class="user-card-history-item"
                    :class="{ 'history-failed': item.status === 'failed','history-pending' : item.status === 'pending' }"
                  >
                    <span class="history-tool">{{ formatToolName(item.tool) }}</span>
                    <!-- 优先显示AI生成的结果，没有则显示用户输入 -->
                    <span v-if="item.resultText" class="history-preview">{{ formatResultText(item.tool, item.resultText) }}</span>
                    <span v-else-if="item.resultUrl" class="history-preview">查看图片/视频</span>
                    <span v-else class="history-preview">{{ item.inputPreview }}</span>
                    <span class="history-status">{{ item.status === 'pending' ? '生成中' : '' }}</span>
                    <span class="history-time">{{ formatRelativeTime(item.createdAt) }}</span>
                  </div>
                </div>
                <div class="user-card-divider"></div>
                <button class="user-card-view-all" @click="openHistoryDialog">
                  查看全部历史记录 <i class="fa fa-angle-right"></i>
                </button>
              </div>
            </div>
          </template>
          <template v-else>
            <el-button link class="login-btn" @click="goToRegister">注册</el-button>
            <el-button link class="login-btn" @click="goToLogin">登录</el-button>
          </template>
        </div>
      </div>
    </nav>

    <!-- ===== Hero 头部 ===== -->
    <section class="hero-section">
      <div class="hero-container">
        <h1 class="hero-title">
          <span class="gradient-text gradient-hero">漫剧创作平台</span>
        </h1>
        <p class="hero-subtitle">
          一键生成短剧剧本、角色形象、分镜脚本，让你的创作效率提升100倍！
        </p>
        <!-- 未登录引导 -->
        <div v-if="!user.id" class="login-tip">
          <i class="fa fa-info-circle"></i> 登录后可连续使用全部创作功能
        </div>
        <div class="hero-actions">
          <button v-if="user.id" class="btn-start" @click="scrollTo('script')">
            开始创作 <i class="fa fa-arrow-right"></i>
          </button>
          <button v-else class="btn-start" @click="goToLogin">
            立即登录 
          </button>
          <button class="btn-tutorial" @click="showTutorial = true">查看教程</button>
        </div>
      </div>
    </section>

    <!-- ===== 工作区区域（直接接在Hero下面，去掉了中间的功能卡片） ===== -->
    <section class="workspace-section">
      <div class="section-container">
        <!-- 1. 剧本生成 - 粉橙主题色 -->
        <section id="script" class="workspace-card workspace-script" style="scroll-margin-top: 80px;">
          <div class="workspace-header">
            <div class="ws-icon ws-icon-script">
              <i class="fa fa-file-text-o"></i>
            </div>
            <h2 class="ws-title gradient-text gradient-script">剧本生成</h2>
            <span class="ws-desc">输入关键词，AI自动生成完整的短剧剧本</span>
            <span class="ws-note">⚠️ 内容不会长期保存，请及时下载</span>
          </div>
          <div class="workspace-body">
            <ScriptWorkspace @script-generated="handleScriptGenerated" />
          </div>
        </section>

        <!-- 2. 拆解剧本 - 蓝青主题色 -->
        <section id="parse" class="workspace-card workspace-parse" style="scroll-margin-top: 80px;">
          <div class="workspace-header">
            <div class="ws-icon ws-icon-parse">
              <i class="fa fa-puzzle-piece"></i>
            </div>
            <h2 class="ws-title gradient-text gradient-parse">拆解剧本</h2>
            <span class="ws-desc">粘贴你的剧本，AI自动拆解出角色、场景、分镜</span>
            <span class="ws-note">⚠️ 内容不会长期保存，请及时下载</span>
          </div>
          <div class="workspace-body">
            <ParseWorkspace @parsed="handleParsed" />
          </div>
        </section>

        <!-- 3. 角色生成 - 绿青主题色 -->
        <section id="character" class="workspace-card workspace-character" style="scroll-margin-top: 80px;">
          <div class="workspace-header">
            <div class="ws-icon ws-icon-character">
              <i class="fa fa-user-o"></i>
            </div>
            <h2 class="ws-title gradient-text gradient-character">角色生成</h2>
            <span class="ws-desc">根据角色描述，AI生成统一风格的角色形象图</span>
            <span class="ws-note">⚠️ 内容不会长期保存，请及时下载</span>
          </div>
          <div class="workspace-body">
            <CharacterWorkspace
              :characters="characters"
              :characterImages="characterImages"
              :styleDeclaration="styleDeclaration"
              @character-generated="handleCharacterGenerated"
              @update-characters="handleCharactersUpdate"
            />
          </div>
        </section>

        <!-- 4. 分镜生成 - 橙黄主题色 -->
        <section id="storyboard" class="workspace-card workspace-storyboard" style="scroll-margin-top: 80px;">
          <div class="workspace-header">
            <div class="ws-icon ws-icon-storyboard">
              <i class="fa fa-film"></i>
            </div>
            <h2 class="ws-title gradient-text gradient-storyboard">分镜生成</h2>
            <span class="ws-desc">根据剧本和角色，AI自动生成分镜脚本和场景图</span>
            <span class="ws-note">⚠️ 内容不会长期保存，请及时下载</span>
          </div>
          <div class="workspace-body">
            <StoryboardWorkspace
              :storyboards="storyboards"
              :characters="characters"
              :characterImages="characterImages"
              :styleDeclaration="styleDeclaration"
              @update-storyboards="handleStoryboardsUpdate"
              @generated="handleStoryboardGenerated"
            />
          </div>
        </section>
      </div>
    </section>

    <!-- ===== 底部 ===== -->
    <footer class="app-footer">
      漫剧创作平台 · 让创作更简单
    </footer>

    <!-- ===== 灵感助手对话框 ===== -->
    <el-dialog
      v-model="inspireVisible"
      width="640px"
      :close-on-click-modal="false"
      :close-on-press-escape="false"
      class="inspire-dialog"
      :show-close="false"
    >
      <div class="dialog-header">
        <div class="dialog-header-gradient">
          <i class="fa fa-lightbulb-o"></i> 灵感助手
        </div>
        <button class="dialog-close" @click="inspireVisible = false">
          <i class="fa fa-times"></i>
        </button>
      </div>
      <el-tabs v-model="inspireTab" class="inspire-tabs">
        <el-tab-pane label="漫剧榜单" name="rank">
          <el-table :data="rankList" stripe style="width: 100%" v-loading="rankLoading">
            <el-table-column prop="title" label="作品名" />
            <el-table-column prop="hotValue" label="热度值" width="100">
              <template #default="scope">
                <span class="hot-value">{{ scope.row.hotValue }}</span>
              </template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无榜单数据"></el-empty>
            </template>
          </el-table>
        </el-tab-pane>
        <el-tab-pane label="小说 IP 搜索" name="search">
          <div class="search-line">
            <el-input v-model="searchKeyword" placeholder="输入关键词，如：修仙、穿越" clearable />
            <el-button type="primary" @click="searchNovelHandle" :loading="searchLoading">搜索</el-button>
          </div>
          <el-table v-if="novelList.length" :data="novelList" stripe style="width: 100%; margin-top: 12px;">
            <el-table-column prop="title" label="书名" />
            <el-table-column prop="author" label="作者" width="100" />
            <el-table-column prop="intro" label="简介" />
          </el-table>
          <el-empty v-else description="输入关键词搜索小说IP" style="margin-top: 40px;"></el-empty>
        </el-tab-pane>
        <el-tab-pane label="对标拆解" name="analyze">
          <div class="search-line">
            <el-input v-model="analyzeWorkName" placeholder="输入作品名，如：庆余年" clearable />
            <el-button type="primary" @click="analyze" :loading="analyzeLoading">分析</el-button>
          </div>
          <el-card v-if="analysisResult" shadow="hover" class="analysis-card">
            <div class="analysis-grid">
              <div v-for="(val, key) in analysisResult" :key="key" class="analysis-cell">
                <span class="cell-key">{{ key }}</span>
                <span class="cell-val">{{ val }}</span>
              </div>
            </div>
          </el-card>
          <el-empty v-else description="输入作品名进行对标分析" style="margin-top: 40px;"></el-empty>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>

    <!-- ===== 历史记录弹窗 ===== -->
    <el-dialog
      v-model="historyDialogVisible"
      title="历史记录"
      width="1100px"
      :close-on-click-modal="true"
      class="history-dialog"
    >
      <div v-if="historyList.length === 0" class="history-dialog-empty">
        <i class="fa fa-history" style="font-size: 48px; color: #d1d5db;"></i>
        <p style="color: #9ca3af; margin-top: 16px;">暂无历史记录</p>
      </div>
      <div v-else class="history-dialog-list">
        <!-- 表头 -->
        <div class="history-row history-header">
          <div class="history-col history-col-tool">工具</div>
          <div class="history-col history-col-input">用户输入</div>
          <div class="history-col history-col-result">AI生成内容</div>
          <div class="history-col history-col-media">图片/视频</div>
          <div class="history-col history-col-status">状态</div>
          <div class="history-col history-col-time">时间</div>
        </div>
        <!-- 数据行 -->
        <div
          v-for="item in historyList"
          :key="item.id"
          class="history-row"
          :class="{ 'history-failed': item.status === 'failed' }"
        >
          <!-- 工具类型 -->
          <div class="history-col history-col-tool">
            <span class="history-tool-tag">{{ formatToolName(item.tool) }}</span>
          </div>
          
          <!-- 用户输入 -->
          <div class="history-col history-col-input">
            <div class="history-text-wrapper">
              <pre class="history-text" :title="item.inputPreview">{{ item.inputPreview || '无' }}</pre>
              <button 
                v-if="item.inputPreview" 
                class="history-copy-btn" 
                @click="copyToClipboard(item.inputPreview)"
                title="复制"
              >
                <i class="fa fa-copy"></i>
              </button>
            </div>
          </div>
          
          <!-- AI生成内容 -->
          <div class="history-col history-col-result">
            <div v-if="item.resultText" class="history-text-wrapper">
              <pre class="history-text ai-text" :title="item.resultText">{{ item.resultText }}</pre>
              <button class="history-copy-btn" @click="copyToClipboard(item.resultText)" title="复制">
                <i class="fa fa-copy"></i>
              </button>
            </div>
            <div v-else class="history-no-text">无文本</div>
          </div>
          
          <!-- 图片/视频 -->
          <div class="history-col history-col-media">
            <template v-if="item.resultUrl">
              <!-- 图片 -->
              <div v-if="isImageUrl(item.resultUrl)" class="history-media-card">
                <el-image
                  :src="item.resultUrl"
                  :preview-src-list="[item.resultUrl]"
                  fit="contain"
                  class="history-img"
                  :preview-teleported="true"
                />
              </div>
              <!-- 视频 -->
              <div v-else-if="isVideoUrl(item.resultUrl)" class="history-media-card">
                <div class="history-video-box">
                  <video controls style="width:100%; height:100%; object-fit: cover;">
                    <source :src="item.resultUrl" type="video/mp4" />
                  </video>
                </div>
              </div>
              <!-- 其他链接 -->
              <a v-else :href="item.resultUrl" target="_blank" class="history-link">
                <i class="fa fa-external-link"></i> 打开
              </a>
            </template>
            <div v-else class="history-no-text">无</div>
          </div>
          
          <!-- 状态 -->
          <div class="history-col history-col-status">
            <span class="history-status" :class="`status-${item.status}`">
              {{ item.status === 'success' ? '成功' : item.status === 'pending' ? '生成中' : '失败' }}
            </span>
          </div>
          
          <!-- 时间 -->
          <div class="history-col history-col-time">
            <span class="history-time">{{ formatRelativeTime(item.createdAt) }}</span>
          </div>
        </div>
      </div>
      
      <div v-if="historyTotal > historySize" class="history-dialog-pagination">
        <el-pagination
          :current-page="historyPage"
          :page-size="historySize"
          :total="historyTotal"
          layout="prev, pager, next"
          @current-change="handlePageChange"
        />
      </div>
    </el-dialog>

    <!-- ===== 使用教程弹窗 ===== -->
    <el-dialog v-model="showTutorial" title="使用教程" width="680px" :close-on-click-modal="true" class="tutorial-dialog">
      <div class="tutorial-content">
        <h3>📚 漫剧创作平台使用指南</h3>
        
        <h4>一、免费与积分机制</h4>
        <div class="tutorial-section">
          <p><strong>未登录用户：</strong>每个工具每日有1次免费试用机会（剧本生成、拆解剧本、角色生成、场景生成、关键帧生成、视频生成各1次）。试用数据仅保存在本地浏览器，刷新页面后清除。</p>
          <p><strong>已登录用户：</strong>注册即送100积分，可享受完整功能。每个工具积分消耗如下：</p>
          <ul>
            <li>剧本生成：5积分</li>
            <li>拆解剧本：5积分</li>
            <li>角色生成：10积分</li>
            <li>场景生成：10积分</li>
            <li>关键帧生成：10积分</li>
            <li>视频生成：20积分</li>
          </ul>
        </div>

        <h4>二、典型创作流程</h4>
        <div class="tutorial-section">
          <p><strong>方式一：AI生成剧本 → 拆解 → 生成分镜</strong></p>
          <p>1. 在「剧本生成」输入关键词，生成完整短剧剧本</p>
          <p>2. 在「拆解剧本」粘贴剧本内容，AI自动拆解出角色、场景、分镜描述</p>
          <p>3. 分镜生成区自动填充提示词，点击「生成场景图」「生成关键帧」制作分镜素材</p>
          <p>4. 为每个分镜添加视频提示词，生成视频片段</p>
          <p>5. 最后可下载全部素材进行后期剪辑</p>
          
          <p><strong>方式二：手动创作分镜</strong></p>
          <p>跳过剧本生成，直接在「分镜生成」区手动添加分镜、手动编写提示词。</p>
        </div>

        <h4>三、各模块详解</h4>
        <div class="tutorial-section">
          <p><strong>剧本生成</strong>：输入一句话或关键词，AI生成完整短剧剧本（包含场景标记、角色台词、动作描写）。</p>
          
          <p><strong>拆解剧本</strong>：将剧本正文粘贴进去，AI自动提取角色信息、生成场景提示词、每个分镜的关键帧描述和视频提示词。</p>
          <p class="tip">💡 拆解后的内容会自动填充分镜工作区，可直接用于后续生成。</p>
          
          <p><strong>角色生成</strong>：输入角色外貌描述，AI生成统一风格的角色四视图（正面、侧面、背面、大头）。</p>
          <p class="tip">💡 生成的角色的形象图可用于关键帧生成，让角色在分镜中保持一致。</p>
          
          <p><strong>场景生成</strong>：根据场景描述生成纯背景图（不含人物）。</p>
          <p class="tip">💡 场景图是生关键帧的前置条件。</p>
          
          <p><strong>关键帧生成</strong>：结合场景图和角色图，生成包含角色的分镜画面。</p>
          <p class="tip">💡 关键帧是视频生成的素材，需先有场景图才能生成关键帧。</p>
          
          <p><strong>视频生成</strong>：根据关键帧图和视频提示词，生成4-5秒的视频片段。</p>
          <p class="warning">⚠️ 视频生成需要较长时间（通常3-5分钟），请耐心等待。可关闭页面稍后再来查看结果。</p>
        </div>

        <h4>四、注意事项</h4>
        <div class="tutorial-section">
          <ul>
            <li>积分不足时无法使用付费功能，请及时充值</li>
            <li>免费试用每日重置，未登录用户数据刷新后丢失</li>
            <li>视频生成等异步任务可在历史记录中查看状态</li>
            <li>分镜内容保存在本地浏览器，换设备或清缓存会丢失，记得及时下载</li>
          </ul>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getPoints, logout as logoutApi } from '@/api/user'
import { getRecentHistory, getHistoryList } from '@/api/history'
import { searchNovel, getRank, analyzeWork } from '@/api/assist'
import {
  loadCharacters, saveCharacters,
  loadCharacterImages, saveCharacterImages,
  loadLocalStoryboards, saveLocalStoryboards,
  loadStyleDeclaration, saveStyleDeclaration,
  clearAllStorage
} from '@/utils/storage'
import ScriptWorkspace from './components/ScriptWorkspace.vue'
import ParseWorkspace from './components/ParseWorkspace.vue'
import CharacterWorkspace from './components/CharacterWorkspace.vue'
import StoryboardWorkspace from './components/StoryboardWorkspace.vue'

const router = useRouter()
// 统一解包 API 响应，code !== 200 时抛出异常
const unwrap = async (promise) => {
  const res = await promise
  if (res.data.code === 200) return res.data.data
  throw new Error(res.data.msg || '请求失败')
}

// ========== 用户状态 ==========
const user = ref({})
const username = computed(() => user.value.username || '')
const points = computed(() => user.value.points ?? '--')
const showUserCard = ref(false)
const userMenuRef = ref(null)

// ========== 导航 ==========
const navItems = [
  { id: 'script', label: '剧本生成' },
  { id: 'parse', label: '拆解剧本' },
  { id: 'character', label: '角色生成' },
  { id: 'storyboard', label: '分镜生成' }
]
const activeNav = ref('')

// ========== 跨工作区共享数据 ==========
const characters = ref([])
const characterImages = ref({})
const storyboards = ref([])
const styleDeclaration = ref('')

// ========== 历史记录 ==========
const recentHistory = ref([])
const historyDialogVisible = ref(false)
const historyList = ref([])
const historyTotal = ref(0)
const historyPage = ref(1)
const historySize = ref(20)

// ========== 灵感助手 ==========
const inspireVisible = ref(false)
const inspireLoading = ref(false)
const inspireTab = ref('rank')
const rankList = ref([])
const rankLoading = ref(false)
const searchKeyword = ref('')
const novelList = ref([])
const searchLoading = ref(false)
const analyzeWorkName = ref('')
const analysisResult = ref(null)
const analyzeLoading = ref(false)

// ========== 教程 ==========
const showTutorial = ref(false)

// ========== 初始化 ==========
onMounted(async () => {
  // 1. 恢复用户信息
  const userStr = localStorage.getItem('user')
  if (userStr) {
    try {
      user.value = JSON.parse(userStr)
    } catch {
      localStorage.removeItem('user')
    }
  }

  // 2. 验证 session 并刷新积分 + 加载最近历史
  if (user.value.id) {
    try {
      user.value.points = await unwrap(getPoints())
    } catch {
      user.value = {}
      localStorage.removeItem('user')
      clearAllStorage()
    }
        await loadRecentHistory()
  }

  // 4. 恢复工作区数据
  characters.value = loadCharacters()
  characterImages.value = loadCharacterImages()
  storyboards.value = loadLocalStoryboards()
  styleDeclaration.value = loadStyleDeclaration()

  // 5. 点击外部关闭用户卡片
  document.addEventListener('click', handleClickOutside)

  // 6. 监听其他标签页的 localStorage 变更
  window.addEventListener('storage', handleStorageChange)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
  window.removeEventListener('storage', handleStorageChange)
})

// 路由变化时刷新积分
watch(() => router.currentRoute.value.path, (path) => {
  if ((path === '/' || path === '/home') && user.value.id) {
    refreshPoints()
  }
})

// ========== 用户方法 ==========
const toggleUserCard = () => { showUserCard.value = !showUserCard.value }

const refreshPoints = async () => {
  if (!user.value.id) return
  try { user.value.points = await unwrap(getPoints()) } catch { /* ignore */ }
}

const handleLogout = async () => {
  try { await logoutApi() } catch { /* ignore */ }
  user.value = {}
  showUserCard.value = false
  recentHistory.value = []
  localStorage.removeItem('user')
  clearAllStorage()
  ElMessage.success('已退出登录')
}

const goToLogin = () => router.push('/login')
const goToRegister = () => router.push('/register')

const handleClickOutside = (e) => {
  if (userMenuRef.value && !userMenuRef.value.contains(e.target)) {
    showUserCard.value = false
  }
}

// ========== 导航 ==========
const scrollTo = (sectionId) => {
  activeNav.value = sectionId
  document.getElementById(sectionId)?.scrollIntoView({ behavior: 'smooth' })
}

// ========== 数据流转 ==========
const handleScriptGenerated = () => {
  if (user.value.id) {
    loadRecentHistory()
    refreshPoints()
  }
}

const handleParsed = (data) => {
  characters.value = data.characters
  storyboards.value = data.storyboards
  styleDeclaration.value = data.styleDeclaration
  saveCharacters(data.characters)
  saveLocalStoryboards(data.storyboards)
  saveStyleDeclaration(data.styleDeclaration)
  scrollTo('character')
  if (user.value.id) {
    loadRecentHistory()
    refreshPoints()
  }
}

const handleCharacterGenerated = (name, url) => {
  characterImages.value[name] = url
  saveCharacterImages(characterImages.value)
  if (user.value.id) {
    loadRecentHistory()
    refreshPoints()
  }
}

const handleCharactersUpdate = (list) => {
  characters.value = list
  saveCharacters(list)
}

const handleStoryboardsUpdate = (list) => {
  storyboards.value = list
  saveLocalStoryboards(list)
}

const handleStoryboardGenerated = () => {
  if (user.value.id) {
    loadRecentHistory()
    refreshPoints()
  }
}

// ========== 历史记录 ==========
const openHistoryDialog = () => {
  historyDialogVisible.value = true
  historyPage.value = 1
  showUserCard.value = false
  fetchHistoryList()
}
const loadRecentHistory = async () => {
  if (!user.value.id) return
  try {
    const data = await unwrap(getRecentHistory(5))
    recentHistory.value = data || []
  } catch { /* ignore */ }
}

const fetchHistoryList = async () => {
  try {
    const data = await unwrap(getHistoryList(historyPage.value, historySize.value))
    historyList.value = data.list || []
    historyTotal.value = data.total || 0
  } catch { ElMessage.error('获取历史记录失败') }
}

const handlePageChange = (page) => {
  historyPage.value = page
  fetchHistoryList()
}

const formatToolName = (tool) => {
  const map = {
    'script_generate': '剧本生成',
    'parse_script': '拆解剧本',
    'character_generate': '角色生成',
    'scene_generate': '场景生成',
    'keyframe_generate': '关键帧生成',
    'video_generate': '视频生成'
  }
  return map[tool] || tool
}

const formatRelativeTime = (timeStr) => {
  if (!timeStr) return ''
  const d = new Date(timeStr)
  const now = new Date()
  const diff = now - d
  if (diff < 60000) return '刚刚'
  if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
  if (diff < 86400000) return Math.floor(diff / 3600000) + '小时前'
  return `${String(d.getMonth()+1).padStart(2,'0')}-${String(d.getDate()).padStart(2,'0')} ${String(d.getHours()).padStart(2,'0')}:${String(d.getMinutes()).padStart(2,'0')}`
}

const formatResultText = (tool, text) => {
  if (!text) return ''
  return text.length > 20 ? text.substring(0, 20) + '...' : text
}

const copyToClipboard = (text) => {
  navigator.clipboard.writeText(text).then(() => {
    ElMessage.success('已复制')
  }).catch(() => ElMessage.error('复制失败'))
}

const isImageUrl = (url) => /\.(jpg|jpeg|png|gif|webp)(\?|$)/i.test(url)
const isVideoUrl = (url) => /\.(mp4|webm|ogg)(\?|$)/i.test(url)

// ========== 灵感助手 ==========
const openInspire = () => {
  inspireVisible.value = true
  if (!rankList.value.length) fetchRank()
}

const fetchRank = async () => {
  rankLoading.value = true
  try {
  rankList.value = await unwrap(getRank())
} catch { /* ignore */ }
finally { rankLoading.value = false }
}

const searchNovelHandle = async () => {
  if (!searchKeyword.value.trim()) return ElMessage.warning('请输入关键词')
  searchLoading.value = true
  try {
  novelList.value = await unwrap(searchNovel(searchKeyword.value))
} catch { ElMessage.error('搜索失败') }
finally { searchLoading.value = false }
}

const analyze = async () => {
  if (!analyzeWorkName.value.trim()) return ElMessage.warning('请输入作品名')
  analyzeLoading.value = true
  try {
  analysisResult.value = await unwrap(analyzeWork(analyzeWorkName.value))
} catch { ElMessage.error('分析失败') }
finally { analyzeLoading.value = false }
}

// ========== 多标签页同步 ==========
const handleStorageChange = (e) => {
  if (e.key === 'user') {
    if (e.newValue) {
      try {
        user.value = JSON.parse(e.newValue)
        getPoints().then(res => {
          if (res.data.code === 200) user.value.points = res.data.data
        }).catch(() => {})
        characters.value = loadCharacters()
        characterImages.value = loadCharacterImages()
        storyboards.value = loadLocalStoryboards()
        styleDeclaration.value = loadStyleDeclaration()
      } catch { /* ignore */ }
    } else {
      user.value = {}
      showUserCard.value = false
      recentHistory.value = []
      clearAllStorage()
    }
  } else if (e.key === 'manju_characters' && e.newValue) {
    try { characters.value = JSON.parse(e.newValue) } catch { /* ignore */ }
  } else if (e.key === 'manju_character_images' && e.newValue) {
    try { characterImages.value = JSON.parse(e.newValue) } catch { /* ignore */ }
  } else if (e.key === 'manju_local_storyboards' && e.newValue) {
    try { storyboards.value = JSON.parse(e.newValue) } catch { /* ignore */ }
  } else if (e.key === 'manju_style_declaration' && e.newValue) {
    try { styleDeclaration.value = JSON.parse(e.newValue) } catch { /* ignore */ }
  }
}
</script>

<style scoped>
/* ===== Reset & Base ===== */
.app-root {
  min-height: 100vh;
  background: linear-gradient(135deg, #fdf2f8, #f5f3ff, #eff6ff);
  font-family: -apple-system, 'PingFang SC', 'Microsoft YaHei', sans-serif;
  color: #1f2937;
  scroll-behavior: smooth;
}

.gradient-text {
  background-clip: text;
  -webkit-background-clip: text;
  color: transparent;
}

/* ===== Navbar ===== */
.navbar {
  position: fixed; top: 0; left: 0; right: 0; z-index: 1000;
  background: rgba(255,255,255,0.7);
  backdrop-filter: blur(12px);
  -webkit-backdrop-filter: blur(12px);
  border-bottom: 1px solid rgba(255,255,255,0.2);
  box-shadow: 0 1px 8px rgba(0,0,0,0.04);
}
.navbar-container {
  max-width: 1280px; margin: 0 auto;
  display: flex; align-items: center; justify-content: space-between;
  height: 64px; padding: 0 24px;
}
.navbar-left { display: flex; align-items: center; gap: 32px; }
.logo-box { display: flex; align-items: center; gap: 8px; }
.logo-icon {
  width: 32px; height: 32px; border-radius: 8px;
  background: linear-gradient(135deg, #ec4899, #fb923c);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 16px;
}
.logo-text { font-size: 20px; font-weight: 700; }
.logo-accent {
  background: linear-gradient(135deg, #ec4899, #a855f7);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.nav-links { display: flex; align-items: center; gap: 2px; }
.nav-btn {
  padding: 6px 16px; border-radius: 8px;
  background: transparent; border: none;
  font-size: 14px; font-weight: 500; color: #4b5563;
  cursor: pointer; transition: all 0.2s ease;
}
.nav-btn:hover { background: rgba(255,255,255,0.5); }
.nav-btn.nav-active {
  background: rgba(255,255,255,0.8);
  font-weight: 600;
}
/* 导航栏按钮颜色 */
.nav-hover-script:hover, .nav-hover-script.nav-active { color: #ec4899; }
.nav-hover-parse:hover, .nav-hover-parse.nav-active { color: #3b82f6; }
.nav-hover-character:hover, .nav-hover-character.nav-active { color: #22c55e; }
.nav-hover-storyboard:hover, .nav-hover-storyboard.nav-active { color: #f97316; }
.navbar-right { display: flex; align-items: center; gap: 12px; }
.btn-inspire {
  display: flex; align-items: center; gap: 6px;
  padding: 8px 20px; border-radius: 999px;
  background: linear-gradient(135deg, #a855f7, #ec4899);
  color: white; border: none;
  font-size: 14px; font-weight: 500;
  cursor: pointer; transition: all 0.2s ease;
  box-shadow: 0 4px 12px rgba(168,85,247,0.3);
}
.btn-inspire:hover {
  transform: scale(1.04);
  box-shadow: 0 6px 20px rgba(168,85,247,0.4);
}
.user-badge {
  display: flex; align-items: center; gap: 10px;
}
.badge-name { font-size: 14px; color: #4b5563; }
.badge-points {
  display: inline-flex; align-items: center; gap: 4px;
  border: 1px solid ;
  padding: 2px 8px; border-radius: 999px;
  background: none;
  color: #6b7280; font-size: 13px; font-weight: 400;
}
.btn-logout {
  background: none; border: none;
  font-size: 13px; color: #9ca3af;
  cursor: pointer; transition: color 0.15s;
}
.btn-logout:hover { color: #ef4444; }
.login-btn {
  font-size: 14px; color: #4b5563;
  margin-left: 0 !important;
}

/* ===== 用户菜单和悬停卡片 ===== */
.user-menu {
  position: relative;
  display: flex;
  align-items: center;
}
.user-avatar {
  width: 36px; height: 36px; border-radius: 50%;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 15px; font-weight: 600;
  cursor: pointer; transition: transform 0.2s ease;
}
.user-avatar:hover {
  transform: scale(1.08);
}
.user-card {
  position: absolute;
  top: 48px; right: 0;
  width: 320px;
  background: white;
  border-radius: 16px;
  box-shadow: 0 10px 40px rgba(0,0,0,0.12);
  padding: 20px;
  z-index: 1001;
  animation: cardSlideIn 0.2s ease;
}
@keyframes cardSlideIn {
  from { opacity: 0; transform: translateY(-8px); }
  to { opacity: 1; transform: translateY(0); }
}
.user-card-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 12px;
}
.user-card-name {
  font-size: 16px; font-weight: 600; color: #1f2937;
}
.user-card-logout {
  background: none; border: none;
  font-size: 13px; color: #9ca3af;
  cursor: pointer; transition: color 0.15s;
}
.user-card-logout:hover { color: #ef4444; }
.user-card-points {
  display: flex; align-items: center; gap: 6px;
  font-size: 14px; color: #6b7280;
  margin-bottom: 16px;
}
.user-card-points i {
  color: #ec4899;
}
.user-card-divider {
  height: 1px; background: #f0f0f0;
  margin: 12px 0;
}
.user-card-history-title {
  font-size: 13px; font-weight: 600; color: #374151;
  margin-bottom: 10px;
}
.user-card-empty {
  text-align: center; padding: 20px 0;
  color: #9ca3af; font-size: 13px;
}
.user-card-history-list {
  display: flex; flex-direction: column; gap: 8px;
}
.user-card-history-item {
  display: flex; align-items: center; gap: 8px;
  padding: 8px 10px;
  border-radius: 8px;
  background: #f9fafb;
  font-size: 13px;
  transition: background 0.15s;
}
.user-card-history-item:hover {
  background: #f3f4f6;
}
.user-card-history-item.history-failed {
  opacity: 0.6;
}
.user-card-history-item.history-pending{
  opacity: 0.8;
}
.user-card-history-item.history-pending .history-status{
  color: #f59e0b;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
  flex-shrink: 0;
}
.history-tool {
  display: inline-block;
  padding: 2px 8px; border-radius: 6px;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  color: white; font-size: 11px; font-weight: 500;
  white-space: nowrap; flex-shrink: 0;
}
.history-preview {
  color: #4b5563;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
  flex: 1;
}
.history-time {
  color: #9ca3af; font-size: 11px;
  white-space: nowrap; flex-shrink: 0;
}
.user-card-view-all {
  width: 100%;
  padding: 10px;
  border-radius: 10px;
  border: 1px solid #e5e7eb;
  background: white;
  color: #4b5563;
  font-size: 13px; font-weight: 500;
  cursor: pointer;
  transition: all 0.2s ease;
  display: flex; align-items: center; justify-content: center; gap: 4px;
}
.user-card-view-all:hover {
  background: #f9fafb;
  border-color: #d1d5db;
}

/* ===== 历史记录弹窗 ===== */
.history-dialog {
  --el-dialog-border-radius: 20px;
}
.history-dialog-empty {
  text-align: center; padding: 40px 0;
}
.history-dialog-list {
  display: flex; flex-direction: column; gap: 10px;
  max-height: 480px;
  overflow-y: auto;
}
.history-dialog-item {
  display: flex; justify-content: space-between; align-items: center;
  padding: 14px 16px;
  border-radius: 12px;
  background: #f9fafb;
  transition: background 0.15s;
}
.history-dialog-item:hover {
  background: #f3f4f6;
}
.history-dialog-item.history-failed {
  opacity: 0.6;
}
.history-dialog-left {
  display: flex; align-items: center; gap: 12px;
  flex: 1; min-width: 0;
}
.history-dialog-tool {
  display: inline-block;
  padding: 3px 10px; border-radius: 8px;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  color: white; font-size: 12px; font-weight: 500;
  white-space: nowrap; flex-shrink: 0;
}
.history-dialog-preview {
  color: #4b5563; font-size: 14px;
  overflow: hidden; text-overflow: ellipsis; white-space: nowrap;
}
.history-dialog-right {
  display: flex; align-items: center; gap: 12px;
  flex-shrink: 0;
}
.history-dialog-status {
  font-size: 12px; font-weight: 500;
  padding: 2px 8px; border-radius: 6px;
}
.status-success {
  background: #dcfce7; color: #16a34a;
}
.status-failed {
  background: #fee2e2; color: #dc2626;
}
.status-pending{
  background: #fef3c7; color: #d97706;
}
.history-dialog-time {
  color: #9ca3af; font-size: 12px;
  white-space: nowrap;
}
.history-dialog-pagination {
  margin-top: 20px;
  display: flex; justify-content: center;
}

/* ===== Hero ===== */
.hero-section {
  padding: 120px 24px 48px;
}
.hero-container {
  max-width: 800px; margin: 0 auto; text-align: center;
}
.hero-title {
  font-size: 52px; font-weight: 800; margin-bottom: 20px;
  line-height: 1.15;
}
@media (max-width: 768px) {
  .hero-title { font-size: 36px; }
}
.gradient-hero {
  background: linear-gradient(135deg, #ec4899, #a855f7, #3b82f6);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.hero-subtitle {
  font-size: 18px; color: #6b7280;
  max-width: 600px; margin: 0 auto 16px;
  line-height: 1.7;
}
.login-tip {
  font-size: 14px; color: #9ca3af;
  margin-bottom: 20px;
}
.login-tip i {
  color: #3b82f6;
  margin-right: 4px;
}
.hero-actions { display: flex; justify-content: center; gap: 12px; flex-wrap: wrap; }
.btn-start {
  display: flex; align-items: center; gap: 8px;
  padding: 14px 36px; border-radius: 999px;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  color: white; border: none;
  font-size: 16px; font-weight: 600;
  cursor: pointer; transition: all 0.2s ease;
  box-shadow: 0 6px 20px rgba(236,72,153,0.3);
}
.btn-start:hover {
  transform: scale(1.04);
  box-shadow: 0 8px 28px rgba(236,72,153,0.4);
}
.btn-tutorial {
  padding: 14px 36px; border-radius: 999px;
  border: 1.5px solid #d1d5db; background: transparent;
  font-size: 16px; font-weight: 500; color: #4b5563;
  cursor: pointer; transition: all 0.2s ease;
}
.btn-tutorial:hover { background: rgba(255,255,255,0.5); }

/* ===== Workspace ===== */
.workspace-section {
  padding: 0 24px 80px;
}
.section-container {
  max-width: 1100px; margin: 0 auto;
}
.workspace-card {
  background: white; border-radius: 24px;
  border: 2px solid transparent;
  padding: 32px; margin-bottom: 32px;
  transition: all 0.3s ease;
  position: relative;
  overflow: hidden;
}
/* 每个工作台的专属主题色 */
.workspace-script {
  border-color: rgba(236,72,153,0.15);
  box-shadow: 0 10px 25px -5px rgba(236,72,153,0.08);
}
.workspace-script:hover {
  box-shadow: 0 15px 35px -5px rgba(236,72,153,0.15);
  border-color: rgba(236,72,153,0.3);
}
.workspace-parse {
  border-color: rgba(59,130,246,0.15);
  box-shadow: 0 10px 25px -5px rgba(59,130,246,0.08);
}
.workspace-parse:hover {
  box-shadow: 0 15px 35px -5px rgba(59,130,246,0.15);
  border-color: rgba(59,130,246,0.3);
}
.workspace-character {
  border-color: rgba(34,197,94,0.15);
  box-shadow: 0 10px 25px -5px rgba(34,197,94,0.08);
}
.workspace-character:hover {
  box-shadow: 0 15px 35px -5px rgba(34,197,94,0.15);
  border-color: rgba(34,197,94,0.3);
}
.workspace-storyboard {
  border-color: rgba(249,115,22,0.15);
  box-shadow: 0 10px 25px -5px rgba(249,115,22,0.08);
}
.workspace-storyboard:hover {
  box-shadow: 0 15px 35px -5px rgba(249,115,22,0.15);
  border-color: rgba(249,115,22,0.3);
}
.workspace-header {
  display: flex; align-items: center; gap: 12px;
  margin-bottom: 24px; padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}
.ws-icon {
  width: 40px; height: 40px; border-radius: 10px;
  display: flex; align-items: center; justify-content: center;
  color: white; font-size: 20px;
}
.ws-icon-script { background: linear-gradient(135deg, #ec4899, #fb923c); }
.ws-icon-parse { background: linear-gradient(135deg, #3b82f6, #22d3ee); }
.ws-icon-character { background: linear-gradient(135deg, #22c55e, #14b8a6); }
.ws-icon-storyboard { background: linear-gradient(135deg, #f97316, #eab308); }
.ws-title {
  font-size: 24px; font-weight: 700; margin: 0;
}
.gradient-script {
  background: linear-gradient(135deg, #ec4899, #fb923c);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.gradient-parse {
  background: linear-gradient(135deg, #3b82f6, #22d3ee);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.gradient-character {
  background: linear-gradient(135deg, #22c55e, #14b8a6);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.gradient-storyboard {
  background: linear-gradient(135deg, #f97316, #eab308);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
}
.ws-desc {
  font-size: 14px; color: #9ca3af;
  margin-left: auto;
}
.ws-note {
  font-size: 11px;
  color: #ef4444;
  white-space: nowrap;
  font-weight: 500;
}
.workspace-body {
  width: 100%;
  display: flex;
  flex-direction: column;
}

/* ===== Footer ===== */
.app-footer {
  text-align: center; padding: 32px;
  color: #9ca3af; font-size: 14px;
}

/* ===== 灵感助手对话框 ===== */
.inspire-dialog {
  --el-dialog-border-radius: 20px;
}

/* ===== 使用教程弹窗 ===== */
.tutorial-dialog {
  --el-dialog-border-radius: 16px;
}
.tutorial-content {
  max-height: 60vh;
  overflow-y: auto;
  padding-right: 8px;
}
.tutorial-content h3 {
  margin: 0 0 20px 0;
  font-size: 20px;
  background: linear-gradient(135deg, #ec4899, #a855f7);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
  background-clip: text;
}
.tutorial-content h4 {
  margin: 20px 0 12px 0;
  font-size: 16px;
  color: #1f2937;
  border-left: 3px solid #ec4899;
  padding-left: 10px;
}
.tutorial-content h4:first-of-type {
  margin-top: 0;
}
.tutorial-section {
  padding: 12px 16px;
  background: #f9fafb;
  border-radius: 8px;
  margin-bottom: 12px;
}
.tutorial-section p {
  margin: 8px 0;
  font-size: 14px;
  line-height: 1.7;
  color: #4b5563;
}
.tutorial-section ul {
  margin: 8px 0;
  padding-left: 20px;
}
.tutorial-section li {
  margin: 6px 0;
  font-size: 14px;
  line-height: 1.6;
  color: #4b5563;
}
.tutorial-content .tip {
  color: #059669;
  font-size: 13px;
}
.tutorial-content .warning {
  color: #dc2626;
  font-size: 13px;
  background: #fef2f2;
  padding: 8px 12px;
  border-radius: 6px;
  border-left: 3px solid #dc2626;
}
.dialog-header {
  display: flex; justify-content: space-between; align-items: center;
  margin-bottom: 16px;
}
.dialog-header-gradient {
  font-size: 20px; font-weight: 700;
  background: linear-gradient(135deg, #a855f7, #ec4899);
  -webkit-background-clip: text; -webkit-text-fill-color: transparent;
  background-clip: text;
  display: flex; align-items: center; gap: 6px;
}
.dialog-close {
  width: 32px; height: 32px; border-radius: 50%;
  border: none; background: #f3f4f6;
  display: flex; align-items: center; justify-content: center;
  color: #6b7280; cursor: pointer;
  transition: all 0.2s ease;
}
.dialog-close:hover {
  background: #e5e7eb; color: #374151;
}
.inspire-tabs {
  --el-tabs-header-text-color: #6b7280;
  --el-tabs-active-text-color: #ec4899;
  --el-tabs-border-color: #e5e7eb;
}
.search-line {
  display: flex; gap: 10px; align-items: center;
}
.analysis-card {
  margin-top: 16px;
}
.analysis-grid {
  display: grid; grid-template-columns: repeat(2, 1fr);
  gap: 16px;
}
.analysis-cell {
  display: flex; flex-direction: column; gap: 4px;
}
.cell-key {
  font-size: 12px; color: #9ca3af;
}
.cell-val {
  font-size: 14px; color: #1f2937; font-weight: 500;
}

/* ===== 响应式优化 ===== */
@media (max-width: 1024px) {
  .nav-links { display: none; }
  .ws-desc { display: none; }
}
@media (max-width: 640px) {
  .user-badge .badge-name { display: none; }
  .user-card { width: 280px; right: -40px; }
  .btn-inspire { padding: 6px 12px; font-size: 12px; }
  .navbar-container { padding: 0 12px; }
  .workspace-card { padding: 20px; }
  .ws-desc { display: none; }
}

/* ===== 历史记录弹窗样式 ===== */
.history-dialog-list {
  max-height: 60vh;
  overflow-y: auto;
}

/* 横向布局 */
.history-row {
  display: flex;
  align-items: stretch;
  border-bottom: 1px solid #e5e7eb;
  padding: 12px 0;
}
.history-row:last-child {
  border-bottom: none;
}
.history-row.history-failed {
  background: #fef2f2;
}
.history-row:hover {
  background: #f9fafb;
}

/* 列 */
.history-col {
  padding: 0 8px;
  display: flex;
  align-items: flex-start;
  overflow: hidden;
}
.history-col-tool {
  width: 80px;
  flex-shrink: 0;
  justify-content: center;
}
.history-col-input {
  width: 180px;
  flex-shrink: 0;
}
.history-col-result {
  flex: 1;
  min-width: 200px;
}
.history-col-media {
  width: 160px;
  flex-shrink: 0;
  justify-content: flex-start;
}
.history-col-status {
  width: 60px;
  flex-shrink: 0;
  justify-content: center;
}
.history-col-time {
  width: 80px;
  flex-shrink: 0;
  justify-content: center;
}

/* 表头 */
.history-header {
  background: #f3f4f6;
  border-radius: 8px;
  padding: 10px 0;
  margin-bottom: 4px;
}
.history-header:hover {
  background: #f3f4f6;
}
.history-header .history-col {
  font-weight: 600;
  color: #374151;
  font-size: 13px;
}

/* 工具标签 */
.history-tool-tag {
  background: linear-gradient(135deg, #8b5cf6, #a78bfa);
  color: white;
  padding: 4px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}

/* 文本内容 */
.history-text-wrapper {
  position: relative;
  width: 100%;
}
.history-text-wrapper:hover .history-copy-btn {
  opacity: 1;
}
.history-text {
  margin: 0;
  font-size: 12px;
  line-height: 1.5;
  color: #4b5563;
  white-space: pre-wrap;
  word-break: break-word;
  max-height: 120px;
  overflow-y: auto;
  background: #f9fafb;
  padding: 6px 8px;
  border-radius: 4px;
  width: 100%;
  padding-right: 28px;
}
.history-text.ai-text {
  background: #faf5ff;
  border: 1px solid #e9d5ff;
}

/* 复制按钮 */
.history-copy-btn {
  position: absolute;
  top: 4px;
  right: 4px;
  background: white;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
  padding: 4px 6px;
  cursor: pointer;
  color: #6b7280;
  font-size: 11px;
  opacity: 0;
  transition: all 0.2s;
}
.history-copy-btn:hover {
  background: #8b5cf6;
  border-color: #8b5cf6;
  color: white;
}
.history-no-text {
  color: #9ca3af;
  font-size: 12px;
  font-style: italic;
}

/* 图片/视频卡片 */
.history-media-card {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 80px;
}
.history-video-card {
  gap: 8px;
}
.history-img {
  width: auto;
  height: 100%;
  max-width: 100%;
  max-height: 120px;
  object-fit: contain;
  border-radius: 4px;
  cursor: pointer;
}

/* 视频 */
.history-video-box {
  width: 100%;
  aspect-ratio: 16 / 9;
  background: #1f2937;
  border-radius: 4px;
  overflow: hidden;
}
.history-video {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 链接 */
.history-link {
  color: #8b5cf6;
  font-size: 12px;
  text-decoration: none;
}
.history-link:hover {
  text-decoration: underline;
}

/* 状态 */
.history-status {
  padding: 2px 6px;
  border-radius: 8px;
  font-size: 11px;
  font-weight: 500;
  white-space: nowrap;
}
.status-success {
  background: #dcfce7;
  color: #16a34a;
}
.status-failed {
  background: #fee2e2;
  color: #dc2626;
}
.status-pending {
  background: #fef3c7;
  color: #d97706;
}

/* 时间 */
.history-time {
  color: #9ca3af;
  font-size: 11px;
  white-space: nowrap;
}
</style>
