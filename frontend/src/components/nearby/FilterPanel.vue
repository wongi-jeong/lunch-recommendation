<script setup>
import { ref, computed, watch } from 'vue'
import { useAuth } from '@/composables/useAuth'
import SaveFilterPopup from './SaveFilterPopup.vue'
import iconCheckboxOutline from '@/assets/icon-checkbox-outline.svg'
import iconCheckboxChecked from '@/assets/icon-checkbox-checked.svg'

const { isLoggedIn, getToken } = useAuth()

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
/** 새 필터 버튼 클릭 시 내가 저장한 필터 목록 접기/펼치기 */
const showSavedFiltersDropdown = ref(false)
const savedFiltersList = ref([])
const savedFiltersLoading = ref(false)
/** 헤더에 표시할 필터 이름 (저장된 필터 선택 시 해당 이름으로 변경) */
const currentFilterLabel = ref('새 필터')

async function fetchSavedFiltersList() {
  const token = getToken()
  if (!token) {
    savedFiltersList.value = []
    return
  }
  savedFiltersLoading.value = true
  try {
    const res = await fetch('/api/saved-filters/me', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (res.ok) {
      const data = await res.json()
      savedFiltersList.value = Array.isArray(data) ? data : []
    } else {
      savedFiltersList.value = []
    }
  } catch {
    savedFiltersList.value = []
  } finally {
    savedFiltersLoading.value = false
  }
}

function toggleSavedFiltersDropdown() {
  showSavedFiltersDropdown.value = !showSavedFiltersDropdown.value
  if (showSavedFiltersDropdown.value && isLoggedIn.value) {
    fetchSavedFiltersList()
  }
}

function closeSavedFiltersDropdown() {
  showSavedFiltersDropdown.value = false
}

/** 기본 필터 적용 (저장된 필터 아님: 300m, 한식) */
function applyDefaultFilter() {
  currentFilterLabel.value = '새 필터'
  emit('update:modelValue', {
    distance: 300,
    foodTypes: ['korean'],
    openOnly: false
  })
  closeSavedFiltersDropdown()
}

function applySavedFilter(sf) {
  if (!sf) return
  currentFilterLabel.value = sf.name || '새 필터'
  emit('update:modelValue', {
    distance: sf.distance ?? 300,
    foodTypes: Array.isArray(sf.foodTypes) ? [...sf.foodTypes] : [],
    openOnly: !!sf.openOnly
  })
  closeSavedFiltersDropdown()
}

watch(showSavedFiltersDropdown, (open) => {
  if (!open) return
  if (isLoggedIn.value) fetchSavedFiltersList()
})

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

function toggleOpenOnly() {
  openOnly.value = !openOnly.value
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

// 내가 자주 사용하는 필터 저장 (로그인 사용자)
const showSaveFilterPopup = ref(false)
const saveFilterError = ref('')

function openSaveFilterPopup() {
  if (!isLoggedIn.value) return
  showSaveFilterPopup.value = true
  saveFilterError.value = ''
}

function closeSaveFilterPopup() {
  showSaveFilterPopup.value = false
  saveFilterError.value = ''
}

function onSaveFilter(name) {
  const token = getToken()
  if (!token) return
  saveFilterError.value = ''
  fetch('/api/saved-filters', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'X-Auth-Token': token
    },
    body: JSON.stringify({
      name: name.trim().substring(0, 12),
      distance: props.modelValue.distance ?? 300,
      foodTypes: props.modelValue.foodTypes ?? [],
      openOnly: !!props.modelValue.openOnly
    })
  })
    .then((res) => {
      if (!res.ok) return res.json().then((d) => { throw new Error(d.error || '저장에 실패했습니다.') })
      saveFilterError.value = ''
      closeSaveFilterPopup()
    })
    .catch((err) => {
      saveFilterError.value = err.message || '저장에 실패했습니다.'
    })
}
</script>

