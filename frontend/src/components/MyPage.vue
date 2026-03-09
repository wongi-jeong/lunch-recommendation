<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import profileIcon from '@/assets/profile-icon.svg'
import profileEditIcon from '@/assets/profile-edit-icon.svg'
import profileAvatar1 from '@/assets/profile-avatar-1.png'
import profileAvatar2 from '@/assets/profile-avatar-2.png'
import profileAvatar3 from '@/assets/profile-avatar-3.png'
import profileAvatar4 from '@/assets/profile-avatar-4.png'
import profileAvatar5 from '@/assets/profile-avatar-5.png'
import profileAvatar6 from '@/assets/profile-avatar-6.png'
import profileAvatar7 from '@/assets/profile-avatar-7.png'
import profileAvatar8 from '@/assets/profile-avatar-8.png'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'
import externalLinkIcon from '@/assets/external-link-icon.svg'
import ProfileChangeModal from '@/components/ProfileChangeModal.vue'

const route = useRoute()
const { getToken, profileImageIndex: authProfileIndex, setProfileImageIndex: setAuthProfileIndex } = useAuth()
const profileOptions = [profileAvatar1, profileAvatar2, profileAvatar3, profileAvatar4, profileAvatar5, profileAvatar6, profileAvatar7, profileAvatar8]

const currentProfileImage = ref(profileAvatar1)
const showProfileChangeModal = ref(false)
const memberEmail = ref('')
const emailLoadError = ref('')
const ongoingVotes = ref([])
const endedVotes = ref([])

const PAGE_SIZE = 5
const ongoingPage = ref(1)
const endedPage = ref(1)

// 룰렛 결과 히스토리 (로컬 저장)
const ROULETTE_HISTORY_KEY_PREFIX = 'rouletteHistory:'
const rouletteHistory = ref([])

// 즐겨찾기 (최근 즐겨찾기 한 장소) - 회원 DB 기반
const memberFavorites = ref([])

const latestFavorites = computed(() => {
  if (!memberFavorites.value || memberFavorites.value.length === 0) return []
  return memberFavorites.value.slice(0, 3)
})

const getFavoriteImage = (item) => {
  if (item?.photoName) {
    return `/api/restaurants/photo?name=${encodeURIComponent(item.photoName)}`
  }
  return defaultThumbnail
}

const getFavoriteCategory = (item) => {
  if (!item) return '식당'
  if (Array.isArray(item.categories) && item.categories.length > 0) {
    return item.categories[0]
  }
  return '식당'
}

