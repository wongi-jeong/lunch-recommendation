<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import VoteResultCard from './VoteResultCard.vue'
import VoteOptionsList from './VoteOptionsList.vue'
import VoteShareModal from './VoteShareModal.vue'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'
import { getVoterIdentity, findExistingVote } from '@/utils/voterIdentity'

const VOTE_TTL_MS = 7 * 24 * 60 * 60 * 1000

const route = useRoute()
const router = useRouter()
const { getToken } = useAuth()
const voteId = computed(() => route.params.id)

const voteData = ref(null)
const selectedIndex = ref(null)
const hasVoted = ref(false)
const showError = ref(false)
const timerDisplay = ref({ hours: '00', minutes: '00', seconds: '00' })
const hasTimer = ref(false)
const voteEnded = ref(false)
const popoverState = ref('none')
const isLoading = ref(true)
let timerInterval = null
let storageHandler = null
let identity = null

const purgeExpiredVotes = () => {
  const keys = Object.keys(localStorage).filter(k => k.startsWith('vote_'))
  const now = Date.now()
  for (const key of keys) {
    try {
      const data = JSON.parse(localStorage.getItem(key))
      if (data?.ended && data?.endedAt && now - new Date(data.endedAt).getTime() > VOTE_TTL_MS) {
        localStorage.removeItem(key)
      }
    } catch { /* skip corrupt entries */ }
  }
}

const cleanupSession = () => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  if (storageHandler) {
    window.removeEventListener('storage', storageHandler)
    storageHandler = null
  }
}

const endVote = async () => {
  if (!voteData.value || voteEnded.value) return
  voteData.value.ended = true
  voteData.value.endedAt = new Date().toISOString()
  localStorage.setItem(`vote_${voteId.value}`, JSON.stringify(voteData.value))
  voteEnded.value = true
  cleanupSession()

  const token = getToken()
  if (token) {
    try {
      await fetch(`/api/votes/${voteId.value}/end`, {
        method: 'PATCH',
        headers: { 'X-Auth-Token': token }
      })
    } catch (e) {
      console.error('투표 종료 API 호출 실패:', e)
    }
  }
}

const loadVote = async () => {
  const stored = localStorage.getItem(`vote_${voteId.value}`)
  if (!stored) {
    voteData.value = null
    isLoading.value = false
    return
  }
  voteData.value = JSON.parse(stored)

  // API에서 생성자 정보 보강 (예전 투표·다른 기기에서 만든 투표도 '투표 종료하기' 노출)
  if (getToken()) {
    try {
      const res = await fetch(`/api/votes/${voteId.value}`)
      if (res.ok) {
        const data = await res.json()
        if (data.creatorMemberId != null) voteData.value.createdByMemberId = data.creatorMemberId
      }
    } catch (e) { /* 무시 */ }
  }

  if (voteData.value.ended) {
    voteEnded.value = true
    cleanupSession()
    isLoading.value = false
    return
  }

  if (!identity) {
    identity = await getVoterIdentity()
  }

  const existingVote = findExistingVote(
    voteData.value.voters,
    identity.voterId,
    identity.fingerprint
  )
  if (existingVote) {
    hasVoted.value = true
    selectedIndex.value = existingVote.optionIndex
  }
  setupTimer()
  isLoading.value = false
}

const setupTimer = () => {
  if (!voteData.value || voteEnded.value) return
  const { timer, createdAt } = voteData.value
  if (timer === 'none' || !timer) {
    hasTimer.value = false
    return
  }
  hasTimer.value = true
  const durationMs = { '10min': 600000, '30min': 1800000, '1hour': 3600000 }[timer] || 0
  if (!durationMs) { hasTimer.value = false; return }

  const endTime = new Date(createdAt).getTime() + durationMs
  const updateTimer = () => {
    const remaining = endTime - Date.now()
    if (remaining <= 0) {
      timerDisplay.value = { hours: '00', minutes: '00', seconds: '00' }
      endVote()
      return
    }
    const totalSec = Math.floor(remaining / 1000)
    timerDisplay.value = {
      hours: String(Math.floor(totalSec / 3600)).padStart(2, '0'),
      minutes: String(Math.floor((totalSec % 3600) / 60)).padStart(2, '0'),
      seconds: String(totalSec % 60).padStart(2, '0')
    }
  }
  updateTimer()
  timerInterval = setInterval(updateTimer, 1000)
}

