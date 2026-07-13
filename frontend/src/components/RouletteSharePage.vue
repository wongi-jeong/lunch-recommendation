<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import wheelAccessory from '@/assets/icon-roulette-wheel.svg'
import roulettePointer from '@/assets/icon-roulette-pointer.svg'
import externalLinkIcon from '@/assets/icon-external-link.svg'
import starIcon from '@/assets/icon-star.svg'
import defaultThumbnail from '@/assets/img-placeholder-restaurant.png'
import VoteShareModal from '@/components/vote/VoteShareModal.vue'

const SEGMENT_COLORS_2 = ['#D9F0E0', '#5FCB7F']
const SEGMENT_COLORS_3 = ['#5BC086', '#A1D9B7', '#E7F6ED']
const SEGMENT_COLORS_4 = ['#5BC086', '#34A666', '#26824E', '#A1D9B7']
const SEGMENT_COLORS_5 = ['#26824E', '#5BC086', '#A1D9B7', '#34A666', '#7DCE9B']

const route = useRoute()
const router = useRouter()
const { getToken } = useAuth()

const isFromHistory = computed(() => {
  const flag = route.query.fromHistory
  if (flag === 'true' || flag === '1') return true
  return Boolean(flag)
})

const decodedData = computed(() => {
  const raw = route.query.data
  if (!raw || typeof raw !== 'string') return null
  try {
    return JSON.parse(decodeURIComponent(raw))
  } catch {
    return null
  }
})

const restaurants = computed(() => {
  const list = decodedData.value?.restaurants || []
  return Array.isArray(list) ? list.slice(0, 5) : []
})

const count = computed(() => restaurants.value.length)

const winnerName = computed(() =>
  decodedData.value?.winnerName || restaurants.value[0]?.name || ''
)

const winnerIndex = computed(() => {
  const name = winnerName.value
  const idx = restaurants.value.findIndex(r => r.name === name)
  return idx >= 0 ? idx : 0
})

const winner = computed(() =>
  restaurants.value[winnerIndex.value] || restaurants.value[0] || null
)

const hasValidData = computed(() => count.value >= 2)

const shareDataRaw = computed(() => {
  const raw = route.query.data
  return typeof raw === 'string' ? raw : ''
})

const saveHistoryEntry = async () => {
  if (!hasValidData.value || !winner.value || !decodedData.value) return

  // 히스토리에서 조회로 들어온 경우에는 새로 저장하지 않음
  if (isFromHistory.value) return

  const token = getToken()
  if (!token) return
  const shareData = shareDataRaw.value
  if (!shareData) return

  try {
    await fetch('/api/roulette-history', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': token
      },
      body: JSON.stringify({
        winnerName: winnerName.value || '',
        winnerPlaceId: decodedData.value?.winnerPlaceId || winner.value?.googlePlaceId || '',
        shareData
      })
    })
  } catch (e) {
    console.warn('룰렛 히스토리 저장 실패:', e)
  }
}

onMounted(() => {
  saveHistoryEntry()
})

function getRestaurantImage(restaurant) {
  if (restaurant?.photoName && Array.isArray(restaurant.photoName) && restaurant.photoName.length > 0) {
    return `/api/restaurants/photo?name=${encodeURIComponent(restaurant.photoName[0])}`
  }
  return defaultThumbnail
}

function handleImageError(event) {
  event.target.src = defaultThumbnail
}

function openGoogleMaps() {
  if (winner.value?.googleMapsUri) {
    window.open(winner.value.googleMapsUri, '_blank', 'noopener')
  }
}

function handleBack() {
  router.push({ name: 'nearby' })
}

// 공유 모달 (투표 페이지와 동일한 방식)
const showShareModal = ref(false)
const resultPageLink = computed(() =>
  typeof window !== 'undefined' ? window.location.href : ''
)
const shareText = computed(() =>
  winnerName.value
    ? `오늘 점심은 "${winnerName.value}"! 룰렛 결과를 확인해보세요.`
    : '룰렛 결과를 확인해보세요.'
)

const copyModalLink = async () => {
  try {
    await navigator.clipboard.writeText(resultPageLink.value)
    triggerToast()
  } catch { /* silent */ }
}