<template>
  <div class="filter-bar-wrapper">
    <div class="filter-panel" :class="{ collapsed: !isExpanded, 'dropdown-open': showSavedFiltersDropdown }">
      <!-- 필터 헤더: Figma 47:1684 -->
      <div class="filter-header" :class="{ collapsed: !isExpanded }">
        <h2 class="filter-title">필터</h2>
        <button
          type="button"
          class="filter-header__new-filter-btn"
          aria-haspopup="listbox"
          :aria-expanded="showSavedFiltersDropdown"
          aria-label="저장한 필터 목록 열기"
          @click="toggleSavedFiltersDropdown"
        >
          <span class="filter-header__new-filter-text">{{ currentFilterLabel }}</span>
          <span class="filter-header__dropdown-icon" aria-hidden="true">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path v-if="showSavedFiltersDropdown" d="M7 14L12 9L17 14" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <path v-else d="M7 10L12 15L17 10" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </span>
        </button>
        <button
          v-if="isLoggedIn"
          type="button"
          class="icon-button icon-button--star"
          aria-label="내가 자주 사용하는 필터로 저장"
          title="내가 자주 사용하는 필터 추가하기"
          @click.stop="openSaveFilterPopup"
        >
          <img src="@/assets/icon-star-outline.svg" alt="" width="24" height="24" aria-hidden="true" />
        </button>
        <button class="icon-button icon-button--expand" aria-label="필터 패널 접기/펼치기" @click="isExpanded = !isExpanded">
          <svg v-if="isExpanded" width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M7 14L12 9L17 14" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
          <svg v-else width="32" height="32" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M7 10L12 15L17 10" stroke="#5F6368" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
      </div>

      <!-- 헤더 아래 영역: 저장된 필터 목록 + filter-content (dim 없음 - 뒤 콘텐츠 그대로 노출) -->
      <div class="filter-panel__body">
        <!-- 내가 저장한 필터 목록: Figma 47:1105 -->
        <div v-if="showSavedFiltersDropdown" class="saved-filters-dropdown">
          <div class="saved-filters-dropdown__list">
            <button type="button" class="saved-filters-dropdown__row" @click="applyDefaultFilter">
              <span class="saved-filters-dropdown__row-name">새 필터</span>
            </button>
            <p v-if="savedFiltersLoading" class="saved-filters-dropdown__loading">불러오는 중...</p>
            <template v-else-if="savedFiltersList.length === 0">
              <p class="saved-filters-dropdown__empty">저장한 필터가 없어요</p>
            </template>
            <template v-else>
              <button
                v-for="sf in savedFiltersList"
                :key="sf.id"
                type="button"
                class="saved-filters-dropdown__row"
                @click="applySavedFilter(sf)"
              >
                <span class="saved-filters-dropdown__row-name">{{ sf.name }}</span>
              </button>
            </template>
            <button type="button" class="saved-filters-dropdown__close" @click="closeSavedFiltersDropdown">
              닫기
            </button>
          </div>
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
              <button type="button" class="filter-panel-checkbox" @click="toggleAllFoodTypes">
                <img
                  :src="allSelected ? iconCheckboxChecked : iconCheckboxOutline"
                  alt=""
                  class="filter-panel-checkbox__icon"
                  aria-hidden="true"
                />
                <span class="filter-panel-checkbox__label">전체 선택</span>
              </button>
              <button type="button" class="filter-panel-checkbox" @click="toggleOpenOnly">
                <img
                  :src="openOnly ? iconCheckboxChecked : iconCheckboxOutline"
                  alt=""
                  class="filter-panel-checkbox__icon"
                  aria-hidden="true"
                />
                <span class="filter-panel-checkbox__label">영업중인 가게만 보기</span>
              </button>
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

          <!-- 추천 받기 버튼 -->
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
    </div>

    <!-- 접힌 상태일 때만: 선택된 필터 칩 + 추천받기 버튼(패널 오른쪽에 표시) -->
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
          <svg width="18" height="18" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
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

  <SaveFilterPopup
    :show="showSaveFilterPopup"
    :error-message="saveFilterError"
    @save="onSaveFilter"
    @close="closeSaveFilterPopup"
  />
</template>

<style scoped src="./FilterPanel.css"></style>
