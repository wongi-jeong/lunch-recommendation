<script setup>
import { ref, computed, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'
import { getVoterIdentity, findExistingVote } from '@/utils/voterIdentity'

const VOTE_TTL_MS = 7 * 24 * 60 * 60 * 1000

const route = useRoute()
const router = useRouter()
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

const endVote = () => {
  if (!voteData.value || voteEnded.value) return
  voteData.value.ended = true
  voteData.value.endedAt = new Date().toISOString()
  localStorage.setItem(`vote_${voteId.value}`, JSON.stringify(voteData.value))
  voteEnded.value = true
  cleanupSession()
}

const loadVote = async () => {
  const stored = localStorage.getItem(`vote_${voteId.value}`)
  if (!stored) {
    voteData.value = null
    isLoading.value = false
    return
  }
  voteData.value = JSON.parse(stored)
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
</script>

<template>
  <div class="vote-page" @click="popoverState !== 'none' ? closePopover() : null">
    <div v-if="isLoading" class="vote-loading">
      <div class="vote-loading-spinner"></div>
    </div>
    <template v-else-if="voteData">
      <!-- 결과 카드: 투표 종료 후 1등 식당 -->
      <div v-if="voteEnded && winnerOption" class="result-card">
        <div class="result-card-header">
          <span class="result-badge">TOP PICK</span>
          <div class="result-card-title">{{ getPercentage(winnerIndex) }}%의 선택, 오늘의 점심</div>
          <div class="result-card-subtitle">{{ totalVoters }}명이 투표에 참여했어요</div>
        </div>
        <div class="result-card-content">
          <div class="result-card-img">
            <img :src="getPhotoUrl(winnerOption)" :alt="winnerOption.name" class="result-card-photo" @error="handleImageError" />
          </div>
          <div class="result-card-info">
            <div class="result-store-header">
              <span class="result-store-name">{{ winnerOption.name }}</span>
              <span class="result-store-category">{{ (winnerOption.categories || [])[0] || '식당' }}</span>
            </div>
            <div class="result-store-details">
              <div class="result-detail-row">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><mask id="loc_mask" style="mask-type:alpha" maskUnits="userSpaceOnUse" x="0" y="0" width="20" height="20"><rect width="20" height="20" fill="#D9D9D9"/></mask><g mask="url(#loc_mask)"><path d="M10 10C10.4583 10 10.8507 9.83681 11.1771 9.51042C11.5035 9.18403 11.6667 8.79167 11.6667 8.33333C11.6667 7.875 11.5035 7.48264 11.1771 7.15625C10.8507 6.82986 10.4583 6.66667 10 6.66667C9.54167 6.66667 9.14931 6.82986 8.82292 7.15625C8.49653 7.48264 8.33333 7.875 8.33333 8.33333C8.33333 8.79167 8.49653 9.18403 8.82292 9.51042C9.14931 9.83681 9.54167 10 10 10ZM10 18.3333C7.76389 16.4306 6.09375 14.6632 4.98958 13.0312C3.88542 11.3993 3.33333 9.88889 3.33333 8.5C3.33333 6.41667 4.00347 4.75694 5.34375 3.52083C6.68403 2.28472 8.23611 1.66667 10 1.66667C11.7639 1.66667 13.316 2.28472 14.6563 3.52083C15.9965 4.75694 16.6667 6.41667 16.6667 8.5C16.6667 9.88889 16.1146 11.3993 15.0104 13.0312C13.9062 14.6632 12.2361 16.4306 10 18.3333Z" fill="#5F6368"/></g></svg>
                <span>거리: {{ winnerOption.distance || '정보 없음' }}</span>
              </div>
              <div class="result-detail-row">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><mask id="star_mask" style="mask-type:alpha" maskUnits="userSpaceOnUse" x="0" y="0" width="20" height="20"><rect width="20" height="20" fill="#D9D9D9"/></mask><g mask="url(#star_mask)"><path d="M4.85417 17.5L6.20833 11.6458L1.66667 7.70833L7.66667 7.1875L10 1.66667L12.3333 7.1875L18.3333 7.70833L13.7917 11.6458L15.1458 17.5L10 14.3958L4.85417 17.5Z" fill="#5F6368"/></g></svg>
                <span>평점: {{ winnerOption.rating || '정보 없음' }}</span>
              </div>
              <div class="result-detail-row">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><mask id="schedule_mask" style="mask-type:alpha" maskUnits="userSpaceOnUse" x="0" y="0" width="20" height="20"><rect width="20" height="20" fill="#D9D9D9"/></mask><g mask="url(#schedule_mask)"><path d="M12.75 13.9167L13.9167 12.75L10.8333 9.66667V5.83333H9.16667V10.3333L12.75 13.9167ZM10 18.3333C8.84722 18.3333 7.76389 18.1146 6.75 17.6771C5.73611 17.2396 4.85417 16.6458 4.10417 15.8958C3.35417 15.1458 2.76042 14.2639 2.32292 13.25C1.88542 12.2361 1.66667 11.1528 1.66667 10C1.66667 8.84722 1.88542 7.76389 2.32292 6.75C2.76042 5.73611 3.35417 4.85417 4.10417 4.10417C4.85417 3.35417 5.73611 2.76042 6.75 2.32292C7.76389 1.88542 8.84722 1.66667 10 1.66667C11.1528 1.66667 12.2361 1.88542 13.25 2.32292C14.2639 2.76042 15.1458 3.35417 15.8958 4.10417C16.6458 4.85417 17.2396 5.73611 17.6771 6.75C18.1146 7.76389 18.3333 8.84722 18.3333 10C18.3333 11.1528 18.1146 12.2361 17.6771 13.25C17.2396 14.2639 16.6458 15.1458 15.8958 15.8958C15.1458 16.6458 14.2639 17.2396 13.25 17.6771C12.2361 18.1146 11.1528 18.3333 10 18.3333Z" fill="#5F6368"/></g></svg>
                <span>영업여부: <span :class="{ 'status-open': winnerOption.openNow === true, 'status-closed': winnerOption.openNow === false }">{{ getBusinessStatus(winnerOption) }}</span></span>
              </div>
            </div>
          </div>
        </div>
      </div>

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
          <!-- 톱니바퀴 아이콘 -->
          <div class="settings-area" @click.stop>
            <button class="settings-btn" :class="{ 'settings-btn-disabled': voteEnded }" :disabled="voteEnded" @click="toggleSettings" aria-label="설정">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M16 20a4 4 0 100-8 4 4 0 000 8z" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                <path d="M25.9 20a2.18 2.18 0 00.44 2.4l.08.08a2.64 2.64 0 11-3.74 3.74l-.08-.08a2.18 2.18 0 00-2.4-.44 2.18 2.18 0 00-1.32 2v.26a2.64 2.64 0 11-5.28 0v-.14A2.18 2.18 0 0012.28 26a2.18 2.18 0 00-2.4.44l-.08.08a2.64 2.64 0 11-3.74-3.74l.08-.08a2.18 2.18 0 00.44-2.4A2.18 2.18 0 004.58 19h-.26a2.64 2.64 0 010-5.28h.14A2.18 2.18 0 006 12.28a2.18 2.18 0 00-.44-2.4l-.08-.08a2.64 2.64 0 113.74-3.74l.08.08a2.18 2.18 0 002.4.44h.1a2.18 2.18 0 001.32-2V4.32a2.64 2.64 0 015.28 0v.14a2.18 2.18 0 001.32 2 2.18 2.18 0 002.4-.44l.08-.08a2.64 2.64 0 113.74 3.74l-.08.08a2.18 2.18 0 00-.44 2.4v.1a2.18 2.18 0 002 1.32h.26a2.64 2.64 0 010 5.28h-.14a2.18 2.18 0 00-2 1.32z" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
            <!-- 팝오버 -->
            <Transition name="popover-fade">
              <div v-if="popoverState !== 'none'" class="popover-card" @click.stop>
                <!-- 사용자 설정 -->
                <template v-if="popoverState === 'settings'">
                  <p class="popover-title">사용자 설정</p>
                  <div class="popover-btns">
                    <button class="popover-btn popover-btn-outlined" @click="askEndVote">투표 종료하기</button>
                    <button class="popover-btn popover-btn-ghost" @click="askDeleteVote">투표 삭제하기</button>
                  </div>
                </template>
                <!-- 투표 종료 확인 -->
                <template v-if="popoverState === 'confirmEnd'">
                  <p class="popover-title">투표를 종료하시겠습니까?</p>
                  <div class="popover-btns">
                    <button class="popover-btn popover-btn-outlined" @click="confirmEndVote">네</button>
                    <button class="popover-btn popover-btn-ghost" @click="closePopover">아니요</button>
                  </div>
                </template>
                <!-- 투표 삭제 확인 -->
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
        <div class="vote-list-section">
          <p class="vote-subtitle">과연 오늘의 점심은...?!</p>
          <div class="vote-list">
            <div
              v-for="(option, index) in voteData.options"
              :key="index"
              class="vote-item"
              :class="{
                'vote-item-selected': selectedIndex === index && !voteEnded,
                'vote-item-winner': voteEnded && index === winnerIndex,
                'vote-item-result': (hasVoted || voteEnded) && selectedIndex === index && !voteEnded,
                'vote-item-disabled': voteEnded
              }"
              @click="selectOption(index)"
            >
              <div class="vote-item-img">
                <img :src="getPhotoUrl(option)" :alt="option.name" class="vote-item-photo" @error="handleImageError" />
              </div>
              <div class="vote-item-body">
                <div class="vote-item-text">
                  <span class="vote-item-number">{{ index + 1 }}</span>
                  <span class="vote-item-name">{{ option.name }}</span>
                  <span class="vote-item-separator">|</span>
                  <span class="vote-item-category">
                    {{ (option.categories || [])[0] || '식당' }}
                  </span>
                  <span class="vote-item-dot">·</span>
                  <span class="vote-item-rating">
                    <img src="@/assets/star-icon.svg" alt="별점" class="vote-item-star-icon" />
                    {{ option.rating != null ? option.rating.toFixed(1) : '0.0' }}
                  </span>
                  <span class="vote-item-dot">·</span>
                  <span
                    class="vote-item-status"
                    :class="{
                      'status-open': option.openNow === true,
                      'status-closed': option.openNow === false
                    }"
                  >
                    {{ getBusinessStatus(option) }}
                  </span>
                  <span class="vote-item-pct">{{ getPercentage(index) }}%</span>
                </div>
                <div class="vote-progress-bar" :class="{ 'progress-selected': selectedIndex === index || (voteEnded && index === winnerIndex) }">
                  <div class="vote-progress-fill" :class="{ 'fill-selected': selectedIndex === index || (voteEnded && index === winnerIndex) }" :style="{ width: getPercentage(index) + '%' }"></div>
                </div>
              </div>
              <div class="vote-item-check">
                <template v-if="!hasVoted && !voteEnded">
                  <svg v-if="selectedIndex === index" width="32" height="32" viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531"/><path d="M10 16l4 4 8-8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                  <svg v-else width="32" height="32" viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="15" stroke="#BDC1C6" stroke-width="2"/></svg>
                </template>
                <template v-else-if="hasVoted && selectedIndex === index && !voteEnded">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531"/><path d="M10 16l4 4 8-8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </template>
                <template v-else-if="voteEnded && index === winnerIndex">
                  <svg width="32" height="32" viewBox="0 0 32 32" fill="none"><circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531"/><path d="M10 16l4 4 8-8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/></svg>
                </template>
              </div>
            </div>
          </div>
          <p v-if="showError" class="vote-error-text">항목을 선택해주세요</p>
        </div>

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
    <Transition name="share-modal-fade">
      <div v-if="showShareModal" class="share-overlay" @click.self="closeShareModal">
        <div class="share-modal">
          <div class="share-modal-header">
            <span class="share-modal-title">{{ shareModalTitle }}</span>
            <button class="share-modal-close" @click="closeShareModal" aria-label="닫기">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M18 6L6 18M6 6l12 12" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/></svg>
            </button>
          </div>
          <div class="share-platforms">
            <button class="share-platform-btn" @click="shareToKakao">
              <div class="share-platform-icon" style="background-color: #FEE500;">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M12 3C6.477 3 2 6.463 2 10.691c0 2.724 1.8 5.113 4.508 6.46-.2.743-.722 2.694-.828 3.112-.13.518.19.511.399.372.164-.109 2.612-1.776 3.67-2.497.733.104 1.488.162 2.251.162 5.523 0 10-3.463 10-7.691S17.523 3 12 3z" fill="#3C1E1E"/></svg>
              </div>
              <span>카카오톡</span>
            </button>
            <button class="share-platform-btn" @click="shareToFacebook">
              <div class="share-platform-icon" style="background-color: #1877F2;">
                <svg width="24" height="24" viewBox="0 0 24 24" fill="none"><path d="M24 12c0-6.627-5.373-12-12-12S0 5.373 0 12c0 5.99 4.388 10.954 10.125 11.854V15.47H7.078V12h3.047V9.356c0-3.007 1.791-4.668 4.533-4.668 1.312 0 2.686.235 2.686.235v2.953H15.83c-1.491 0-1.956.925-1.956 1.875V12h3.328l-.532 3.469h-2.796v8.385C19.612 22.954 24 17.99 24 12z" fill="white"/></svg>
              </div>
              <span>Facebook</span>
            </button>
            <button class="share-platform-btn" @click="shareToX">
              <div class="share-platform-icon" style="background-color: #000;">
                <svg width="18" height="18" viewBox="0 0 24 24" fill="none"><path d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z" fill="white"/></svg>
              </div>
              <span>X</span>
            </button>
            <button class="share-platform-btn" @click="shareToBand">
              <div class="share-platform-icon" style="background-color: #06CF5E;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z" fill="white"/></svg>
              </div>
              <span>밴드</span>
            </button>
            <button class="share-platform-btn" @click="shareToEmail">
              <div class="share-platform-icon" style="background-color: #9AA0A6;">
                <svg width="20" height="20" viewBox="0 0 24 24" fill="none"><path d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1.9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z" fill="white"/></svg>
              </div>
              <span>이메일</span>
            </button>
          </div>
          <div class="share-link-box">
            <span class="share-link-box-text">{{ shareModalLink }}</span>
            <button class="share-link-copy-btn" @click="copyModalLink">복사</button>
          </div>
        </div>
      </div>
    </Transition>

    <Transition name="toast-fade">
      <div v-if="showToast" class="copy-toast">링크가 클립보드에 복사되었습니다</div>
    </Transition>
  </div>