onMounted(async () => {
  purgeExpiredVotes()
  identity = await getVoterIdentity()
  await loadVote()
  if (!voteEnded.value) {
    storageHandler = loadVote
    window.addEventListener('storage', storageHandler)
  }
})
onUnmounted(() => {
  cleanupSession()
})

const totalVoters = computed(() => (voteData.value?.voters || []).length)

const getPercentage = (index) => {
  if (!voteData.value || totalVoters.value === 0) return 0
  const count = (voteData.value.voters || []).filter(v => v.optionIndex === index).length
  return Math.round((count / totalVoters.value) * 100)
}

const winnerIndex = computed(() => {
  if (!voteData.value || totalVoters.value === 0) return 0
  let maxCount = 0, maxIdx = 0
  voteData.value.options.forEach((_, i) => {
    const count = (voteData.value.voters || []).filter(v => v.optionIndex === i).length
    if (count > maxCount) { maxCount = count; maxIdx = i }
  })
  return maxIdx
})

const winnerOption = computed(() => voteData.value?.options[winnerIndex.value] || null)

const canVote = computed(() => selectedIndex.value !== null && !hasVoted.value && !voteEnded.value)

const selectOption = (index) => {
  if (hasVoted.value || voteEnded.value) return
  showError.value = false
  selectedIndex.value = selectedIndex.value === index ? null : index
}

const submitVote = () => {
  if (hasVoted.value || voteEnded.value || !identity) return
  if (selectedIndex.value === null) { showError.value = true; return }
  if (!voteData.value.voters) voteData.value.voters = []

  const existing = findExistingVote(
    voteData.value.voters,
    identity.voterId,
    identity.fingerprint
  )
  if (existing) {
    const idx = voteData.value.voters.indexOf(existing)
    voteData.value.voters[idx] = {
      voterId: identity.voterId,
      fingerprint: identity.fingerprint,
      optionIndex: selectedIndex.value
    }
  } else {
    voteData.value.voters.push({
      voterId: identity.voterId,
      fingerprint: identity.fingerprint,
      optionIndex: selectedIndex.value
    })
  }
  localStorage.setItem(`vote_${voteId.value}`, JSON.stringify(voteData.value))
  hasVoted.value = true
  showError.value = false
}

const toggleSettings = () => {
  popoverState.value = popoverState.value === 'none' ? 'settings' : 'none'
}
const closePopover = () => { popoverState.value = 'none' }
const askEndVote = () => { popoverState.value = 'confirmEnd' }
const askDeleteVote = () => { popoverState.value = 'confirmDelete' }

const confirmEndVote = () => {
  endVote()
  popoverState.value = 'none'
}

const confirmDeleteVote = () => {
  localStorage.removeItem(`vote_${voteId.value}`)
  popoverState.value = 'none'
  router.push('/nearby')
}

const goHome = () => { router.push('/nearby') }

const showToast = ref(false)
let toastTimer = null

const triggerToast = () => {
  showToast.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { showToast.value = false }, 2000)
}

const googleMapsLink = computed(() => {
  if (!winnerOption.value?.googlePlaceId) return ''
  return `https://www.google.com/maps/place/?q=place_id:${winnerOption.value.googlePlaceId}`
})

const copyGoogleMapsLink = async () => {
  if (!googleMapsLink.value) return
  try {
    await navigator.clipboard.writeText(googleMapsLink.value)
    triggerToast()
  } catch { /* silent */ }
}

const showShareModal = ref(false)
const shareModalMode = ref('result')

const openShareModal = (mode = 'result') => {
  shareModalMode.value = mode
  showShareModal.value = true
}
const closeShareModal = () => { showShareModal.value = false }

const shareModalLink = computed(() => {
  return shareModalMode.value === 'location' ? googleMapsLink.value : shareLink.value
})

const shareModalTitle = computed(() => {
  if (shareModalMode.value === 'location') {
    return `${winnerOption.value?.name || '식당'} 위치 공유`
  }
  if (shareModalMode.value === 'vote') {
    return '투표 링크 공유'
  }
  return `${voteData.value?.title || '투표'} 결과 공유`
})

