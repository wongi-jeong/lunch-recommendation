<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useFavorites } from '@/composables/useFavorites'
import profileIcon from '@/assets/icon-profile-default.svg'
import logoImage from '@/assets/logo-mechu.svg'
import profileAvatar1 from '@/assets/avatar-1.png'
import profileAvatar2 from '@/assets/avatar-2.png'
import profileAvatar3 from '@/assets/avatar-3.png'
import profileAvatar4 from '@/assets/avatar-4.png'
import profileAvatar5 from '@/assets/avatar-5.png'
import profileAvatar6 from '@/assets/avatar-6.png'
import profileAvatar7 from '@/assets/avatar-7.png'
import profileAvatar8 from '@/assets/avatar-8.png'

const router = useRouter()
const route = useRoute()
const { isLoggedIn, clearAuth, getToken, profileImageIndex } = useAuth()
const { resetOnLogout } = useFavorites()

const profileOptions = [profileAvatar1, profileAvatar2, profileAvatar3, profileAvatar4, profileAvatar5, profileAvatar6, profileAvatar7, profileAvatar8]
const headerProfileImage = computed(() => {
  if (!isLoggedIn.value) return profileIcon
  const idx = Math.max(0, Math.min(7, Number(profileImageIndex.value) || 0))
  return profileOptions[idx]
})

const notifications = ref([])
const showNotificationPopup = ref(false)
const notificationWrapperRef = ref(null)
let notificationTimer = null

/** 표시되는 최대 5개 알림 중 미읽음 개수. 배지 표시/숫자에 사용 */
const displayedUnreadCount = computed(() =>
  notifications.value
    .slice(0, 5)
    .filter((n) => !n.isRead)
    .length
)
const hasUnreadNotification = computed(() => displayedUnreadCount.value > 0)

const VOTE_TTL_MS = 7 * 24 * 60 * 60 * 1000

const syncNotifications = async () => {
  const token = getToken()
  if (!token) {
    notifications.value = []
    return
  }

  const now = Date.now()
  const endedVotes = []

  // 1) 로컬에 남아 있는 "내가 만든 투표"들에 대해서는
  //    타이머 기반 자동 종료 및 TTL 정리만 수행한다.
  const keys = Object.keys(localStorage).filter((k) => k.startsWith('vote_'))

  for (const key of keys) {
    try {
      const vote = JSON.parse(localStorage.getItem(key))
      if (!vote || String(vote.createdByMemberId ?? '') !== String(token)) continue

      // 오래된 종료 투표는 로컬에서만 정리
      if (vote.ended && vote.endedAt) {
        const endedAtTime = new Date(vote.endedAt).getTime()
        if (!Number.isNaN(endedAtTime) && now - endedAtTime > VOTE_TTL_MS) {
          localStorage.removeItem(key)
          continue
        }
      }

      let ended = !!vote.ended

      // 타이머 기반 자동 종료 (페이지를 보고 있지 않아도 동작)
      if (!ended && vote.timer && vote.timer !== 'none' && vote.createdAt) {
        const durationMs =
          {
            '10min': 10 * 60 * 1000,
            '30min': 30 * 60 * 1000,
            '1hour': 60 * 60 * 1000
          }[vote.timer] || 0

        if (durationMs > 0) {
          const endTime = new Date(vote.createdAt).getTime() + durationMs
          if (!Number.isNaN(endTime) && now >= endTime) {
            ended = true
            vote.ended = true
            vote.endedAt = new Date(endTime).toISOString()
            localStorage.setItem(key, JSON.stringify(vote))

            try {
              await fetch(`/api/votes/${vote.id}/end`, {
                method: 'PATCH',
                headers: { 'X-Auth-Token': token }
              })
            } catch {
              // 백엔드 동기화 실패는 무시
            }
          }
        }
      }
    } catch {
      // corrupt entry, skip
    }
  }

  // 2) 실제 알림 목록은 서버 기준으로 구성한다.
  try {
    const res = await fetch('/api/votes/me/ended', {
      headers: { 'X-Auth-Token': token }
    })

    if (!res.ok) {
      if (res.status === 401) {
        notifications.value = []
        return
      }
    } else {
      const list = await res.json()
      if (Array.isArray(list)) {
        list.forEach((vote) => {
          if (!vote || !vote.id) return
          const endedAt = vote.endedAt || vote.createdAt
          if (!endedAt) return
          const endedAtTime = new Date(endedAt).getTime()
          if (!Number.isNaN(endedAtTime) && now - endedAtTime > VOTE_TTL_MS) {
            return
          }
          endedVotes.push({
            id: vote.id,
            title: vote.title || '점심 메뉴 투표',
            endedAt,
            isRead: vote.isRead === true
          })
        })
      }
    }
  } catch {
    // 서버 오류 시에는 기존 알림 상태를 유지
  }

  endedVotes.sort(
    (a, b) => new Date(b.endedAt).getTime() - new Date(a.endedAt).getTime()
  )

  notifications.value = endedVotes
}

const toggleNotificationPopup = async () => {
  if (!isLoggedIn.value) {
    router.push('/login')
    return
  }

  if (!showNotificationPopup.value) {
    await syncNotifications()
  }

  showNotificationPopup.value = !showNotificationPopup.value
}

const handleGlobalClick = (event) => {
  if (!showNotificationPopup.value) return
  const wrapper = notificationWrapperRef.value
  if (!wrapper) return
  if (wrapper.contains(event.target)) return
  showNotificationPopup.value = false
}