</template>

<style scoped>
.vote-page {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  background-color: #eff2f5;
  padding: 80px 0 120px;
  overflow-y: auto;
  gap: 40px;
}

/* ===== 결과 카드 ===== */
.result-card {
  background-color: #fff;
  border-radius: 64px;
  padding: 40px 40px 48px;
  width: 894px;
  max-width: 100%;
  flex-shrink: 0;
  overflow: hidden;
  box-sizing: border-box;
}

.result-card-header {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 0 8px 28px;
}

.result-badge {
  display: inline-flex;
  align-items: center;
  align-self: flex-start;
  padding: 4px 14px;
  background: #FF5531;
  color: #fff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 800;
  font-size: 13px;
  letter-spacing: 1.5px;
  border-radius: 20px;
  line-height: 1.4;
}

.result-card-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 800;
  font-size: 28px;
  line-height: 1.35;
  color: #202124;
  margin-top: 4px;
}

.result-card-subtitle {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.4;
  color: #80868b;
}

.result-card-content {
  display: flex;
  gap: 32px;
  align-items: stretch;
}

.result-card-img {
  width: 276px;
  height: 276px;
  background-color: #dadce0;
  border-radius: 32px;
  flex-shrink: 0;
  overflow: hidden;
}

.result-card-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.result-card-info {
  flex: 1;
  display: flex;
  flex-direction: column;
  justify-content: center;
  gap: 24px;
  min-width: 0;
}

