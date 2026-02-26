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

        <p v-if="phase === 'result'" class="result-title">
          [{{ winnerName }}]이<br>당첨되었습니다!
        </p>

        <div class="roulette-container">
          <svg
            class="roulette-wheel"
            :class="{ 'wheel-spinning': isTransitioning }"
            :style="{ transform: `rotate(${wheelRotation}deg)` }"
            viewBox="0 0 444 444"
            xmlns="http://www.w3.org/2000/svg"
            xmlns:xlink="http://www.w3.org/1999/xlink"
            aria-hidden="true"
          >
            <g class="wheel-segments">
              <path
                v-for="(_, i) in displayRestaurants"
                :key="`arc-${i}`"
                :d="getSegmentPath(i)"
                :fill="getSegmentColor(i)"
              />
            </g>

            <image
              :href="wheelAccessory"
              x="0"
              y="0"
              width="444"
              height="444"
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
                :class="{ 'label-bg--winner': phase === 'result' && i === winnerSegmentIndex }"
                x="-60"
                y="-16"
                width="120"
                height="32"
                rx="8"
                ry="8"
              />
              <text
                class="label-text"
                :class="{ 'label-text--winner': phase === 'result' && i === winnerSegmentIndex }"
                text-anchor="middle"
                dominant-baseline="central"
                x="0"
                y="0"
              >
                {{ truncateName(restaurant.name, 10) }}
              </text>
            </g>
          </svg>

          <img
            src="@/assets/roulette-pointer.svg"
            alt=""
            class="roulette-pointer"
            aria-hidden="true"
          />
        </div>

        <div class="roulette-actions">
          <template v-if="phase === 'result'">
            <button type="button" class="btn-share" @click="handleShare">
              결과 공유하기
            </button>
            <button type="button" class="btn-respin" @click="handleRespin">
              다시 돌리기
            </button>
          </template>
          <template v-else>
            <button
              type="button"
              class="btn-spin"
              :disabled="phase === 'spinning'"
              @click="startSpin"
            >
              돌리기
            </button>
            <button
              type="button"
              class="btn-close"
              :disabled="phase === 'spinning'"
              @click="handleClose"
            >
              닫기
            </button>
          </template>
        </div>
      </div>

      <div
        class="roulette-backdrop"
        aria-hidden="true"
        @click="handleClose"
      />
    </div>
  </Teleport>
</template>

<script setup>
import { ref, computed, watch, nextTick } from 'vue'
import wheelAccessory from '@/assets/roulette-wheel-frame.svg'

const SEGMENT_COLORS = ['#26824E', '#5BC086', '#A1D9B7', '#34A666', '#7DCE9B']
const SEGMENT_COLORS_2 = ['#D9F0E0', '#5FCB7F']
const SEGMENT_COLORS_3 = ['#5BC086', '#A1D9B7', '#E7F6ED']
const SEGMENT_COLORS_4 = ['#5BC086', '#34A666', '#26824E', '#A1D9B7']
const SPIN_DURATION = 4000

const props = defineProps({
  modelValue: { type: Boolean, default: false },
  restaurants: { type: Array, default: () => [] }
})

const emit = defineEmits(['update:modelValue', 'share', 'result'])

const phase = ref('idle')
const wheelRotation = ref(0)
const winnerSegmentIndex = ref(-1)
const isTransitioning = ref(false)
let cumulativeRotation = 0
let spinTimer = null

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

