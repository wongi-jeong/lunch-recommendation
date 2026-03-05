<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import wheelAccessory from '@/assets/roulette-wheel-frame.svg'
import roulettePointer from '@/assets/roulette-pointer.svg'
import externalLinkIcon from '@/assets/external-link-icon.svg'
import starIcon from '@/assets/star-icon.svg'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'

const SEGMENT_COLORS_2 = ['#D9F0E0', '#5FCB7F']
const SEGMENT_COLORS_3 = ['#5BC086', '#A1D9B7', '#E7F6ED']
const SEGMENT_COLORS_4 = ['#5BC086', '#34A666', '#26824E', '#A1D9B7']
const SEGMENT_COLORS_5 = ['#26824E', '#5BC086', '#A1D9B7', '#34A666', '#7DCE9B']

const route = useRoute()
const router = useRouter()

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
      </section>

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

<style scoped>
.share-page {
  flex: 1;
  min-height: 0;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #ffffff;
  box-sizing: border-box;
}

.share-result {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 50px;
  max-width: 966px;
  width: 100%;
  padding: 40px 24px;
  box-sizing: border-box;
}

/* ── Left: winner info ── */
.share-left {
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 458px;
  flex-shrink: 0;
  gap: 24px;
}

.winner-title {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 22px;
  line-height: 1.35;
  color: #31373c;
  text-align: center;
}

.winner-card {
  display: flex;
  gap: 12px;
  align-items: flex-start;
  width: 384px;
  padding: 24px;
  border-radius: 24px;
  border: 1px solid #dadce0;
  background: #ffffff;
  box-sizing: border-box;
}

.winner-card-body {
  display: flex;
  gap: 20px;
  align-items: center;
  flex: 1;
  min-width: 0;
}

.winner-thumbnail {
  width: 120px;
  height: 120px;
  border-radius: 6px;
  overflow: hidden;
  flex-shrink: 0;
  background: #f1f3f4;
}

.winner-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.winner-info {
  display: flex;
  flex-direction: column;
  gap: 6px;
  justify-content: center;
  min-width: 0;
}

.winner-name {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.winner-meta {
  display: flex;
  gap: 8px;
  align-items: center;
  flex-wrap: wrap;
}

.winner-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  white-space: nowrap;
}

.winner-category--closed {
  color: #ea4335;
}

.winner-rating {
  display: flex;
  align-items: center;
  gap: 4px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
}

.star-icon {
  width: 20px;
  height: 20px;
  flex-shrink: 0;
}

.winner-distance {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  white-space: nowrap;
}

.winner-card-actions {
  display: flex;
  flex-direction: column;
  gap: 4px;
  flex-shrink: 0;
}

.icon-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
}

.icon-btn img {
  width: 24px;
  height: 24px;
  display: block;
}

/* ── Right: roulette wheel ── */
.share-right {
  width: 458px;
  flex-shrink: 0;
}

.wheel-wrapper {
  position: relative;
  width: 458px;
  aspect-ratio: 458 / 476;
}

.wheel-svg {
  width: 100%;
  height: 100%;
  display: block;
}

.segment-label {
  pointer-events: none;
}

.label-bg {
  fill: #ffffff;
  stroke: #1b623a;
  stroke-width: 1;
}

.label-bg.label-bg--winner {
  fill: #ff5531;
  stroke: none;
}

.label-text {
  fill: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 13px;
}

.label-text.label-text--winner {
  fill: #ffffff;
}

.wheel-pointer {
  position: absolute;
  top: -22px;
  left: 50%;
  transform: translateX(-50%);
  width: 35px;
  height: 65px;
  object-fit: contain;
  pointer-events: none;
}

/* ── Empty state ── */
.share-empty {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 16px;
  font-family: 'Pretendard', sans-serif;
  font-size: 16px;
  color: #5f6368;
}

.btn-back {
  height: 48px;
  padding: 0 24px;
  border-radius: 16px;
  border: 1px solid #dadce0;
  background: #ffffff;
  color: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
}

.btn-back:hover {
  background: #f8f9fa;
}

/* ── Responsive ── */
@media (max-width: 1024px) {
  .share-result {
    flex-direction: column-reverse;
    gap: 32px;
  }

  .share-left,
  .share-right {
    width: 100%;
    max-width: 458px;
  }

  .winner-card {
    width: 100%;
  }

  .wheel-wrapper {
    width: 100%;
    max-width: 458px;
    margin: 0 auto;
  }
}

@media (max-width: 520px) {
  .winner-card {
    padding: 16px;
    border-radius: 16px;
  }

  .winner-thumbnail {
    width: 80px;
    height: 80px;
  }

  .winner-name {
    font-size: 16px;
  }

  .winner-category,
  .winner-rating,
  .winner-distance {
    font-size: 13px;
  }

  .label-text {
    font-size: 11px;
  }

  .wheel-pointer {
    width: 28px;
    height: 52px;
    top: -16px;
  }
}
</style>