.result-store-header {
  display: flex;
  align-items: center;
  gap: 16px;
}

.result-store-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 32px;
  line-height: 1.3;
  color: #202124;
}

.result-store-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
}

.result-store-details {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.result-detail-row {
  display: flex;
  align-items: center;
  gap: 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.5;
  color: #5f6368;
}

.result-detail-row svg {
  flex-shrink: 0;
  width: 24px;
  height: 24px;
}

.result-link-section {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.result-link-label {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #3c4043;
}

/* ===== 투표 폼 ===== */
.vote-form {
  background-color: #fff;
  border-radius: 72px;
  padding: 64px 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 894px;
  max-width: 100%;
  flex-shrink: 0;
  position: relative;
  box-sizing: border-box;
}

.vote-title-section {
  width: 100%;
  padding: 0 80px 40px;
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  box-sizing: border-box;
}

.vote-title-timer {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vote-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #31373c;
  margin: 0;
}

.timer-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #909faa;
  white-space: nowrap;
}

.timer-ended {
  color: #ff4400;
}

/* ===== 톱니바퀴 + 팝오버 ===== */
.settings-area {
  position: relative;
  flex-shrink: 0;
}

.settings-btn {
  width: 56px;
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.settings-btn:hover:not(:disabled) {
  background-color: rgba(0, 0, 0, 0.05);
}

.settings-btn-disabled {
  opacity: 0.35;
  cursor: default;
  pointer-events: none;
}

.popover-card {
  position: absolute;
  top: 64px;
  right: 0;
  width: 300px;
  background-color: #fff;
  border-radius: 32px;
  padding: 24px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  z-index: 100;
  display: flex;
  flex-direction: column;
  align-items: stretch;
}

.popover-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #000;
  text-align: center;
  margin: 0;
  padding-bottom: 16px;
}

.popover-btns {
  display: flex;
  gap: 8px;
}

.popover-btn {
  flex: 1;
  height: 40px;
  border-radius: 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 14px;
  line-height: 1.35;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  white-space: nowrap;
}

.popover-btn-outlined {
  background-color: #fff;
  border: 1px solid #dadce0;
  color: #3c4043;
}

.popover-btn-outlined:hover {
  background-color: #f5f5f5;
}

.popover-btn-ghost {
  background: transparent;
  border: none;
  color: #5f6368;
}

.popover-btn-ghost:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.popover-fade-enter-active,
.popover-fade-leave-active {
  transition: opacity 0.15s ease, transform 0.15s ease;
}
.popover-fade-enter-from,
.popover-fade-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* ===== 투표 목록 ===== */
.vote-list-section {
  width: 100%;
  padding: 0 80px 48px;
  display: flex;
  flex-direction: column;
  gap: 16px;
  box-sizing: border-box;
}

.vote-subtitle {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.vote-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.vote-item {
  display: flex;
  align-items: center;
  height: 100px;
  border: 1px solid #bdc1c6;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  background-color: #fff;
  transition: border-color 0.2s, background-color 0.2s;
}

.vote-item:hover {
  border-color: #9aa0a6;
}

.vote-item-selected {
  border-color: #ff5531;
  background-color: #fff0ea;
}

.vote-item-selected:hover {
  border-color: #ff5531;
}

.vote-item-winner {
  border-color: #ff8a6d;
  background-color: #fff0ea;
}

.vote-item-winner:hover {
  border-color: #ff8a6d;
}

.vote-item-disabled {
  cursor: default;
}

.vote-item-img {
  width: 80px;
  height: 80px;
  background-color: #dadce0;
  border-radius: 12px;
  margin-left: 15px;
  flex-shrink: 0;
  overflow: hidden;
}

.vote-item-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.vote-item-body {
  flex: 1;
  min-width: 0;
  margin-left: 24px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.vote-item-text {
  display: flex;
  align-items: center;
  gap: 8px;
  white-space: nowrap;
}

.vote-item-number,
.vote-item-name,
.vote-item-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
}

.vote-item-separator {
  font-size: 14px;
  color: #9aa0a6;
}

.vote-item-dot {
  font-size: 14px;
  color: #9aa0a6;
}

.vote-item-name {
  overflow: hidden;
  text-overflow: ellipsis;
}

.vote-item-rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #5f6368;
}

.vote-item-status {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
}

.vote-item-pct {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #5f6368;
}

.vote-item-star-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.vote-item-selected .vote-item-number,
.vote-item-selected .vote-item-name,
.vote-item-selected .vote-item-category,
.vote-item-selected .vote-item-pct,
.vote-item-winner .vote-item-number,
.vote-item-winner .vote-item-name,
.vote-item-winner .vote-item-category,
.vote-item-winner .vote-item-pct {
  color: #202124;
}

.vote-progress-bar {
  width: 470px;
  max-width: 100%;
  height: 6px;
  background-color: #e8eaed;
  border-radius: 3px;
  overflow: hidden;
}

.vote-progress-bar.progress-selected {
  background-color: #ffe0d8;
}

.vote-progress-fill {
  height: 100%;
  background-color: #bdc1c6;
  border-radius: 3px;
  transition: width 0.4s ease;
}

.vote-progress-fill.fill-selected {
  background-color: #ff5531;
}

.vote-item-check {
  margin-right: 31px;
  margin-left: auto;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
}

.vote-error-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #ff4400;
  margin: 0;
  padding: 0 4px;
}