const winnerName = computed(() => {
  if (winnerSegmentIndex.value < 0 || !segmentRestaurants.value[winnerSegmentIndex.value]) return ''
  return segmentRestaurants.value[winnerSegmentIndex.value].name || ''
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
  const cx = 222
  const cy = 222
  const x = cx + labelR * Math.cos(rad)
  const y = cy - labelR * Math.sin(rad)

  const n = segmentCount.value
  if (n <= 2) {
    return `translate(${x}, ${y})`
  }

  const rotation = -midDeg + 90

  return `translate(${x}, ${y}) rotate(${rotation})`
}

function truncateName(name, maxLen) {
  if (!name || typeof name !== 'string') return ''
  const trimmed = name.trim()
  if (trimmed.length <= maxLen) return trimmed
  return trimmed.slice(0, maxLen) + '…'
}

async function startSpin() {
  if (phase.value === 'spinning' || segmentCount.value < 2) return

  const n = segmentCount.value
  const winnerIdx = Math.floor(Math.random() * n)
  winnerSegmentIndex.value = winnerIdx

  const midDeg = getSegmentMidDeg(winnerIdx)
  let targetAngle = ((midDeg - 90) % 360 + 360) % 360

  const sliceAngle = 360 / n
  targetAngle += (Math.random() - 0.5) * sliceAngle * 0.5
  targetAngle = ((targetAngle % 360) + 360) % 360

  const currentMod = ((cumulativeRotation % 360) + 360) % 360
  let delta = targetAngle - currentMod
  if (delta < 0) delta += 360

  delta += (5 + Math.floor(Math.random() * 3)) * 360
  cumulativeRotation += delta

  phase.value = 'spinning'

  isTransitioning.value = true
  await nextTick()
  requestAnimationFrame(() => {
    wheelRotation.value = cumulativeRotation
  })

  if (spinTimer) clearTimeout(spinTimer)
  spinTimer = setTimeout(() => {
    isTransitioning.value = false
    phase.value = 'result'
    emit('result', segmentRestaurants.value[winnerIdx])
  }, SPIN_DURATION + 200)
}

function handleRespin() {
  startSpin()
}

function handleShare() {
  if (winnerSegmentIndex.value >= 0) {
    emit('share', segmentRestaurants.value[winnerSegmentIndex.value])
  }
}

function handleClose() {
  if (phase.value === 'spinning') return
  emit('update:modelValue', false)
}

watch(() => props.modelValue, (open) => {
  if (open) {
    phase.value = 'idle'
    isTransitioning.value = false
    wheelRotation.value = 0
    cumulativeRotation = 0
    winnerSegmentIndex.value = -1
    if (spinTimer) {
      clearTimeout(spinTimer)
      spinTimer = null
    }
  }
})
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
  border-radius: 32px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  padding: 40px;
  display: flex;
  flex-direction: column;
  align-items: center;
}

.result-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 22px;
  line-height: 1.35;
  color: #31373c;
  text-align: center;
  margin: -20px 0 28px;
}

.roulette-container {
  position: relative;
  width: min(458px, calc(100vw - 48px));
  aspect-ratio: 458 / 476;
  flex-shrink: 0;
  margin-bottom: 24px;
}

.roulette-wheel {
  width: 100%;
  height: 100%;
  display: block;
  will-change: transform;
}

.roulette-wheel.wheel-spinning {
  transition: transform 4s cubic-bezier(0.17, 0.67, 0.12, 0.99);
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
  justify-content: center;
}

.btn-spin {
  flex: 1;
  min-width: 0;
  height: 56px;
  border: none;
  border-radius: 16px;
  background: #ff5531;
  color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
}

.btn-spin:hover:not(:disabled) {
  background: #e54d2b;
}

.btn-spin:disabled {
  background: #e8eaed;
  color: #bdc1c6;
  cursor: not-allowed;
}

.btn-close {
  flex: 0 0 auto;
  height: 56px;
  padding: 0 20px;
  border: 1px solid #dadce0;
  border-radius: 16px;
  background: #ffffff;
  color: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
}

.btn-close:hover:not(:disabled) {
  background: #f8f9fa;
  border-color: #bdc1c6;
}

.btn-close:disabled {
  color: #bdc1c6;
  cursor: not-allowed;
}

.btn-share {
  flex: 1;
  min-width: 0;
  height: 56px;
  border: none;
  border-radius: 16px;
  background: #ff5531;
  color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
}

.btn-share:hover {
  background: #e54d2b;
}

.btn-respin {
  flex: 0 0 auto;
  height: 56px;
  padding: 0 20px;
  border: 1px solid #dadce0;
  border-radius: 16px;
  background: #ffffff;
  color: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  cursor: pointer;
}

.btn-respin:hover {
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

  .btn-spin,
  .btn-close,
  .btn-share,
  .btn-respin {
    height: 48px;
    font-size: 16px;
  }
}
</style>
