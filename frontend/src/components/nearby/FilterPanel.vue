<script setup>
import { ref, computed } from 'vue'

const props = defineProps({
  modelValue: {
    type: Object,
    default: () => ({
      distance: 300,
      foodTypes: [],
      openOnly: false
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
  { id: 'noodle', label: '면/국물' },
  { id: 'cafe', label: '카페' }
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

const openOnly = computed({
  get: () => !!props.modelValue.openOnly,
  set: (value) => {
    emit('update:modelValue', {
      ...props.modelValue,
      openOnly: value
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
        <div class="checkbox-row">
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
          <div class="checkbox-wrapper">
            <input
              type="checkbox"
              id="open-only"
              v-model="openOnly"
              class="checkbox-input"
            />
            <label for="open-only" class="checkbox-label">영업중인 가게만 보기</label>
          </div>
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

  <!-- 접힌 상태일 때만: 선택된 필터 칩 + 추천받기 버튼(칩 배열 우측에 붙임) -->
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
    <button
      class="recommend-button recommend-button--collapsed"
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
</template>

<style scoped src="./FilterPanel.css"></style>