/* ===== 버튼 영역 ===== */
.vote-action-section {
  width: 100%;
  padding: 0 80px;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  box-sizing: border-box;
}

.vote-action-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.vote-submit-btn {
  flex: 1;
  height: 64px;
  border: none;
  border-radius: 20px;
  background-color: #e8eaed;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #bdc1c6;
  cursor: not-allowed;
  transition: background-color 0.2s, color 0.2s;
}

.vote-submit-btn.vote-submit-enabled {
  background-color: #ff5531;
  color: #fff;
  cursor: pointer;
}

.vote-submit-btn.vote-submit-enabled:hover {
  background-color: #e6442a;
}

.vote-share-outline-btn {
  height: 64px;
  padding: 0 28px;
  border-radius: 20px;
  background-color: #fff;
  border: 1.5px solid #dadce0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}

.vote-share-outline-btn:hover {
  border-color: #ff5531;
  color: #ff5531;
  background-color: #fff5f3;
}

.vote-share-outline-btn:hover svg path {
  fill: #ff5531;
}

/* ===== 공유 링크 ===== */
.vote-share-field {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 56px;
  border: 1px solid #dadce0;
  border-radius: 16px;
  padding: 4px 12px;
  background-color: #fff;
  overflow: hidden;
  box-sizing: border-box;
  width: 100%;
}

.share-link-icon {
  flex-shrink: 0;
}

.share-link-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.35;
  color: #797f86;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.result-share-field {
  width: 100%;
}

