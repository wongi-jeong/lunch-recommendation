<template>
  <div class="bottom-seat">
    <div class="bottom-seat-panel">
      <!-- 상단 버튼 영역 (결과 있을 때) -->
      <div v-if="hasResults" class="bottom-seat-actions">
        <button type="button" class="btn-primary" @click="$emit('vote-create')">
          투표 만들기
        </button>
        <button type="button" class="btn-roulette" @click="$emit('roulette')">
          룰렛 돌리기
        </button>
      </div>

      <!-- 빈 상태 -->
      <div v-if="!hasResults" class="bottom-seat-content empty-state">
        <p class="bottom-seat-text">필터를 이용해 식당을 추천 받아보세요!</p>
      </div>

      <!-- 추천 카드 목록 (Figma recommendationCards 레이아웃) -->
      <div
        v-else
        ref="cardsScrollRef"
        class="bottom-seat-content recommendation-cards"
        :class="{ 'is-dragging': isDragging }"
        @mousedown="onCardsMouseDown"
      >
        <div class="recommendation-cards-inner">
          <article
            v-for="(restaurant, index) in restaurants"
            :key="(restaurant.googlePlaceId || restaurant.placeId || restaurant.id) || `restaurant-${index}`"
            class="recommendation-card"
            :class="{
              'is-refreshing': refreshingIndex === index,
              'is-swapped': swappedIndex === index,
              'is-selected': selectedIndex === index
            }"
            @click="handleCardClick(restaurant)"
          >
            <!-- shimmer 오버레이 (로딩 중) -->
            <div v-if="refreshingIndex === index" class="card-shimmer-overlay" />

            <!-- 순번 배지 (이미지 좌상단) -->
            <div class="card-number-badge">{{ index + 1 }}</div>

            <!-- 음식 이미지 영역 -->
            <div class="card-image-wrap">
              <img
                :src="getRestaurantImage(restaurant)"
                :alt="restaurant.name"
                class="card-image"
                @error="handleImageError"
              />
              <!-- 찜하기 (이미지 하단 오버레이) -->
              <button
                type="button"
                class="card-favorite"
                :class="{ active: isFavorite(restaurant) }"
                @click.stop="emitToggleFavorite(restaurant)"
                aria-label="찜하기"
              >
                <svg
                  class="card-favorite-icon"
                  viewBox="0 0 24 24"
                  aria-hidden="true"
                >
                  <path
                    class="card-favorite-icon-path"
                    d="M12 20.25c-.32 0-.64-.1-.9-.3-.76-.56-1.45-1.09-2.08-1.56-2.53-1.92-4.42-3.36-4.42-5.89C4.6 9.5 6.1 8 7.92 8c1.12 0 2.12.52 2.78 1.39L12 10.9l1.3-1.51C13.96 8.52 14.96 8 16.08 8 17.9 8 19.4 9.5 19.4 12.5c0 2.53-1.89 3.97-4.42 5.89-.63.47-1.32 1-2.08 1.56-.26.2-.58.3-.9.3Z"
                  />
                </svg>
              </button>
            </div>

            <!-- 식당 정보 영역 -->
            <div class="card-info">
              <h3 class="card-name">{{ restaurant.name }}</h3>
              <div class="card-categories">
                <span
                  v-for="(cat, i) in getDisplayCategories(restaurant)"
                  :key="'cat-' + i"
                  class="card-category"
                  :class="{ 'card-category--primary': isActiveCategory(cat) }"
                >
                  {{ cat }}
                  <span v-if="i < getDisplayCategories(restaurant).length - 1" class="card-category-separator">|</span>
                </span>
              </div>
              <div class="card-business-status">
                {{ getBusinessStatus(restaurant) }}
              </div>
              <div class="card-rating">
                <img src="@/assets/icon-star.svg" alt="별점" class="card-star-icon" />
                <span class="card-rating-value">{{ restaurant.rating ? restaurant.rating.toFixed(1) : '0.0' }}</span>
              </div>
              <div v-if="restaurant.distanceMeters" class="card-distance">
                현 위치에서 {{ formatDistance(restaurant.distanceMeters) }}
              </div>
            </div>

            <!-- 액션 아이콘 (카드 우측 상단) -->
            <div class="card-actions">
              <button
                type="button"
                class="card-action-btn"
                :class="{ 'is-spinning': refreshingIndex === index }"
                @click.stop="refreshRestaurant(restaurant, index)"
              >
                <img src="@/assets/icon-refresh.svg" alt="새로고침" class="refresh-icon" />
              </button>
              <button type="button" class="card-action-btn" @click.stop="openExternalLink(restaurant)">
                <img src="@/assets/icon-external-link.svg" alt="외부 링크" />
              </button>
            </div>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick, computed } from 'vue'
