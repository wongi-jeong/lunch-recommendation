<template>
  <Teleport to="body">
    <div
      v-if="modelValue"
      class="roulette-overlay"
      role="dialog"
      aria-modal="true"
      aria-labelledby="roulette-title"
    >
      <div class="roulette-popup">
        <h2 id="roulette-title" class="sr-only">점심 추천 룰렛</h2>

        <div class="roulette-container">
          <!-- 룰렛 휠 SVG (viewBox 기반, 컨테이너와 함께 스케일) -->
          <svg
            class="roulette-wheel"
            viewBox="0 0 444 444"
            xmlns="http://www.w3.org/2000/svg"
            xmlns:xlink="http://www.w3.org/1999/xlink"
            aria-hidden="true"
          >
            <!-- 세그먼트: 2~5개 모두 arc로 통일 (웨지 이미지는 #E7F6ED가 거의 흰색이라 arc 사용) -->
            <g class="wheel-segments">
              <path
                v-for="(_, i) in displayRestaurants"
                :key="`arc-${i}`"
                :d="getSegmentPath(i)"
                :fill="getSegmentColor(i)"
              />
            </g>

            <!-- 테두리 점 + 중앙 원 -->
            <image
              :href="wheelAccessory"
              x="0"
              y="0"
              width="444"
              height="444"
              preserveAspectRatio="xMidYMid meet"
            />

            <!-- 식당명 라벨: SVG 내부에 rect + text, 항상 수평 -->
            <g
              v-for="(restaurant, i) in segmentRestaurants"
              :key="`label-${i}`"
              :transform="getLabelTransform(i)"
              class="segment-label"
            >
              <rect
                class="label-bg"
                x="-60"
                y="-16"
                width="120"
                height="32"
                rx="8"
                ry="8"
              />
              <text
                class="label-text"
                text-anchor="middle"
                dominant-baseline="central"
                x="0"
                y="0"
              >
                {{ truncateName(restaurant.name, 10) }}
              </text>
            </g>
          </svg>

          <!-- 포인터: 휠 위쪽 중앙 -->
          <img
            src="@/assets/roulette-pointer.svg"
            alt=""
            class="roulette-pointer"
            aria-hidden="true"
          />
        </div>

        <!-- 버튼 -->
        <div class="roulette-actions">
          <button type="button" class="btn-spin" @click="$emit('spin')">
            돌리기
          </button>
          <button
            type="button"
            class="btn-close"
            @click="$emit('update:modelValue', false)"
          >
            닫기
          </button>
        </div>
      </div>

      <div
        class="roulette-backdrop"
        aria-hidden="true"
        @click="$emit('update:modelValue', false)"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { computed } from 'vue'
import wheelAccessory from '@/assets/roulette-wheel-frame.svg'

const SEGMENT_COLORS = ['#26824E', '#5BC086', '#A1D9B7', '#34A666', '#7DCE9B']
const SEGMENT_COLORS_2 = ['#D9F0E0', '#5FCB7F']
const SEGMENT_COLORS_3 = ['#5BC086', '#A1D9B7', '#E7F6ED']
const SEGMENT_COLORS_4 = ['#5BC086', '#34A666', '#26824E', '#A1D9B7']

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  restaurants: { type: Array, default: () => [] }
})

defineEmits(['update:modelValue', 'spin'])

const displayRestaurants = computed(() => {
  const list = props.restaurants || []
  const len = Math.min(5, Math.max(2, list.length))
  return list.length >= 2 ? list.slice(0, len) : []
})

const segmentCount = computed(() => displayRestaurants.value.length)

const segmentRestaurants = computed(() => {
  const list = displayRestaurants.value
  if (list.length === 3) {
    return [list[1], list[2], list[0]]
  }
  return list
})

function getSegmentColor(index) {
  const n = segmentCount.value
  if (n === 2) return SEGMENT_COLORS_2[index]
  if (n === 3) return SEGMENT_COLORS_3[index]
  if (n === 4) return SEGMENT_COLORS_4[index]
  return SEGMENT_COLORS[index % 5]
}