.share-copy-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 12px;
  border: 1px solid #dadce0;
  border-radius: 12px;
  background-color: #fff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 14px;
  line-height: 1.35;
  color: #3c4043;
  cursor: pointer;
  transition: background-color 0.2s;
  white-space: nowrap;
}

.share-copy-btn:hover {
  background-color: #f5f5f5;
}

/* ===== 클릭 가능한 링크 박스 ===== */
.result-share-clickable {
  cursor: pointer;
  transition: background-color 0.15s, border-color 0.15s;
}

.result-share-clickable:hover {
  background-color: #f8f9fa;
  border-color: #bdc1c6;
}

.result-share-clickable:active {
  background-color: #f1f3f4;
}

/* ===== 투표 종료 CTA ===== */
.vote-ended-actions {
  display: flex;
  gap: 12px;
  width: 100%;
}

.vote-ended-btn {
  flex: 1;
  height: 56px;
  border-radius: 20px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  cursor: pointer;
  transition: background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.vote-ended-btn-primary {
  background-color: #ff5531;
  color: #fff;
  border: none;
}

.vote-ended-btn-primary:hover {
  background-color: #e6442a;
}

.vote-ended-btn-secondary {
  background-color: #fff;
  color: #3c4043;
  border: 1px solid #dadce0;
}

.vote-ended-btn-secondary:hover {
  background-color: #f5f5f5;
}

.vote-ended-btn-outlined {
  background-color: #fff;
  color: #ff5531;
  border: 1px solid #ff5531;
}

.vote-ended-btn-outlined:hover {
  background-color: #fff5f3;
}

/* ===== 공유 모달 ===== */
.share-overlay {
  position: fixed;
  inset: 0;
  background-color: rgba(0, 0, 0, 0.5);
  z-index: 900;
  display: flex;
  align-items: center;
  justify-content: center;
}

.share-modal {
  background-color: #fff;
  border-radius: 24px;
  width: 100%;
  max-width: 480px;
  padding: 24px 24px 28px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.share-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.share-modal-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: #202124;
}

.share-modal-close {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 50%;
  cursor: pointer;
  transition: background-color 0.15s;
}

.share-modal-close:hover {
  background-color: rgba(0, 0, 0, 0.06);
}

.share-platforms {
  display: flex;
  gap: 16px;
  overflow-x: auto;
  overflow-y: visible;
  padding: 6px 4px;
}

.share-platform-btn {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  border: none;
  background: transparent;
  cursor: pointer;
  flex-shrink: 0;
  padding: 0;
}

.share-platform-icon {
  width: 56px;
  height: 56px;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: transform 0.15s, box-shadow 0.15s;
}

.share-platform-btn:hover .share-platform-icon {
  transform: scale(1.08);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.12);
}