import defaultThumbnail from '@/assets/img-placeholder-restaurant.png'

const props = defineProps({
  hasResults: {
    type: Boolean,
    default: false
  },
  restaurants: {
    type: Array,
    default: () => []
  },
  /**
   * 현재 선택된 필터 기준 카테고리 리스트 (예: ["한식", "고기"])
   * 없으면 단순히 식당 카테고리만 보여준다.
   */
  activeCategories: {
    type: Array,
    default: () => []
  },
  favoriteIds: {
    type: Array,
    default: () => []
  },
  /** 눌린(선택된) 카드 인덱스. -1이면 none */
  selectedIndex: {
    type: Number,
    default: -1
  }
})

const emit = defineEmits([
  'recommend',
  'select',
  'roulette',
  'refresh',
  'vote-create',
  'toggle-favorite'
])

const favoriteIdSet = computed(() => new Set(props.favoriteIds ?? []))
const refreshingIndex = ref(-1)
const swappedIndex = ref(-1)
// deep watch에서는 newVal/oldVal이 동일 참조이므로, 새로고침 시작 시점의 식별자를 저장해 비교
const refreshingOldId = ref(null)

// 가로 스크롤 드래그 (마우스 클릭 후 드래그)
const cardsScrollRef = ref(null)
const isDragging = ref(false)
const dragStartX = ref(0)
const scrollStartLeft = ref(0)
const hasDragged = ref(false)
const DRAG_THRESHOLD = 5

function onCardsMouseDown(e) {
  if (e.button !== 0) return
  const el = cardsScrollRef.value
  if (!el) return
  dragStartX.value = e.clientX
  scrollStartLeft.value = el.scrollLeft
  hasDragged.value = false
  document.addEventListener('mousemove', onCardsMouseMove)
  document.addEventListener('mouseup', onCardsMouseUp)
}

function onCardsMouseMove(e) {
  const el = cardsScrollRef.value
  if (!el) return
  const dx = e.clientX - dragStartX.value
  if (!isDragging.value && Math.abs(dx) > DRAG_THRESHOLD) {
    isDragging.value = true
    hasDragged.value = true
  }
  if (isDragging.value) {
    el.scrollLeft = scrollStartLeft.value + (dragStartX.value - e.clientX)
    e.preventDefault()
  }
}

function onCardsMouseUp() {
  document.removeEventListener('mousemove', onCardsMouseMove)
  document.removeEventListener('mouseup', onCardsMouseUp)
  isDragging.value = false
}

function handleCardClick(restaurant) {
  if (hasDragged.value) return
  emit('select', restaurant)
}

watch(() => props.restaurants, (restaurants) => {
  if (refreshingIndex.value < 0 || refreshingOldId.value == null) return
  const idx = refreshingIndex.value
  if (!restaurants || idx >= restaurants.length) return

  const current = restaurants[idx]
  const currentId = current?.googlePlaceId ?? current?.name
  if (currentId && currentId !== refreshingOldId.value) {
    refreshingIndex.value = -1
    refreshingOldId.value = null
    swappedIndex.value = idx
    nextTick(() => {
      setTimeout(() => { swappedIndex.value = -1 }, 500)
    })
  }
}, { deep: true })

const getRestaurantImage = (restaurant) => {
  if (restaurant.photoName && Array.isArray(restaurant.photoName) && restaurant.photoName.length > 0) {
    return `/api/restaurants/photo?name=${encodeURIComponent(restaurant.photoName[0])}`
  }
  return defaultThumbnail
}

const handleImageError = (event) => {
  event.target.src = defaultThumbnail
}

const emitToggleFavorite = (restaurant) => {
  emit('toggle-favorite', restaurant)
}

