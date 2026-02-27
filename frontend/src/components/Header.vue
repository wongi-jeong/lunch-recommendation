<script setup>
import { computed, ref, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import profileIcon from '@/assets/profile-icon.svg'
import logoImage from '@/assets/logo-mechu.svg'

const router = useRouter()
const route = useRoute()

// TODO: 회원 테이블/API 연동 후 세션 체크 로직 구현
// 현재는 sessionStorage에 authToken 존재 여부로 로그인 상태 판단
const isLoggedIn = ref(false)

const checkAuthSession = () => {
  isLoggedIn.value = !!sessionStorage.getItem('authToken')
}

onMounted(() => {
  checkAuthSession()
})

const handleAuthButtonClick = () => {
  if (isLoggedIn.value) {
    sessionStorage.removeItem('authToken')
    isLoggedIn.value = false
    // TODO: API 연동 시 로그아웃 요청 추가
  } else {
    router.push('/login')
  }
}

const tabs = [
  { id: 'ai', path: '/ai', label: 'AI 추천' },
  { id: 'near', path: '/nearby', label: '내 근처 추천' },
  { id: 'favorites', path: '/', label: '즐겨찾기' }
]

const visibleTabs = computed(() =>
  isLoggedIn.value ? tabs : tabs.filter((t) => t.id !== 'favorites')
)

const activeTab = computed(() => {
  if (route.path === '/nearby') return '내 근처 추천'
  if (route.path === '/ai') return 'AI 추천'
  if (route.path === '/' && isLoggedIn.value) return '즐겨찾기'
  if (route.path === '/') return null
  return tabs.find((t) => route.path.startsWith(t.path))?.label ?? null
})

const setActiveTab = (tab) => {
  if (tab.path) router.push(tab.path)
}
</script>

<template>
  <header class="header">
    <div class="header-container">
      <div class="header-left">
        <button class="logo" type="button" aria-label="메인 페이지로 이동" @click="router.push('/')">
          <img :src="logoImage" alt="MECHU" class="logo-image" />
        </button>
        <nav class="tabs">
          <div
            v-for="tab in visibleTabs"
            :key="tab.id"
            class="tab"
            :class="{ active: activeTab === tab.label }"
            @click="setActiveTab(tab)"
          >
            <div class="tab-content">
              <p
                class="tab-text"
                :class="{ 'tab-text-active': activeTab === tab.label }"
              >
                {{ tab.label }}
              </p>
            </div>
          </div>
        </nav>
      </div>
      <div class="header-right">
        <button class="user-button">
          <p class="user-button-text">알림</p>
        </button>
        <button class="user-button">
          <img :src="profileIcon" alt="프로필" class="profile-icon" />
          <p class="user-button-text">마이</p>
        </button>
        <button class="user-button" @click="handleAuthButtonClick">
          <p class="user-button-text">{{ isLoggedIn ? '로그아웃' : '로그인' }}</p>
        </button>
      </div>
    </div>
  </header>
</template>

<style scoped>
.header {
  display: flex;
  height: 80px;
  align-items: center;
  justify-content: space-between;
  overflow: hidden;
  padding: 0 380px;
  width: 100%;
  background-color: white;
}

.header-container {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
  width: 1200px;
}

.header-left {
  display: flex;
  gap: 32px;
  height: 100%;
  align-items: center;
}

.logo {
  height: 35px;
  width: 92px;
  display: flex;
  align-items: center;
  flex-shrink: 0;
  padding: 0;
  background: none;
  border: none;
  cursor: pointer;
}

.logo:hover {
  opacity: 0.85;
}

.logo-image {
  width: 100%;
  height: 100%;
  object-fit: contain;
  display: block;
}

.tabs {
  display: flex;
  gap: 24px;
  height: 100%;
  align-items: center;
}

.tab {
  display: flex;
  height: 56px;
  align-items: center;
  justify-content: center;
  padding: 0 20px;
  border-radius: 12px;
  flex-shrink: 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.tab:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.tab-content {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
}

.tab.active .tab-content {
  border-bottom: 2px solid #ff5531;
}

.tab-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 22px;
  line-height: 1.35;
  color: #5f6368;
  white-space: pre;
  margin: 0;
}

.tab-text-active {
  font-weight: 700;
  color: #ff5531;
}

.header-right {
  display: flex;
  gap: 20px;
  height: 100%;
  align-items: center;
}

.user-button {
  display: flex;
  gap: 8px;
  height: 48px;
  align-items: center;
  justify-content: center;
  padding: 12px 16px;
  border-radius: 14px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background-color 0.2s;
}

.user-button:hover {
  background-color: rgba(0, 0, 0, 0.02);
}

.profile-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: block;
}

.user-button-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  white-space: pre;
  margin: 0;
}
</style>
