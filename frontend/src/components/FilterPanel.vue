<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      distance: 300,
      foodTypes: []
    })
  },
  loading: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['update:modelValue', 'recommend'])

const isExpanded = ref(true)

const distanceOptions = [
  { value: 300, label: '300m' },
  { value: 800, label: '800m' },
  { value: 1200, label: '1.2km' }
]

const foodTypes = [
  { id: 'korean', label: '한식' },
  { id: 'chinese', label: '중식' },
  { id: 'japanese', label: '일식' },
  { id: 'western', label: '양식' },
  { id: 'vegan', label: '비건' },
  { id: 'asian', label: '아시안' },
  { id: 'fastfood', label: '패스트푸드' },
  { id: 'meat', label: '고기' },
  { id: 'noodle', label: '면/국물' }
]

const allSelected = computed(() => {
  return foodTypes.every(type => props.modelValue.foodTypes.includes(type.id))
})

const selectedDistance = computed({
  get: () => props.modelValue.distance,
  set: (value) => {
    emit('update:modelValue', {
      ...props.modelValue,
      distance: value
    })
  }
})

const selectedFoodTypes = computed({
  get: () => props.modelValue.foodTypes,
  set: (value) => {
    emit('update:modelValue', {
      ...props.modelValue,
      foodTypes: value
    })
  }
})

const toggleAllFoodTypes = () => {
  if (allSelected.value) {
    selectedFoodTypes.value = []
  } else {
    selectedFoodTypes.value = foodTypes.map(type => type.id)
  }
}

const toggleFoodType = (typeId) => {
  const index = selectedFoodTypes.value.indexOf(typeId)
  if (index > -1) {
    selectedFoodTypes.value = selectedFoodTypes.value.filter(id => id !== typeId)
  } else {
    selectedFoodTypes.value = [...selectedFoodTypes.value, typeId]
  }
}

const canRecommend = computed(() => (props.modelValue.foodTypes?.length ?? 0) > 0)

// 접힌 상태에서 표시할 거리 라벨 (예: "800m")
const selectedDistanceChipLabel = computed(() => {
  const option = distanceOptions.find(o => o.value === props.modelValue.distance)
  return option ? option.label : ''
})

// 접힌 상태에서 표시할 음식 종류 칩 목록 (id, label)
const selectedFoodTypeChips = computed(() => {
  const ids = props.modelValue.foodTypes ?? []
  return ids.map(id => {
    const type = foodTypes.find(t => t.id === id)
    return type ? { id, label: type.label } : null
  }).filter(Boolean)
})

const removeFoodType = (typeId) => {
  selectedFoodTypes.value = selectedFoodTypes.value.filter(id => id !== typeId)
}

const handleRecommend = () => {
  if (props.loading || !canRecommend.value) return
  emit('recommend')
}
</script>

<template>
  <div class="filter-bar-wrapper">
    <div class="filter-panel" :class="{ collapsed: !isExpanded }">
      <div class="filter-header" :class="{ collapsed: !isExpanded }">
        <span class="filter-title">필터</span>
        <span class="filter-subtitle">새 필터</span>
        <button class="icon-button" @click="isExpanded = !isExpanded">
        <svg v-if="isExpanded" width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M7 14L12 9L17 14" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
        <svg v-else width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M7 10L12 15L17 10" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </div>

    <div v-if="isExpanded" class="filter-content">
      <!-- 거리 필터 -->
      <div class="filter-section">
        <h3 class="section-title">거리</h3>
        <div class="distance-buttons">
          <button
            v-for="option in distanceOptions"
            :key="option.value"
            class="distance-button"
            :class="{ active: selectedDistance === option.value }"
            @click="selectedDistance = option.value"
          >
            {{ option.label }}
          </button>
        </div>
      </div>

      <!-- 음식 종류 필터 -->
      <div class="filter-section">
        <h3 class="section-title">음식 종류</h3>
        <div class="checkbox-wrapper">
          <input
            type="checkbox"
            :id="'all-food-types'"
            :checked="allSelected"
            @change="toggleAllFoodTypes"
            class="checkbox-input"
          />
          <label :for="'all-food-types'" class="checkbox-label">전체 선택</label>
        </div>
        <div class="food-type-buttons">
          <button
            v-for="type in foodTypes"
            :key="type.id"
            class="food-type-button"
            :class="{ active: selectedFoodTypes.includes(type.id) }"
            @click="toggleFoodType(type.id)"
          >
            {{ type.label }}
          </button>
        </div>
      </div>

      <!-- 추천 받기 버튼 (음식 종류 1개 이상 선택 시에만 활성화) -->
      <button 
        class="recommend-button" 
        :class="{ loading }"
        :disabled="loading || !canRecommend"
        :title="!canRecommend ? '음식 종류를 하나 이상 선택해주세요' : undefined"
        @click="handleRecommend"
      >
        <span v-if="loading" class="loading-spinner"></span>
        {{ loading ? '위치 확인 중...' : '추천 받기' }}
      </button>
    </div>
  </div>

  <!-- 접힌 상태일 때만: 선택된 필터 칩을 패널 오른쪽 바깥에 표시 -->
  <div v-if="!isExpanded" class="filter-chips-outside">
    <span v-if="selectedDistanceChipLabel" class="filter-chip filter-chip--distance">
      {{ selectedDistanceChipLabel }}
    </span>
    <span
      v-for="chip in selectedFoodTypeChips"
      :key="chip.id"
      class="filter-chip filter-chip--removable"
    >
      {{ chip.label }}
      <button type="button" class="filter-chip-remove" aria-label="필터 제거" @click="removeFoodType(chip.id)">
        <svg width="14" height="14" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M18 6L6 18M6 6l12 12" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
        </svg>
      </button>
    </span>
  </div>
  </div>