const shareText = computed(() => {
  if (shareModalMode.value === 'location') {
    return winnerOption.value ? `"${winnerOption.value.name}" 위치를 확인해보세요!` : '식당 위치를 확인해보세요!'
  }
  if (shareModalMode.value === 'vote') {
    return `"${voteData.value?.title || '점심 투표'}"에 참여해보세요!`
  }
  if (!winnerOption.value) return '투표 결과를 확인해보세요.'
  return `오늘 점심은 "${winnerOption.value.name}"! 투표 결과를 확인해보세요.`
})

const openExternalLink = (option) => {
  const placeId = option?.googlePlaceId
  if (!placeId) return
  const url = `https://www.google.com/maps/place/?q=place_id:${encodeURIComponent(placeId)}`
  window.open(url, '_blank', 'noopener,noreferrer')
}

const copyModalLink = async () => {
  try {
    await navigator.clipboard.writeText(shareModalLink.value)
    triggerToast()
  } catch { /* silent */ }
}

const shareToKakao = () => {
  const url = `https://sharer.kakao.com/talk/friends/picker/link?url=${encodeURIComponent(shareModalLink.value)}&text=${encodeURIComponent(shareText.value)}`
  window.open(url, '_blank', 'width=600,height=700')
}
const shareToFacebook = () => {
  window.open(`https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(shareModalLink.value)}`, '_blank', 'width=600,height=500')
}
const shareToX = () => {
  window.open(`https://x.com/intent/post?text=${encodeURIComponent(shareText.value)}&url=${encodeURIComponent(shareModalLink.value)}`, '_blank', 'width=600,height=500')
}
const shareToBand = () => {
  window.open(`https://band.us/plugin/share?body=${encodeURIComponent(shareText.value + '\n' + shareModalLink.value)}`, '_blank', 'width=600,height=500')
}
const shareToEmail = () => {
  const subject = shareModalTitle.value
  window.location.href = `mailto:?subject=${encodeURIComponent(subject)}&body=${encodeURIComponent(shareText.value + '\n\n' + shareModalLink.value)}`
}

const getBusinessStatus = (option) => {
  if (option?.openNow === true) return '영업중'
  if (option?.openNow === false) return '영업 종료'
  return '정보 없음'
}

const getPhotoUrl = (option) => {
  if (option?.photoName && Array.isArray(option.photoName) && option.photoName.length > 0) {
    return `/api/restaurants/photo?name=${encodeURIComponent(option.photoName[0])}`
  }
  return defaultThumbnail
}
const handleImageError = (event) => { event.target.src = defaultThumbnail }

const shareLink = computed(() => `${window.location.origin}/vote/${voteId.value}`)
const copyShareLink = async () => {
  try { await navigator.clipboard.writeText(shareLink.value) } catch { /* silent */ }
}

/** 로그인한 투표 생성자만 true. 종료/삭제는 생성자만 가능 */
const isCreator = computed(() => {
  const token = getToken()
  const creatorId = voteData.value?.createdByMemberId
  if (!token || creatorId == null) return false
  return String(creatorId) === String(token)
})
</script>

