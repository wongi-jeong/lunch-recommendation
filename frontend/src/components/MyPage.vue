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

function formatVoteDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleDateString('ko-KR', { year: 'numeric', month: 'short', day: 'numeric' })
}

watch(() => route.path, (path) => {
  if (path === '/my') {
    applyProfileIndex(authProfileIndex.value)
    fetchMe()
    fetchOngoingVotes()
    fetchEndedVotes()
  }
}, { immediate: true })

const handlePageshow = () => {
  if (route.path === '/my') {
    fetchMe()
    fetchOngoingVotes()
    fetchEndedVotes()
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
        <div v-else class="section-contents section-contents--list">
          <router-link
            v-for="v in ongoingVotes"
            :key="v.id"
            :to="`/vote/${v.id}`"
            class="vote-list-item"
          >
            <span class="vote-list-title">{{ v.title }}</span>
            <span class="vote-list-date">{{ formatVoteDate(v.createdAt) }}</span>
          </router-link>
        </div>
        <div v-if="ongoingVotes.length > 0" class="pagination">
          <button type="button" class="pagination-arrow" aria-label="이전">‹</button>
          <div class="pagination-page pagination-page--current">1</div>
          <button type="button" class="pagination-arrow" aria-label="다음">›</button>
        </div>
      </section>

      <section class="section" aria-labelledby="section-fav-title">
        <div class="section-head">
          <h2 id="section-fav-title" class="section-title">최근 즐겨찾기 한 장소</h2>
          <button type="button" class="btn-ghost">전체보기</button>
        </div>
        <div class="section-contents section-contents--empty">
          <p class="empty-text">즐겨찾기한 장소가 없어요</p>
        </div>
      </section>

      <section class="section" aria-labelledby="section-result-title">
        <div class="section-head">
          <h2 id="section-result-title" class="section-title">최근 진행한 결과</h2>
          <button type="button" class="btn-ghost">전체보기</button>
        </div>
        <div v-if="endedVotes.length === 0" class="section-contents section-contents--empty">
          <p class="empty-text">진행한 결과가 없어요</p>
        </div>
        <div v-else class="section-contents section-contents--list">
          <router-link
            v-for="v in endedVotes"
            :key="v.id"
            :to="`/vote/${v.id}`"
            class="vote-list-item"
          >
            <span class="vote-list-title">{{ v.title }}</span>
            <span class="vote-list-date">{{ formatVoteDate(v.endedAt || v.createdAt) }}</span>
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
  gap: 70px;
  padding: 80px 32px 48px 32px;
  box-sizing: border-box;
}

.section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 800px;
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
  gap: 50px;
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
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
}

.pagination-page--current {
  background-color: #3c4043;
  color: #ffffff;
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
    padding: 32px 20px 48px;
  }
}
</style>