const shareToKakao = () => {
  window.open(
    `https://sharer.kakao.com/talk/friends/picker/link?url=${encodeURIComponent(resultPageLink.value)}&text=${encodeURIComponent(shareText.value)}`,
    '_blank',
    'width=600,height=700'
  )
}
const shareToFacebook = () => {
  window.open(
    `https://www.facebook.com/sharer/sharer.php?u=${encodeURIComponent(resultPageLink.value)}`,
    '_blank',
    'width=600,height=500'
  )
}
const shareToX = () => {
  window.open(
    `https://x.com/intent/post?text=${encodeURIComponent(shareText.value)}&url=${encodeURIComponent(resultPageLink.value)}`,
    '_blank',
    'width=600,height=500'
  )
}
const shareToBand = () => {
  window.open(
    `https://band.us/plugin/share?body=${encodeURIComponent(shareText.value + '\n' + resultPageLink.value)}`,
    '_blank',
    'width=600,height=500'
  )
}
const shareToEmail = () => {
  window.location.href = `mailto:?subject=${encodeURIComponent('룰렛 결과 공유')}&body=${encodeURIComponent(shareText.value + '\n\n' + resultPageLink.value)}`
}

const showToast = ref(false)
let toastTimer = null
const triggerToast = () => {
  showToast.value = true
  if (toastTimer) clearTimeout(toastTimer)
  toastTimer = setTimeout(() => { showToast.value = false }, 2000)
}

// ── Roulette wheel (static) ──

const segmentCount = computed(() => count.value)

const segmentRestaurants = computed(() => {
  const list = restaurants.value
  if (list.length === 3) return [list[1], list[2], list[0]]
  return list
})

const winnerSegmentIndex = computed(() => {
  const name = winnerName.value
  return segmentRestaurants.value.findIndex(r => r.name === name)
})

function getSegmentColor(index) {
  const n = segmentCount.value
  if (n === 2) return SEGMENT_COLORS_2[index]
  if (n === 3) return SEGMENT_COLORS_3[index]
  if (n === 4) return SEGMENT_COLORS_4[index]
  return SEGMENT_COLORS_5[index % 5]
}

