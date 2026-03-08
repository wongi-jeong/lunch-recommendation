<script setup>
import { computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import profileIcon from '@/assets/profile-icon.svg'
import logoImage from '@/assets/logo-mechu.svg'

const router = useRouter()
const route = useRoute()
const { isLoggedIn, clearAuth, getToken } = useAuth()

onMounted(() => {
  getToken()
})

const handleAuthButtonClick = () => {
  if (isLoggedIn.value) {
    clearAuth()
    if (route.meta.requiresAuth) {
      router.replace('/')
    }
  } else {
    router.push('/login')
  }
}

const goToMyPage = () => {
  if (isLoggedIn.value) {
    router.push('/my')
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

const activeTabId = computed(() => {
  const path = route.path

  if (path === '/nearby') return 'near'
  if (path === '/ai') return 'ai'
  // 메인(/)은 상단 카테고리에 해당하지 않음 → 어떤 탭도 강조하지 않음
  if (path === '/') return null

  return tabs.find((t) => t.path !== '/' && path.startsWith(t.path))?.id ?? null
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
        <nav class="tabs" aria-label="페이지 이동">
          <div
            v-for="tab in visibleTabs"
            :key="tab.id"
            class="tab"
            :class="{ active: activeTabId === tab.id }"
            :aria-current="activeTabId === tab.id ? 'page' : undefined"
            @click="setActiveTab(tab)"
          >
            <div class="tab-content">
              <p
                class="tab-text"
                :class="{ 'tab-text-active': activeTabId === tab.id }"
              >
                {{ tab.label }}
              </p>
            </div>
          </div>
        </nav>
      </div>
      <div class="header-right">
        <button class="user-button" type="button">
          <p class="user-button-text">알림</p>
        </button>
        <button class="user-button" type="button" aria-label="마이페이지" @click="goToMyPage">
          <img :src="profileIcon" alt="" class="profile-icon" aria-hidden="true" />
          <p class="user-button-text">마이</p>
        </button>
        <button class="user-button" type="button" @click="handleAuthButtonClick">
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
  justify-content: center;
  overflow: hidden;
  width: 100%;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
}

.header-container {
  display: flex;
  height: 100%;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  max-width: 1200px;
  padding: 0 32px;
  box-sizing: border-box;
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

@media (max-width: 1024px) {
  .header {
    height: 72px;
  }

  .header-container {
    padding: 0 20px;
  }

  .tab-text {
    font-size: 18px;
  }

  .user-button-text {
    font-size: 16px;
  }
}

@media (max-width: 768px) {
  .header {
    height: 64px;
  }

  .header-left {
    gap: 20px;
  }

  .tabs {
    gap: 16px;
  }

  .tab {
    height: 44px;
    padding: 0 12px;
  }
}
</style>