.share-platform-btn:active .share-platform-icon {
  transform: scale(0.95);
}

.share-platform-btn span {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 12px;
  color: #5f6368;
  white-space: nowrap;
}

.share-link-box {
  display: flex;
  align-items: center;
  gap: 12px;
  height: 52px;
  border: 1px solid #dadce0;
  border-radius: 12px;
  padding: 0 6px 0 16px;
  background-color: #f8f9fa;
}

.share-link-box-text {
  flex: 1;
  min-width: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  color: #5f6368;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.share-link-copy-btn {
  flex-shrink: 0;
  height: 40px;
  padding: 0 20px;
  border: none;
  border-radius: 10px;
  background-color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 14px;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s;
}

.share-link-copy-btn:hover {
  background-color: #e6442a;
}

.share-modal-fade-enter-active,
.share-modal-fade-leave-active {
  transition: opacity 0.2s ease;
}

.share-modal-fade-enter-active .share-modal,
.share-modal-fade-leave-active .share-modal {
  transition: transform 0.2s ease;
}

.share-modal-fade-enter-from,
.share-modal-fade-leave-to {
  opacity: 0;
}

.share-modal-fade-enter-from .share-modal {
  transform: scale(0.95);
}

.share-modal-fade-leave-to .share-modal {
  transform: scale(0.95);
}

/* ===== 토스트 알림 ===== */
.copy-toast {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  background-color: #202124;
  color: #fff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  padding: 12px 24px;
  border-radius: 12px;
  z-index: 1000;
  pointer-events: none;
  white-space: nowrap;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.15);
}

.toast-fade-enter-active,
.toast-fade-leave-active {
  transition: opacity 0.25s ease, transform 0.25s ease;
}

.toast-fade-enter-from,
.toast-fade-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(8px);
}

.status-open {
  color: #34a853;
  font-weight: 700;
}

.status-closed {
  color: #ea4335;
  font-weight: 700;
}

.vote-loading {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 120px 0;
}

.vote-loading-spinner {
  width: 40px;
  height: 40px;
  border: 4px solid #e8eaed;
  border-top-color: #ff5531;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.vote-not-found {
  margin: 120px auto;
  text-align: center;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 20px;
  color: #5f6368;
}
</style>