function getSegmentPath(index) {
  const n = segmentCount.value
  if (n < 2) return ''
  const cx = 222, cy = 222, r = 218
  let startDeg, endDeg

  if (n === 3) {
    const sliceAngle = 120
    startDeg = 90 - index * sliceAngle
    endDeg = 90 - (index + 1) * sliceAngle
  } else if (n === 4) {
    const starts = [90, 180, 270, 0]
    startDeg = starts[index]
    endDeg = (starts[index] + 90) % 360 || 360
  } else {
    const sliceAngle = 360 / n
    startDeg = 90 - index * sliceAngle
    endDeg = 90 - (index + 1) * sliceAngle
  }

  const startRad = (startDeg * Math.PI) / 180
  const endRad = (endDeg * Math.PI) / 180
  const x1 = cx + r * Math.cos(startRad)
  const y1 = cy - r * Math.sin(startRad)
  const x2 = cx + r * Math.cos(endRad)
  const y2 = cy - r * Math.sin(endRad)
  const sliceAngle = Math.abs(endDeg - startDeg) || 90
  const large = sliceAngle > 180 ? 1 : 0
  const sweep = n === 2 || n === 4 ? 0 : 1
  return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} ${sweep} ${x2} ${y2} Z`
}

function getSegmentMidDeg(index) {
  const n = segmentCount.value
  if (n === 2) return [180, 0][index]
  if (n === 4) return [135, 45, 225, 315][index]
  const sliceAngle = 360 / n
  return 90 - (index + 0.5) * sliceAngle
}

function getLabelTransform(index) {
  const midDeg = getSegmentMidDeg(index)
  const rad = (midDeg * Math.PI) / 180
  const labelR = 125
  const cx = 222, cy = 222
  const x = cx + labelR * Math.cos(rad)
  const y = cy - labelR * Math.sin(rad)
  const n = segmentCount.value
  if (n <= 2) return `translate(${x}, ${y})`
  return `translate(${x}, ${y}) rotate(${-midDeg + 90})`
}

function truncateName(name, maxLen) {
  if (!name || typeof name !== 'string') return ''
  const trimmed = name.trim()
  return trimmed.length <= maxLen ? trimmed : trimmed.slice(0, maxLen) + '…'
}

const wheelRotation = computed(() => {
  if (winnerSegmentIndex.value < 0) return 0
  const midDeg = getSegmentMidDeg(winnerSegmentIndex.value)
  return ((midDeg - 90) % 360 + 360) % 360
})
</script>

<template>
  <main class="share-page" aria-labelledby="share-title">
    <div v-if="hasValidData" class="share-result">
      <!-- Left: winner info -->
      <section class="share-left" aria-label="당첨 결과">
        <h1 id="share-title" class="winner-title">
          [{{ winnerName }}]이<br>당첨되었습니다!
        </h1>

        <article v-if="winner" class="winner-card">
          <div class="winner-card-body">
            <div class="winner-thumbnail">
              <img
                :src="getRestaurantImage(winner)"
                :alt="winner.name"
                class="winner-thumbnail-img"
                @error="handleImageError"
              />
            </div>

            <div class="winner-info">
              <p class="winner-name">{{ winner.name }}</p>

              <div class="winner-meta">
                <span
                  v-for="(cat, i) in (winner.categories || [])"
                  :key="i"
                  class="winner-category"
                >{{ cat }}</span>
                <span
                  v-if="winner.openNow === true"
                  class="winner-category"
                >영업중</span>
                <span
                  v-else-if="winner.openNow === false"
                  class="winner-category winner-category--closed"
                >영업종료</span>
              </div>

              <div v-if="typeof winner.rating === 'number'" class="winner-rating">
                <img :src="starIcon" alt="" class="star-icon" aria-hidden="true" />
                <span>{{ winner.rating.toFixed(1) }}</span>
              </div>

              <p v-if="winner.distanceMeters" class="winner-distance">
                현 위치에서 {{ winner.distanceMeters }}m
              </p>
            </div>
          </div>

          <div class="winner-card-actions">
            <button
              v-if="winner.googleMapsUri"
              type="button"
              class="icon-btn"
              aria-label="Google 지도에서 보기"
              @click="openGoogleMaps"
            >
              <img :src="externalLinkIcon" alt="" aria-hidden="true" />
            </button>
          </div>
        </article>

        <button
          type="button"
          class="btn-copy-link"
          aria-label="결과 링크 공유"
          @click="showShareModal = true"
        >
          결과 링크 공유하기
        </button>
      </section>

      <VoteShareModal
        :visible="showShareModal"
        title="룰렛 결과 공유"
        :link="resultPageLink"
        @close="showShareModal = false"
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

      <!-- Right: static roulette wheel -->
      <section class="share-right" aria-label="룰렛 결과">
        <div class="wheel-wrapper">
          <svg
            class="wheel-svg"
            :style="{ transform: `rotate(${wheelRotation}deg)` }"
            viewBox="0 0 444 444"
            xmlns="http://www.w3.org/2000/svg"
            aria-hidden="true"
          >
            <g>
              <path
                v-for="(_, i) in restaurants"
                :key="`arc-${i}`"
                :d="getSegmentPath(i)"
                :fill="getSegmentColor(i)"
              />
            </g>
            <image
              :href="wheelAccessory"
              x="0" y="0"
              width="444" height="444"
              preserveAspectRatio="xMidYMid meet"
            />
            <g
              v-for="(restaurant, i) in segmentRestaurants"
              :key="`label-${i}`"
              :transform="getLabelTransform(i)"
              class="segment-label"
            >
              <rect
                class="label-bg"
                :class="{ 'label-bg--winner': i === winnerSegmentIndex }"
                x="-60" y="-16"
                width="120" height="32"
                rx="8" ry="8"
              />
              <text
                class="label-text"
                :class="{ 'label-text--winner': i === winnerSegmentIndex }"
                text-anchor="middle"
                dominant-baseline="central"
                x="0" y="0"
              >
                {{ truncateName(restaurant.name, 10) }}
              </text>
            </g>
          </svg>
          <img
            :src="roulettePointer"
            alt=""
            class="wheel-pointer"
            aria-hidden="true"
          />
        </div>
      </section>
    </div>

    <div v-else class="share-empty">
      <p>공유할 수 있는 룰렛 결과가 없습니다.</p>
      <button type="button" class="btn-back" @click="handleBack">
        추천 화면으로 돌아가기
      </button>
    </div>
  </main>
</template>

<style scoped src="./RouletteSharePage.css"></style>

<!-- 공유 모달·토스트 스타일 (VoteShareModal / VotePage와 동일) -->
<style>
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

.share-modal-fade-enter-from .share-modal,
.share-modal-fade-leave-to .share-modal {
  transform: scale(0.95);
}

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
</style>