const isFavorite = (restaurant) => {
  const id = restaurant.googlePlaceId ?? restaurant.id ?? restaurant.name
  if (!id) return false
  return favoriteIdSet.value.has(id)
}

const refreshRestaurant = (restaurant, index) => {
  if (refreshingIndex.value >= 0) return
  refreshingIndex.value = index
  refreshingOldId.value = restaurant?.googlePlaceId ?? restaurant?.name ?? null
  emit('refresh', index)
}

const resetRefreshing = () => {
  refreshingIndex.value = -1
  refreshingOldId.value = null
}

const startRefreshing = (restaurant, index) => {
  if (refreshingIndex.value >= 0) return
  refreshingIndex.value = index
  refreshingOldId.value = restaurant?.googlePlaceId ?? restaurant?.name ?? null
}

defineExpose({ resetRefreshing, startRefreshing })

const openExternalLink = (restaurant) => {
  if (restaurant.googleMapsUri) {
    window.open(restaurant.googleMapsUri, '_blank')
  }
}

const PLACE_TYPE_TO_CATEGORY = {
  korean_restaurant: '한식',
  barbecue_restaurant: '고기',
  buffet_restaurant: '한식',
  japanese_restaurant: '일식',
  ramen_restaurant: '일식',
  sushi_restaurant: '일식',
  chinese_restaurant: '중식',
  italian_restaurant: '양식',
  french_restaurant: '양식',
  american_restaurant: '양식',
  spanish_restaurant: '양식',
  greek_restaurant: '양식',
  steak_house: '고기',
  cafe: '카페',
  coffee_shop: '카페',
  vietnamese_restaurant: '아시안',
  thai_restaurant: '아시안',
  indian_restaurant: '아시안',
  indonesian_restaurant: '아시안',
  mediterranean_restaurant: '아시안',
  turkish_restaurant: '아시안',
  middle_eastern_restaurant: '아시안',
  lebanese_restaurant: '아시안',
  brazilian_restaurant: '아시안',
  afghani_restaurant: '아시안',
  african_restaurant: '아시안',
  fast_food_restaurant: '패스트푸드',
  hamburger_restaurant: '패스트푸드',
  sandwich_shop: '패스트푸드',
  deli: '패스트푸드',
  cafeteria: '패스트푸드',
  bagel_shop: '패스트푸드',
  bar_and_grill: '고기',
  noodle_restaurant: '면/국물',
  vegan_restaurant: '비건',
  vegetarian_restaurant: '비건',
  salad_bar: '비건',
}

const getCategories = (restaurant) => {
  const typesJson = restaurant.placeTypesJson
  if (!typesJson) return ['식당']

  try {
    const types = typeof typesJson === 'string' ? JSON.parse(typesJson) : typesJson
    if (!Array.isArray(types)) return ['식당']

    const categories = []
    const seen = new Set()
    for (const type of types) {
      const cat = PLACE_TYPE_TO_CATEGORY[type]
      if (cat && !seen.has(cat)) {
        seen.add(cat)
        categories.push(cat)
      }
    }
    return categories.length > 0 ? categories : ['식당']
  } catch {
    return ['식당']
  }
}

/**
 * 카드에 보여줄 카테고리 순서를 결정한다.
 * - 현재 선택된 필터(activeCategories)에 포함된 카테고리를 먼저 배치
 * - 나머지 카테고리는 그 뒤에 그대로 붙인다.
 */
const getDisplayCategories = (restaurant) => {
  const baseCategories = getCategories(restaurant)
  if (!props.activeCategories || props.activeCategories.length === 0) {
    return baseCategories
  }

  const activeSet = new Set(props.activeCategories)
  const primary = []
  const secondary = []

  for (const cat of baseCategories) {
    if (activeSet.has(cat)) {
      primary.push(cat)
    } else {
      secondary.push(cat)
    }
  }

  return [...primary, ...secondary]
}

const isActiveCategory = (cat) => {
  return props.activeCategories?.includes(cat)
}

const getBusinessStatus = (restaurant) => {
  if (restaurant.openNow === true) return '영업중'
  if (restaurant.openNow === false) return '영업 종료'
  return '정보 없음'
}

const formatDistance = (meters) => {
  if (meters < 1000) return `${meters}m`
  return `${(meters / 1000).toFixed(1)}km`
}
</script>

<style scoped src="./BottomSeat.css"></style>