<template>
  <div class="vote-page" @click="popoverState !== 'none' ? closePopover() : null">
    <div v-if="isLoading" class="vote-loading">
      <div class="vote-loading-spinner"></div>
    </div>
    <template v-else-if="voteData">
      <!-- 결과 카드: 투표 종료 후 1등 식당 -->
      <VoteResultCard
        v-if="voteEnded && winnerOption"
        :winner-option="winnerOption"
        :percentage="getPercentage(winnerIndex)"
        :total-voters="totalVoters"
        :get-photo-url="getPhotoUrl"
        :handle-image-error="handleImageError"
        :get-business-status="getBusinessStatus"
        @share="openShareModal('result')"
      />

      <!-- 투표 폼 -->
      <div class="vote-form">
        <!-- 타이틀 영역 -->
        <div class="vote-title-section">
          <div class="vote-title-timer">
            <h2 class="vote-title">{{ voteData.title }} {{ voteEnded ? '종료' : '진행 중' }}</h2>
            <div v-if="hasTimer && !voteEnded" class="timer-row">
              <span>종료까지</span>
              <span>{{ timerDisplay.hours }}시간</span>
              <span>{{ timerDisplay.minutes }}분</span>
              <span>{{ timerDisplay.seconds }}초</span>
              <span>남음</span>
            </div>
            <div v-if="voteEnded" class="timer-row timer-ended">
              <span>투표가 종료되었습니다</span>
            </div>
          </div>
          <!-- 톱니바퀴(설정): 생성자만 표시, 종료/삭제 메뉴 -->
          <div v-if="isCreator" class="settings-area" @click.stop>
            <button class="settings-btn" :class="{ 'settings-btn-disabled': voteEnded }" :disabled="voteEnded" @click="toggleSettings" aria-label="설정">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 20a4 4 0 100-8 4 4 0 000 8z" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M25.9 20a2.18 2.18 0 00.44 2.4l.08.08a2.64 2.64 0 11-3.74 3.74l-.08-.08a2.18 2.18 0 00-2.4-.44 2.18 2.18 0 00-1.32 2v.26a2.64 2.64 0 11-5.28 0v-.14A2.18 2.18 0 0012.28 26a2.18 2.18 0 00-2.4.44l-.08.08a2.64 2.64 0 11-3.74-3.74l.08-.08a2.18 2.18 0 00.44-2.4A2.18 2.18 0 004.58 19h-.26a2.64 2.64 0 010-5.28h.14A2.18 2.18 0 006 12.28a2.18 2.18 0 00-.44-2.4l-.08-.08a2.64 2.64 0 113.74-3.74l.08.08a2.18 2.18 0 002.4.44h.1a2.18 2.18 0 001.32-2V4.32a2.64 2.64 0 015.28 0v.14a2.18 2.18 0 001.32 2 2.18 2.18 0 002.4-.44l.08-.08a2.64 2.64 0 113.74 3.74l-.08.08a2.18 2.18 0 00-.44 2.4v.1a2.18 2.18 0 002 1.32h.26a2.64 2.64 0 010 5.28h-.14a2.18 2.18 0 00-2 1.32z" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <!-- 팝오버: 종료 확인 / 삭제 메뉴·확인 -->
            <Transition name="popover-fade">
              <div v-if="popoverState !== 'none'" class="popover-card" @click.stop>
                <template v-if="popoverState === 'settings'">
                  <div class="popover-menu">
                    <button v-if="!voteEnded" type="button" class="popover-menu-item" @click="askEndVote">투표 종료하기</button>
                    <button type="button" class="popover-menu-item popover-menu-item-danger" @click="askDeleteVote">투표 삭제하기</button>
                  </div>
                </template>
                <template v-if="popoverState === 'confirmEnd'">
                  <p class="popover-title">투표를 종료하시겠습니까?</p>
                  <div class="popover-btns">
                    <button class="popover-btn popover-btn-outlined" @click="confirmEndVote">네</button>
                    <button class="popover-btn popover-btn-ghost" @click="closePopover">아니요</button>
                  </div>
                </template>
                <template v-if="popoverState === 'confirmDelete'">
                  <p class="popover-title">투표를 삭제하시겠습니까?</p>
                  <div class="popover-btns">
                    <button class="popover-btn popover-btn-outlined" @click="confirmDeleteVote">네</button>
                    <button class="popover-btn popover-btn-ghost" @click="closePopover">아니요</button>
                  </div>
                </template>
              </div>
            </Transition>
          </div>
        </div>

        <!-- 서브타이틀 + 투표 목록 -->
        <VoteOptionsList
          :options="voteData.options"
          :selected-index="selectedIndex"
          :has-voted="hasVoted"
          :vote-ended="voteEnded"
          :winner-index="winnerIndex"
          :get-percentage="getPercentage"
          :get-photo-url="getPhotoUrl"
          :handle-image-error="handleImageError"
          :get-business-status="getBusinessStatus"
          :open-external-link="openExternalLink"
          :show-error="showError"
          @select="selectOption"
        />

        <!-- 버튼 영역 -->
        <div class="vote-action-section">
          <div v-if="!voteEnded" class="vote-action-row">
            <button
              v-if="!hasVoted"
              class="vote-submit-btn"
              :class="{ 'vote-submit-enabled': canVote }"
              @click="submitVote"
            >투표하기</button>
            <button class="vote-share-outline-btn" @click="openShareModal('vote')">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><mask id="link_btn_mask" style="mask-type:alpha" maskUnits="userSpaceOnUse" x="0" y="0" width="20" height="20"><rect width="20" height="20" fill="#D9D9D9"/></mask><g mask="url(#link_btn_mask)"><path d="M9.16667 14.1667H5.83333C4.68056 14.1667 3.69792 13.7604 2.88542 12.9479C2.07292 12.1354 1.66667 11.1528 1.66667 10C1.66667 8.84722 2.07292 7.86458 2.88542 7.05208C3.69792 6.23958 4.68056 5.83333 5.83333 5.83333H9.16667V7.5H5.83333C5.13889 7.5 4.54861 7.74306 4.0625 8.22917C3.57639 8.71528 3.33333 9.30556 3.33333 10C3.33333 10.6944 3.57639 11.2847 4.0625 11.7708C4.54861 12.2569 5.13889 12.5 5.83333 12.5H9.16667V14.1667ZM6.66667 10.8333V9.16667H13.3333V10.8333H6.66667ZM10.8333 14.1667V12.5H14.1667C14.8611 12.5 15.4514 12.2569 15.9375 11.7708C16.4236 11.2847 16.6667 10.6944 16.6667 10C16.6667 9.30556 16.4236 8.71528 15.9375 8.22917C15.4514 7.74306 14.8611 7.5 14.1667 7.5H10.8333V5.83333H14.1667C15.3194 5.83333 16.3021 6.23958 17.1146 7.05208C17.9271 7.86458 18.3333 8.84722 18.3333 10C18.3333 11.1528 17.9271 12.1354 17.1146 12.9479C16.3021 13.7604 15.3194 14.1667 14.1667 14.1667H10.8333Z" fill="#5f6368"/></g></svg>
              링크 공유
            </button>
          </div>

          <div v-if="voteEnded" class="vote-ended-actions">
            <button v-if="googleMapsLink" class="vote-ended-btn vote-ended-btn-outlined" @click="openShareModal('location')">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><mask id="loc_btn_mask" style="mask-type:alpha" maskUnits="userSpaceOnUse" x="0" y="0" width="20" height="20"><rect width="20" height="20" fill="#D9D9D9"/></mask><g mask="url(#loc_btn_mask)"><path d="M10 10C10.4583 10 10.8507 9.83681 11.1771 9.51042C11.5035 9.18403 11.6667 8.79167 11.6667 8.33333C11.6667 7.875 11.5035 7.48264 11.1771 7.15625C10.8507 6.82986 10.4583 6.66667 10 6.66667C9.54167 6.66667 9.14931 6.82986 8.82292 7.15625C8.49653 7.48264 8.33333 7.875 8.33333 8.33333C8.33333 8.79167 8.49653 9.18403 8.82292 9.51042C9.14931 9.83681 9.54167 10 10 10ZM10 18.3333C7.76389 16.4306 6.09375 14.6632 4.98958 13.0312C3.88542 11.3993 3.33333 9.88889 3.33333 8.5C3.33333 6.41667 4.00347 4.75694 5.34375 3.52083C6.68403 2.28472 8.23611 1.66667 10 1.66667C11.7639 1.66667 13.316 2.28472 14.6563 3.52083C15.9965 4.75694 16.6667 6.41667 16.6667 8.5C16.6667 9.88889 16.1146 11.3993 15.0104 13.0312C13.9062 14.6632 12.2361 16.4306 10 18.3333Z" fill="#ff5531"/></g></svg>
              위치 공유
            </button>
            <button class="vote-ended-btn vote-ended-btn-primary" @click="openShareModal('result')">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M13.333 6.667a2.667 2.667 0 100-5.334 2.667 2.667 0 000 5.334zM6.667 12.667a2.667 2.667 0 100-5.334 2.667 2.667 0 000 5.334zM13.333 18.667a2.667 2.667 0 100-5.334 2.667 2.667 0 000 5.334zM8.94 11.273l2.127 1.787M11.06 6.94L8.94 8.727" stroke="white" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
              결과 공유
            </button>
          </div>
        </div>
      </div>
    </template>

    <div v-else class="vote-not-found">
      <p>투표를 찾을 수 없습니다.</p>
    </div>

    <!-- 공유 모달 -->
    <VoteShareModal
      :visible="showShareModal"
      :title="shareModalTitle"
      :link="shareModalLink"
      @close="closeShareModal"
      @share-kakao="shareToKakao"
      @share-facebook="shareToFacebook"
      @share-x="shareToX"
      @share-band="shareToBand"
      @share-email="shareToEmail"
      @copy-link="copyModalLink"
    />

    <Transition name="toast-fade">
      <div v-if="showToast" class="copy-toast">링크가 클립보드에 복사되었습니다</div>
    </Transition>
  </div>
</template>

<style src="./VotePage.css"></style>
