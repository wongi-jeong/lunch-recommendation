<script setup>
const props = defineProps({
  modelValue: {
    type: String,
    required: true
  }
})

const emit = defineEmits(['update:modelValue'])

const setOption = (value) => {
  if (props.modelValue !== value) {
    emit('update:modelValue', value)
  }
}
</script>

<template>
  <div class="timer-section">
    <div class="timer-header">
      <div class="timer-label">
        <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 6v6l4 2" stroke="#202124" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
          <circle cx="12" cy="12" r="9" stroke="#202124" stroke-width="2" />
        </svg>
        <span class="timer-text">타이머</span>
      </div>
      <div class="timer-options">
        <label class="radio-option" :class="{ active: modelValue === 'none' }">
          <input
            type="radio"
            class="radio-hidden"
            :checked="modelValue === 'none'"
            @change="setOption('none')"
          />
          <span class="radio-circle" :class="{ filled: modelValue === 'none' }"></span>
          <span class="radio-label">타이머 없음(직접 종료)</span>
        </label>
        <label class="radio-option" :class="{ active: modelValue === '10min' }">
          <input
            type="radio"
            class="radio-hidden"
            :checked="modelValue === '10min'"
            @change="setOption('10min')"
          />
          <span class="radio-circle" :class="{ filled: modelValue === '10min' }"></span>
          <span class="radio-label">10분 후</span>
        </label>
        <label class="radio-option" :class="{ active: modelValue === '30min' }">
          <input
            type="radio"
            class="radio-hidden"
            :checked="modelValue === '30min'"
            @change="setOption('30min')"
          />
          <span class="radio-circle" :class="{ filled: modelValue === '30min' }"></span>
          <span class="radio-label">30분 후</span>
        </label>
        <label class="radio-option" :class="{ active: modelValue === '1hour' }">
          <input
            type="radio"
            class="radio-hidden"
            :checked="modelValue === '1hour'"
            @change="setOption('1hour')"
          />
          <span class="radio-circle" :class="{ filled: modelValue === '1hour' }"></span>
          <span class="radio-label">1시간 후</span>
        </label>
      </div>
    </div>
  </div>
</template>

<style scoped>
.timer-section {
  width: 664px;
  padding-bottom: 48px;
}

.timer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.timer-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timer-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #202124;
}

.timer-options {
  display: flex;
  align-items: center;
  gap: 12px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-hidden {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.radio-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #bdc1c6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.radio-circle.filled {
  border-color: #ff5531;
}

.radio-circle.filled::after {
  content: '';
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ff5531;
}

.radio-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #9aa0a6;
  white-space: nowrap;
}

.radio-option.active .radio-label {
  color: #202124;
}
</style>