const getFavoriteLink = (item) => {
  if (!item) return ''
  if (item.googleMapsUri) return item.googleMapsUri
  if (item.latitude != null && item.longitude != null) {
    const lat = Number(item.latitude)
    const lng = Number(item.longitude)
    if (!Number.isNaN(lat) && !Number.isNaN(lng)) {
      return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`
    }
  }
  return ''
}

const openFavoriteExternal = (item) => {
  const url = getFavoriteLink(item)
  if (!url) return
  window.open(url, '_blank', 'noopener')
}

const handleToggleFavoriteFromMyPage = (item) => {
  if (!item?.id) return
  const token = getToken()
  if (!token) return
  fetch(`/api/favorites/${encodeURIComponent(item.id)}`, {
    method: 'DELETE',
    headers: {
      'X-Auth-Token': token
    }
  })
    .then(() => fetchMemberFavorites())
    .catch(() => fetchMemberFavorites())
}

const totalOngoingPages = computed(() => {
  return Math.max(1, Math.ceil(ongoingVotes.value.length / PAGE_SIZE))
})

const totalEndedPages = computed(() => {
  return Math.max(1, Math.ceil(endedVotes.value.length / PAGE_SIZE))
})

const pagedOngoingVotes = computed(() => {
  const start = (ongoingPage.value - 1) * PAGE_SIZE
  return ongoingVotes.value.slice(start, start + PAGE_SIZE)
})

const ongoingPlaceholderCount = computed(() => {
  const count = PAGE_SIZE - pagedOngoingVotes.value.length
  return count > 0 ? count : 0
})

const sortedEndedVotes = computed(() => {
  return [...endedVotes.value].sort((a, b) => {
    const aTime = new Date(a.endedAt || a.createdAt || 0).getTime()
    const bTime = new Date(b.endedAt || b.createdAt || 0).getTime()
    return bTime - aTime
  })
})

const pagedEndedVotes = computed(() => {
  const start = (endedPage.value - 1) * PAGE_SIZE
  return sortedEndedVotes.value.slice(start, start + PAGE_SIZE)
})

const combinedRecentResults = computed(() => {
  const voteItems = sortedEndedVotes.value.map((v) => ({
    type: 'vote',
    id: v.id,
    title: v.title,
    createdAt: v.endedAt || v.createdAt
  }))

  const rouletteItems = (rouletteHistory.value || []).map((r) => ({
    type: 'roulette',
    id: r.id,
    winnerName: r.winnerName,
    shareData: r.shareData,
    createdAt: r.createdAt
  }))

  return [...voteItems, ...rouletteItems]
    .filter((item) => item.createdAt)
    .sort((a, b) => {
      const aTime = new Date(a.createdAt || 0).getTime()
      const bTime = new Date(b.createdAt || 0).getTime()
      return bTime - aTime
    })
})

const limitedRecentResults = computed(() => {
  return combinedRecentResults.value.slice(0, 3)
})

function applyProfileIndex(idx) {
  const n = typeof idx === 'number' ? idx : parseInt(idx, 10)
  if (!Number.isNaN(n) && n >= 0 && n < profileOptions.length) {
    currentProfileImage.value = profileOptions[n]
    setAuthProfileIndex(n)
  }
}

async function fetchMe() {
  const token = getToken()
  if (!token) {
    emailLoadError.value = '로그인이 필요합니다.'
    return
  }
  try {
    const res = await fetch('/api/auth/me', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      emailLoadError.value = data.message || '회원 정보를 불러오지 못했습니다.'
      return
    }
    const data = await res.json()
    memberEmail.value = data.email || ''
    const idx = data.profileImageIndex ?? data.profile_image_index
    applyProfileIndex(idx)
  } catch (err) {
    emailLoadError.value = '네트워크 오류가 발생했습니다.'
  }
}

async function fetchOngoingVotes() {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch('/api/votes/me/ongoing', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (res.ok) {
      const data = await res.json()
      ongoingVotes.value = Array.isArray(data) ? data : []
    } else {
      ongoingVotes.value = []
    }
  } catch {
    ongoingVotes.value = []
  }
}

async function fetchEndedVotes() {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch('/api/votes/me/ended', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (res.ok) {
      const data = await res.json()
      endedVotes.value = Array.isArray(data) ? data : []
    } else {
      endedVotes.value = []
    }
  } catch {
    endedVotes.value = []
  }
}

async function loadRouletteHistory() {
  const token = getToken()
  if (!token) {
    rouletteHistory.value = []
    return
  }
  try {
    const res = await fetch('/api/roulette-history/me', {
      headers: {
        'X-Auth-Token': token
      },
      cache: 'no-store'
    })
    if (!res.ok) {
      rouletteHistory.value = []
      return
    }
    const data = await res.json()
    rouletteHistory.value = Array.isArray(data) ? data : []
  } catch {
    rouletteHistory.value = []
  }
}

async function fetchMemberFavorites() {
  const token = getToken()
  if (!token) {
    memberFavorites.value = []
    return
  }
  try {
    const res = await fetch('/api/favorites/me?limit=3', {
      headers: {
        'X-Auth-Token': token
      },
      cache: 'no-store'
    })
    if (!res.ok) {
      memberFavorites.value = []
      return
    }
    const data = await res.json()
    memberFavorites.value = Array.isArray(data) ? data : []
  } catch {
    memberFavorites.value = []
  }
}

watch(ongoingVotes, () => {
  ongoingPage.value = 1
})

watch(endedVotes, () => {
  endedPage.value = 1
})

const setOngoingPage = (page) => {
  const target = Number(page)
  if (Number.isNaN(target)) return
  if (target < 1 || target > totalOngoingPages.value) return
  ongoingPage.value = target
}

const goPrevOngoingPage = () => {
  if (ongoingPage.value > 1) {
    setOngoingPage(ongoingPage.value - 1)
  }
}

const goNextOngoingPage = () => {
  if (ongoingPage.value < totalOngoingPages.value) {
    setOngoingPage(ongoingPage.value + 1)
  }
}

const ongoingPageItems = computed(() => {
  const total = totalOngoingPages.value
  const items = []

  // 총 페이지가 10 이하이면 모두 노출
  if (total <= 10) {
    for (let i = 1; i <= total; i += 1) {
      items.push({ type: 'number', value: i, label: String(i), key: `page-${i}` })
    }
    return items
  }

  // 총 페이지가 10을 초과하면:
  // 1~8 페이지, 9번째는 ..., 10번째는 마지막 페이지
  for (let i = 1; i <= 8; i += 1) {
    items.push({ type: 'number', value: i, label: String(i), key: `page-${i}` })
  }
  items.push({ type: 'ellipsis', value: null, label: '...', key: 'page-ellipsis' })
  items.push({ type: 'number', value: total, label: String(total), key: `page-${total}` })

  return items
})

const goPrevEndedPage = () => {
  if (endedPage.value > 1) {
    endedPage.value -= 1
  }
}

const goNextEndedPage = () => {
  if (endedPage.value < totalEndedPages.value) {
    endedPage.value += 1
  }
}

function formatVoteDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true
  })
}

watch(() => route.path, (path) => {
  if (path === '/my') {
    applyProfileIndex(authProfileIndex.value)
    fetchMe()
    fetchOngoingVotes()
    fetchEndedVotes()
    loadRouletteHistory()
    fetchMemberFavorites()
  }
}, { immediate: true })

const handlePageshow = () => {
  if (route.path === '/my') {
    fetchMe()
    fetchOngoingVotes()
    fetchEndedVotes()
    loadRouletteHistory()
    fetchMemberFavorites()
  }
}

onMounted(() => {
  window.addEventListener('pageshow', handlePageshow)
})

onUnmounted(() => {
  window.removeEventListener('pageshow', handlePageshow)
})

// 사이드 프로필에 표시할 이름: 이메일에서 @ 앞부분 / @이후
const profileDisplayParts = computed(() => {
  const email = memberEmail.value
  if (!email) return { name: '-', domain: '' }
  const at = email.indexOf('@')
  if (at < 0) return { name: email, domain: '' }
  return { name: email.slice(0, at), domain: email.slice(at) }
})

const openProfileChangeModal = () => {
  showProfileChangeModal.value = true
}

const closeProfileChangeModal = () => {
  showProfileChangeModal.value = false
}

const onProfileConfirm = async (selectedIndex) => {
  const index = typeof selectedIndex === 'number' ? selectedIndex : profileOptions.indexOf(selectedIndex)
  if (import.meta.env.DEV) {
    console.log('[MyPage] onProfileConfirm 호출', { selectedIndex, index, profileOptionsLength: profileOptions.length })
  }
  if (index < 0 || index >= profileOptions.length) {
    showProfileChangeModal.value = false
    return
  }
  const selected = profileOptions[index]
  currentProfileImage.value = selected
  showProfileChangeModal.value = false

  const token = getToken()
  if (import.meta.env.DEV) {
    console.log('[MyPage] PATCH 요청 예정', { token: token ? `${token.slice(0, 4)}...` : null, profileImageIndex: index })
  }
  if (token) {
    try {
      const res = await fetch('/api/auth/me', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': token
        },
        body: JSON.stringify({ profileImageIndex: index }),
        cache: 'no-store'
      })
      const data = await res.json().catch(() => ({}))
      if (import.meta.env.DEV) {
        console.log('[MyPage] PATCH 응답', { ok: res.ok, status: res.status, data })
      }
      if (res.ok) {
        emailLoadError.value = ''
        const idx = data.profileImageIndex ?? data.profile_image_index
        applyProfileIndex(idx)
      } else {
        emailLoadError.value = '프로필 저장에 실패했습니다. 다시 시도해주세요.'
      }
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('[MyPage] PATCH 실패', err)
      }
      emailLoadError.value = '프로필 저장에 실패했습니다. 다시 시도해주세요.'
    }
  }
}

const currentProfileIndex = computed(() => {
  const idx = profileOptions.indexOf(currentProfileImage.value)
  return idx >= 0 ? idx : 0
})
</script>

<template>
  <div class="mypage">
    <aside class="side-menu" aria-label="마이페이지 메뉴">
      <div class="side-menu-contents">
        <div class="profile">
          <div class="profile-thumbnail">
            <img :src="currentProfileImage" alt="" class="profile-thumbnail-img" aria-hidden="true" />
          </div>
          <p class="profile-name">
            <template v-if="memberEmail">{{ profileDisplayParts.name }}<br />{{ profileDisplayParts.domain }}</template>
            <template v-else-if="emailLoadError">{{ emailLoadError }}</template>
            <template v-else>로딩 중...</template>
          </p>
        </div>
        <div class="divider" aria-hidden="true"></div>
        <div class="menu-1depth">
          <p class="menu-1depth-title">마이페이지</p>
        </div>
        <nav class="sub-menu" aria-label="마이페이지 하위 메뉴">
          <div class="sub-menu-item sub-menu-item--selected">내 정보</div>
          <div class="sub-menu-item">개인정보동의</div>
          <div class="sub-menu-item">회원탈퇴</div>
        </nav>
        <div class="menu-1depth menu-1depth--with-arrow">
          <p class="menu-1depth-title">내 저장</p>
          <span class="menu-arrow" aria-hidden="true">▼</span>
        </div>
        <div class="menu-1depth menu-1depth--with-arrow">
          <p class="menu-1depth-title">지난 결과 내역</p>
          <span class="menu-arrow" aria-hidden="true">▼</span>
        </div>
      </div>
    </aside>

    <main class="mypage-main">
      <section class="section" aria-labelledby="section-info-title">
        <h2 id="section-info-title" class="section-title">내 정보</h2>
        <div class="section-contents section-contents--row">
          <div class="thumbnail-wrap">
            <div class="thumbnail thumbnail--large">
              <img :src="currentProfileImage" alt="" class="thumbnail-img" aria-hidden="true" />
              <button
                type="button"
                class="thumbnail-edit-btn"
                aria-label="프로필 사진 변경"
                @click="openProfileChangeModal"
              >
                <img :src="profileEditIcon" alt="" class="thumbnail-edit-icon" width="32" height="32" aria-hidden="true" />
              </button>
            </div>
          </div>
          <div class="account">
            <div class="account-row">
              <span class="account-label">이메일</span>
              <span class="account-value">{{ memberEmail || (emailLoadError || '로딩 중...') }}</span>
            </div>
            <div class="account-row">
              <span class="account-label">비밀번호</span>
              <span class="account-value">********</span>
              <button type="button" class="btn-outlined">비밀번호 변경</button>
            </div>
          </div>
        </div>
      </section>

      <section class="section" aria-labelledby="section-vote-title">
        <h2 id="section-vote-title" class="section-title">현재 진행중인 투표</h2>
        <div v-if="ongoingVotes.length === 0" class="section-contents section-contents--empty">
          <p class="empty-text">진행중인 투표가 없어요</p>
        </div>
        <div v-else class="section-contents section-contents--list section-contents--ongoing-votes">
          <router-link
            v-for="v in pagedOngoingVotes"
            :key="v.id"
            :to="`/vote/${v.id}`"
            class="vote-list-item"
          >
            <div class="vote-list-main">
              <span class="result-label result-label--vote">투표</span>
              <span class="vote-list-title">{{ v.title }}</span>
            </div>
            <span class="vote-list-date">{{ formatVoteDate(v.createdAt) }}</span>
          </router-link>
          <div
            v-for="n in ongoingPlaceholderCount"
            :key="`ongoing-placeholder-${n}`"
            class="vote-list-item vote-list-item--placeholder"
          >
            <span class="vote-list-title">새로운 투표를 만들어보세요</span>
            <span class="vote-list-date"> </span>
          </div>
        </div>
        <div v-if="ongoingVotes.length > PAGE_SIZE" class="pagination">
          <button
            type="button"
            class="pagination-arrow"
            aria-label="이전"
            :disabled="ongoingPage === 1"
            @click="goPrevOngoingPage"
          >
            ‹
          </button>
          <button
            v-for="item in ongoingPageItems"
            :key="item.key"
            type="button"
            class="pagination-page"
            :class="{
              'pagination-page--current': item.type === 'number' && item.value === ongoingPage,
              'pagination-page--ellipsis': item.type === 'ellipsis'
            }"
            :disabled="item.type === 'ellipsis'"
            @click="item.type === 'number' ? setOngoingPage(item.value) : null"
          >
            {{ item.label }}
          </button>
          <button
            type="button"
            class="pagination-arrow"
            aria-label="다음"
            :disabled="ongoingPage === totalOngoingPages"
            @click="goNextOngoingPage"
          >
            ›
          </button>
        </div>
      </section>

      <section class="section" aria-labelledby="section-fav-title">
        <div class="section-head">
          <h2 id="section-fav-title" class="section-title">최근 즐겨찾기 한 장소</h2>
          <button type="button" class="btn-ghost">전체보기</button>
        </div>
        <div v-if="latestFavorites.length === 0" class="section-contents section-contents--empty">
          <p class="empty-text">즐겨찾기한 장소가 없어요</p>
        </div>
        <div v-else class="section-contents">
          <ul class="favorites-list">
            <li
              v-for="item in latestFavorites"
              :key="item.id"
              class="favorite-item"
            >
              <div class="favorite-card">
                <div class="favorite-thumb-wrap">
                  <img
                    :src="getFavoriteImage(item)"
                    :alt="item.name"
                    class="favorite-thumb"
                    @error="(e) => { e.target.src = defaultThumbnail }"
                  />
                  <button
                    type="button"
                    class="favorite-heart-button"
                    aria-label="즐겨찾기 해제"
                    @click.stop="handleToggleFavoriteFromMyPage(item)"
                  >
                    <svg
                      class="favorite-heart-icon"
                      viewBox="0 0 24 24"
                      aria-hidden="true"
                    >
                      <path
                        class="favorite-heart-path"
                        d="M12 20.25c-.32 0-.64-.1-.9-.3-.76-.56-1.45-1.09-2.08-1.56-2.53-1.92-4.42-3.36-4.42-5.89C4.6 9.5 6.1 8 7.92 8c1.12 0 2.12.52 2.78 1.39L12 10.9l1.3-1.51C13.96 8.52 14.96 8 16.08 8 17.9 8 19.4 9.5 19.4 12.5c0 2.53-1.89 3.97-4.42 5.89-.63.47-1.32 1-2.08 1.56-.26.2-.58.3-.9.3Z"
                      />
                    </svg>
                  </button>
                </div>
                <div class="favorite-info">
                  <div class="favorite-header-row">
                    <p class="favorite-name">
                      {{ item.name }}
                    </p>
                    <button
                      type="button"
                      class="favorite-external-btn"
                      aria-label="지도에서 보기"
                      @click="openFavoriteExternal(item)"
                    >
                      <img
                        :src="externalLinkIcon"
                        alt=""
                        class="favorite-external-icon"
                        aria-hidden="true"
                      />
                    </button>
                  </div>
                  <p class="favorite-category">
                    {{ getFavoriteCategory(item) }}
                  </p>
                </div>
              </div>
            </li>
          </ul>
        </div>
      </section>

      <section class="section section--bottom-space" aria-labelledby="section-result-title">
        <div class="section-head">
          <h2 id="section-result-title" class="section-title">최근 진행한 결과</h2>
          <button type="button" class="btn-ghost">전체보기</button>
        </div>
        <div
          v-if="limitedRecentResults.length === 0"
          class="section-contents section-contents--empty"
        >
          <p class="empty-text">진행한 결과가 없어요</p>
        </div>
        <div v-else class="section-contents section-contents--list">
          <router-link
            v-for="item in limitedRecentResults"
            :key="`${item.type}-${item.id}-${item.createdAt}`"
            :to="
              item.type === 'vote'
                ? `/vote/${item.id}`
                : { name: 'rouletteShare', query: { data: item.shareData, fromHistory: 'true' } }
            "
            class="vote-list-item"
          >
            <div class="vote-list-main">
              <span
                class="result-label"
                :class="item.type === 'vote' ? 'result-label--vote' : 'result-label--roulette'"
              >
                {{ item.type === 'vote' ? '투표' : '룰렛' }}
              </span>
              <span class="vote-list-title">
                {{ item.type === 'vote' ? item.title : '오늘 점심은 여기로 결정!' }}
              </span>
            </div>
            <span class="vote-list-date">{{ formatVoteDate(item.createdAt) }}</span>
          </router-link>
        </div>
      </section>
    </main>

    <ProfileChangeModal
      :show="showProfileChangeModal"
      :profile-options="profileOptions"
      :current-value="currentProfileIndex"
      @close="closeProfileChangeModal"
      @confirm="onProfileConfirm"
    />
  </div>
</template>

<style scoped>
.mypage {
  display: flex;
  width: 100%;
  min-height: 100%;
  flex: 1;
  background-color: #ffffff;
}

.side-menu {
  flex-shrink: 0;
  width: 309px;
  padding: 100px 40px;
  box-sizing: border-box;
}

.side-menu-contents {
  display: flex;
  flex-direction: column;
  gap: 40px;
  width: 223px;
}

.profile {
  display: flex;
  gap: 20px;
  align-items: center;
}

.profile-thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 4px;
  overflow: hidden;
  background-color: #e8eaed;
}

.profile-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 18px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
  white-space: nowrap;
} 

.divider {
  width: 223px;
  height: 1px;
  background-color: #e8eaed;
  flex-shrink: 0;
}

.menu-1depth {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  width: 223px;
  box-sizing: border-box;
  border-radius: 999px;
}

.menu-1depth-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.menu-arrow {
  flex-shrink: 0;
  font-size: 14px;
  color: #5f6368;
}

.sub-menu {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-left: 30px;
  width: 100%;
  box-sizing: border-box;
}

.sub-menu-item {
  padding: 12px 20px;
  border-radius: 999px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #5f6368;
}

.sub-menu-item--selected {
  background-color: #ffe0d8;
  font-weight: 700;
  color: #ff5531;
}

.mypage-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 60px;
  padding: 80px 32px 96px 32px;
  box-sizing: border-box;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 800px;
  margin: 0;
}

.section--bottom-space {
  padding-bottom: 80px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.section-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 28px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.section-contents {
  width: 100%;
}

.section-contents--row {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.thumbnail-wrap {
  flex-shrink: 0;
}

.thumbnail {
  border-radius: 8px;
  overflow: hidden;
  background-color: #e8eaed;
  position: relative;
}

.thumbnail--large {
  width: 160px;
  height: 160px;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumbnail-edit-btn {
  position: absolute;
  right: 4px;
  bottom: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s;
}

.thumbnail-edit-icon {
  display: block;
  width: 32px;
  height: 32px;
  object-fit: contain;
  object-position: center;
}

.thumbnail-edit-btn:hover {
  background: rgba(0, 0, 0, 0.75);
}

.thumbnail-edit-btn:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.account {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.account-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.account-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  width: 100px;
  flex-shrink: 0;
}

.account-value {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 22px;
  line-height: 1.5;
  color: #3c4043;
}

.btn-outlined {
  height: 48px;
  padding: 12px 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.btn-outlined:hover {
  background-color: #f8f9fa;
  border-color: #bdc1c6;
}

.btn-outlined:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.btn-ghost {
  padding: 8px 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-ghost:hover {
  background-color: #f1f3f4;
}

.btn-ghost:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.section-contents--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.section-contents--list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.favorites-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  gap: 16px;
}

.favorite-item {
  list-style: none;
}

.favorite-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 24px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  box-sizing: border-box;
  width: 260px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.favorite-thumb-wrap {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  background-color: #e8eaed;
  position: relative;
}

.favorite-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.favorite-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.favorite-name {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.favorite-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.favorite-category {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 13px;
  color: #ff5531;
}

.favorite-external-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.favorite-external-btn:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.favorite-external-icon {
  width: 20px;
  height: 20px;
  display: block;
}

.favorite-heart-button {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  padding: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.35);
  transition:
    background 0.18s ease-out,
    transform 0.18s ease-out,
    box-shadow 0.18s ease-out;
}

.favorite-heart-button:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.45);
}

.favorite-heart-button:active {
  transform: translateY(0) scale(0.96);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
}

.favorite-heart-icon {
  width: 20px;
  height: 20px;
}

.favorite-heart-path {
  fill: #ff5531;
  stroke: #ff5531;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.vote-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #dadce0;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, background-color 0.2s;
}

.vote-list-item:hover {
  border-color: #ff5531;
  background-color: #fff5f3;
}

.vote-list-item--placeholder {
  border-style: dashed;
  border-color: #e8eaed;
  background-color: #f8f9fa;
  cursor: default;
}

.vote-list-item--placeholder .vote-list-title {
  color: #9aa0a6;
}

.vote-list-item--roulette {
  border-style: solid;
  border-color: #dadce0;
  background-color: #ffffff;
}

.vote-list-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.result-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 10px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 12px;
  line-height: 1.35;
  white-space: nowrap;
}

.result-label--vote {
  background-color: #fff0ea;
  border: 1px solid #ff5531;
  color: #ff5531;
}

.result-label--roulette {
  background-color: #f1f3f4;        /* var(--Color-Gray-100, #F1F3F4) */
  border: 1px solid #202124;        /* var(--Color-Gray-900, #202124) */
  color: #3c4043;
}

.vote-list-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #3c4043;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.vote-list-date {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 14px;
  color: #5f6368;
  flex-shrink: 0;
}

.empty-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 24px;
  line-height: 1.35;
  color: #dadce0;
  margin: 0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px 0;
}

.pagination-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #5f6368;
  cursor: pointer;
  transition: color 0.2s;
}

.pagination-arrow:hover {
  color: #3c4043;
}

.pagination-arrow:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.pagination-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  cursor: pointer;
  padding: 0;
  margin: 0;
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.pagination-page:not(.pagination-page--ellipsis):not(.pagination-page--current):hover {
  background-color: #f8f9fa;
  border-color: #bdc1c6;
}

.pagination-page--current {
  background-color: #3c4043;
  color: #ffffff;
  border-color: #3c4043;
}

.pagination-page--ellipsis {
  cursor: default;
}

@media (max-width: 1024px) {
  .mypage {
    flex-direction: column;
  }

  .side-menu {
    width: 100%;
    padding: 24px 20px;
    border-bottom: 1px solid #e8eaed;
  }

  .side-menu-contents {
    width: 100%;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 20px;
  }

  .profile {
    width: 100%;
  }

  .divider {
    display: none;
  }

  .menu-1depth,
  .sub-menu {
    padding-left: 0;
  }

  .mypage-main {
    padding: 32px 20px 96px;
  }
}
</style>