function getSegmentPath(index) {
  const n = segmentCount.value
  if (n < 2) return ''
  const cx = 222
  const cy = 222
  const r = 218
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
  // n=2,4: 각도 증가 방향 → sweep 0(반시계). n=3,5: 각도 감소 → sweep 1(시계)
  const sweep = n === 2 || n === 4 ? 0 : 1
  return `M ${cx} ${cy} L ${x1} ${y1} A ${r} ${r} 0 ${large} ${sweep} ${x2} ${y2} Z`
}

/** 라벨 위치: 세그먼트 중앙, 항상 수평(회전 없음) */
function getLabelTransform(index) {
  const n = segmentCount.value
  let midDeg
  if (n === 2) {
    midDeg = [180, 0][index]
  } else if (n === 4) {
    midDeg = [135, 45, 225, 315][index]
  } else {
    const sliceAngle = 360 / n
    midDeg = 90 - (index + 0.5) * sliceAngle
  }
  const rad = (midDeg * Math.PI) / 180
  const r = 110
  const cx = 222
  const cy = 222
  const x = cx + r * Math.cos(rad)
  const y = cy - r * Math.sin(rad)
  return `translate(${x}, ${y})`
}

/** 긴 이름 축약 (SVG text는 ellipsis 지원 제한적) */
function truncateName(name, maxLen) {
  if (!name || typeof name !== 'string') return ''
  const trimmed = name.trim()
  if (trimmed.length <= maxLen) return trimmed
  return trimmed.slice(0, maxLen) + '…'
}
</script>

<style scoped>
.roulette-overlay {
  position: fixed;
  inset: 0;
  z-index: 2000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  box-sizing: border-box;
}

.roulette-backdrop {
  position: absolute;
  inset: 0;
  background: rgba(0, 0, 0, 0.5);
  cursor: pointer;
}

.roulette-popup {
  position: relative;
  z-index: 1;
  width: 100%;
  max-width: 480px;
  background: #ffffff;
  border-radius: 24px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.15);
  padding: 32px 24px 24px;
  display: flex;
  flex-direction: column;
  align-items: center;
  border: 1px solid #dadce0;
}

.roulette-container {
  position: relative;
  width: min(444px, calc(100vw - 48px));
  aspect-ratio: 1;
  flex-shrink: 0;
  margin-bottom: 24px;
}

.roulette-wheel {
  width: 100%;
  height: 100%;
  display: block;
}


.segment-label {
  pointer-events: none;
}

.label-bg {
  fill: #ffffff;
  stroke: rgba(0, 0, 0, 0.1);
  stroke-width: 1;
}

.label-text {
  fill: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 13px;
}

.roulette-pointer {
  position: absolute;
  top: -22px;
  left: 50%;
  transform: translateX(-50%);
  width: 35px;
  height: 65px;
  object-fit: contain;
  pointer-events: none;
}

.roulette-actions {
  display: flex;
  gap: 12px;
  width: 100%;
  max-width: 360px;
  justify-content: center;
}

.btn-spin {
  flex: 1;
  min-width: 0;
  height: 48px;
  border: none;
  border-radius: 14px;
  background: #ff5531;
  color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  cursor: pointer;
}

.btn-spin:hover {
  background: #e54d2b;
}

.btn-close {
  flex: 0 0 auto;
  min-width: 90px;
  height: 48px;
  padding: 0 20px;
  border: 1px solid #dadce0;
  border-radius: 14px;
  background: #ffffff;
  color: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  cursor: pointer;
}

.btn-close:hover {
  background: #f8f9fa;
  border-color: #bdc1c6;
}

.sr-only {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}

@media (max-width: 520px) {
  .roulette-pointer {
    width: 28px;
    height: 52px;
    top: -16px;
  }

  .label-text {
    font-size: 11px;
  }
}
</style>