</template>

<style scoped>
.filter-bar-wrapper {
  position: absolute;
  top: 10px;
  left: 360px;
  display: flex;
  align-items: center;
  gap: 12px;
  z-index: 1000;
}

.filter-panel {
  width: 400px;
  background-color: white;
  border-radius: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  overflow: visible;
  flex-shrink: 0;
}

.filter-panel:not(.collapsed) {
  bottom: 300px;
}

.filter-panel.collapsed {
  width: fit-content;
  height: fit-content;
}

.filter-chips-outside {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
}

.filter-header {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 20px 24px;
  border-bottom: 1px solid #e8eaed;
  height: 12px;
}

.filter-header.collapsed {
  border-bottom: none;
}

.filter-chip {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  padding: 6px 12px;
  background-color: #fff;
  border: 1px solid #e8eaed;
  border-radius: 20px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  color: #5f6368;
  white-space: nowrap;
}

.filter-chip--distance {
  padding: 6px 12px;
}

.filter-chip--removable {
  padding: 6px 8px 6px 12px;
}

.filter-chip-remove {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  padding: 0;
  border: none;
  background: transparent;
  color: #5f6368;
  cursor: pointer;
  border-radius: 50%;
  transition: color 0.2s, background-color 0.2s;
}

.filter-chip-remove:hover {
  color: #202124;
  background-color: rgba(0, 0, 0, 0.06);
}

.filter-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  color: #202124;
}

.filter-subtitle {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 16px;
  color: #5f6368;
  margin-left: 8px;
}

.icon-button {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
  margin-left: auto;
}

.icon-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.filter-content {
  padding: 24px;
}

.filter-section {
  margin-bottom: 32px;
}

.filter-section:last-of-type {
  margin-bottom: 24px;
}

.section-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  color: #202124;
  margin: 0 0 16px 0;
}

.distance-buttons {
  display: flex;
  gap: 12px;
}

.distance-button {
  flex: 1;
  padding: 12px 16px;
  border: 2px solid #e8eaed;
  border-radius: 24px;
  background-color: white;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #5f6368;
  cursor: pointer;
  transition: all 0.2s;
}

.distance-button:hover {
  border-color: #ff5531;
  background-color: rgba(255, 85, 49, 0.05);
}

.distance-button.active {
  background-color: #ff5531;
  border-color: #ff5531;
  color: white;
}

.checkbox-wrapper {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.checkbox-input {
  width: 20px;
  height: 20px;
  cursor: pointer;
  accent-color: #ff5531;
}

.checkbox-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  color: #202124;
  cursor: pointer;
}

.food-type-buttons {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
}

.food-type-button {
  padding: 10px 20px;
  border: 2px solid #e8eaed;
  border-radius: 24px;
  background-color: white;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #5f6368;
  cursor: pointer;
  transition: all 0.2s;
  white-space: nowrap;
}

.food-type-button:hover {
  border-color: #ff5531;
  background-color: rgba(255, 85, 49, 0.05);
}

.food-type-button.active {
  background-color: #ff5531;
  border-color: #ff5531;
  color: white;
}

.recommend-button {
  width: 100%;
  padding: 16px;
  border: none;
  border-radius: 12px;
  background-color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: white;
  cursor: pointer;
  transition: background-color 0.2s;
  margin-bottom: 16px;
}

.recommend-button:hover {
  background-color: #e6442a;
}

.recommend-button:active {
  background-color: #cc3a24;
}

.recommend-button:disabled {
  cursor: not-allowed;
  opacity: 0.7;
}

.recommend-button.loading {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.loading-spinner {
  width: 18px;
  height: 18px;
  border: 2px solid rgba(255, 255, 255, 0.3);
  border-top-color: white;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.help-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 14px;
  color: #9aa0a6;
  text-align: center;
  margin: 0;
}
</style>