const goToVoteResultFromNotification = async (id) => {
  showNotificationPopup.value = false

  if (id) {
    const token = getToken()
    if (token) {
      try {
        await fetch(`/api/votes/${id}/notification-read`, {
          method: 'PATCH',
          headers: { 'X-Auth-Token': token }
        })
      } catch {
        // 실패해도 화면에서는 읽음 처리 유지
      }
    }

    notifications.value = notifications.value.map((item) =>
      item.id === id ? { ...item, isRead: true } : item
    )

    router.push(`/vote/${id}`)
  }
}

onMounted(() => {
  getToken()
  syncNotifications()
  notificationTimer = setInterval(syncNotifications, 15000)
  window.addEventListener('click', handleGlobalClick)
})

onUnmounted(() => {
  if (notificationTimer) {
    clearInterval(notificationTimer)
    notificationTimer = null
  }
  window.removeEventListener('click', handleGlobalClick)
})

const handleAuthButtonClick = () => {
  if (isLoggedIn.value) {
    clearAuth()
    resetOnLogout()
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
  { id: 'favorites', path: '/my/saved', label: '즐겨찾기' }
]

const visibleTabs = computed(() =>
  isLoggedIn.value ? tabs : tabs.filter((t) => t.id !== 'favorites')
)

const activeTabId = computed(() => {
  const path = route.path
  const section = route.query?.section

  if (path === '/nearby') return 'near'
  if (path === '/ai') return 'ai'
  // 마이페이지 내 저장(/my?section=saved 또는 /my/saved)이면 즐겨찾기 탭 활성
  if (path === '/my' && section === 'saved') return 'favorites'
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
        <div class="notification-wrapper" ref="notificationWrapperRef">
          <button
            class="user-button notification-button"
            type="button"
            aria-label="종료된 투표 알림"
            @click="toggleNotificationPopup"
          >
            <p class="user-button-text">알림</p>
            <span
              v-if="hasUnreadNotification"
              class="notification-badge"
              aria-label="읽지 않은 알림 {{ displayedUnreadCount }}개"
            >
              {{ displayedUnreadCount > 9 ? '9+' : displayedUnreadCount }}
            </span>
          </button>
          <Transition name="popover-fade">
            <div
              v-if="showNotificationPopup"
              class="notification-popup"
            >
              <p class="notification-title">종료된 투표</p>
              <p
                v-if="notifications.length === 0"
                class="notification-empty"
              >
                새로운 종료된 투표가 없습니다.
              </p>
              <ul
                v-else
                class="notification-list"
              >
                <li
                  v-for="item in notifications.slice(0, 5)"
                  :key="item.id"
                  class="notification-item"
                >
                  <button
                    type="button"
                    class="notification-item-btn"
                    :class="{ 'notification-item-btn-unread': !item.isRead }"
                    @click="goToVoteResultFromNotification(item.id)"
                  >
                    <span
                      class="notification-item-title"
                      :class="{ 'notification-item-title-unread': !item.isRead }"
                    >
                      {{ item.title }}
                    </span>
                    <span
                      class="notification-item-meta"
                      :class="{ 'notification-item-meta-unread': !item.isRead }"
                    >
                      {{ item.isRead ? '투표가 종료되었습니다' : '새로운 종료된 투표입니다' }}
                    </span>
                  </button>
                </li>
              </ul>
            </div>
          </Transition>
        </div>
        <button class="user-button" type="button" aria-label="마이페이지" @click="goToMyPage">
          <img :src="headerProfileImage" alt="" class="profile-icon" :class="{ 'profile-icon--avatar': isLoggedIn }" aria-hidden="true" />
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
  overflow: visible;
  width: 100%;
  background-color: #ffffff;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.04);
  position: sticky;
  top: 0;
  z-index: 1000;
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

.notification-wrapper {
  position: relative;
}

.notification-button {
  position: relative;
}

.notification-badge {
  position: absolute;
  top: 4px;
  right: 4px;
  min-width: 18px;
  height: 18px;
  padding: 0 5px;
  border-radius: 9px;
  background-color: #ff5531;
  color: #fff;
  font-family: 'Pretendard', sans-serif;
  font-size: 11px;
  font-weight: 700;
  line-height: 18px;
  text-align: center;
  box-sizing: border-box;
}

.notification-popup {
  position: absolute;
  top: 56px;
  right: 0;
  width: 280px;
  padding: 12px 0;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  z-index: 1100;
}

.notification-title {
  margin: 0 16px 8px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  color: #202124;
}

.notification-empty {
  margin: 4px 16px 0;
  font-family: 'Pretendard', sans-serif;
  font-size: 14px;
  color: #5f6368;
}

.notification-list {
  list-style: none;
  margin: 4px 0 0;
  padding: 0;
  max-height: 260px;
  overflow-y: auto;
}

.notification-item {
  margin: 0;
  padding: 0;
}

.notification-item-btn {
  width: 100%;
  border: none;
  background: transparent;
  padding: 8px 16px;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 2px;
  cursor: pointer;
}

.notification-item-btn:hover {
  background-color: rgba(0, 0, 0, 0.03);
}

.notification-item-btn-unread {
  background-color: rgba(255, 85, 49, 0.04);
}

.notification-item-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  color: #202124;
}

.notification-item-title-unread {
  font-weight: 700;
}

.notification-item-meta {
  font-family: 'Pretendard', sans-serif;
  font-size: 12px;
  color: #5f6368;
}

.notification-item-meta-unread {
  color: #ff5531;
}

.popover-fade-enter-active,
.popover-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}

.popover-fade-enter-from,
.popover-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
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

.profile-icon--avatar {
  border-radius: 50%;
  object-fit: cover;
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
