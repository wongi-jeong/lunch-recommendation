<template>
  <div class="bottom-seat">
    <div class="bottom-seat-panel">
      <!-- 상단 버튼 영역 (결과 있을 때) -->
      <div v-if="hasResults" class="bottom-seat-actions">
        <button type="button" class="btn-primary" @click="$emit('recommend')">
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
      <div v-else class="bottom-seat-content recommendation-cards">
        <div class="recommendation-cards-inner">
          <article
            v-for="(restaurant, index) in restaurants"
            :key="restaurant.id ?? index"
            class="recommendation-card"
            :class="{
              'is-refreshing': refreshingIndex === index,
              'is-swapped': swappedIndex === index
            }"
            @click="$emit('select', restaurant)"
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
                :class="{ active: favorites.has(restaurant.id ?? restaurant.name) }"
                @click.stop="toggleFavorite(restaurant)"
              >
                <img src="@/assets/heart-icon.svg" alt="찜하기" class="card-favorite-icon" />
              </button>
            </div>

            <!-- 식당 정보 영역 -->
            <div class="card-info">
              <h3 class="card-name">{{ restaurant.name }}</h3>
              <div class="card-categories">
                <span
                  v-for="(cat, i) in getCategories(restaurant)"
                  :key="'cat-' + i"
                  class="card-category"
                >{{ cat }}</span>
                <span class="card-category">{{ getBusinessStatus(restaurant) }}</span>
              </div>
              <div class="card-rating">
                <img src="@/assets/star-icon.svg" alt="별점" class="card-star-icon" />
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
                <img src="@/assets/refresh-icon.svg" alt="새로고침" class="refresh-icon" />
              </button>
              <button type="button" class="card-action-btn" @click.stop="openExternalLink(restaurant)">
                <img src="@/assets/external-link-icon.svg" alt="외부 링크" />
              </button>
            </div>
          </article>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, watch, nextTick } from 'vue'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'

const props = defineProps({
  hasResults: {
    type: Boolean,
    default: false
  },
  restaurants: {
    type: Array,
    default: () => []
  }
})

const emit = defineEmits(['recommend', 'select', 'roulette', 'refresh'])

const favorites = ref(new Set())
const refreshingIndex = ref(-1)
const swappedIndex = ref(-1)
// deep watch에서는 newVal/oldVal이 동일 참조이므로, 새로고침 시작 시점의 식별자를 저장해 비교
const refreshingOldId = ref(null)

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

const toggleFavorite = (restaurant) => {
  const id = restaurant.id ?? restaurant.name
  if (favorites.value.has(id)) {
    favorites.value.delete(id)
  } else {
    favorites.value.add(id)
  }
  favorites.value = new Set(favorites.value)
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

defineExpose({ resetRefreshing })

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

<style scoped>
/* 하단 패널: 뷰포트 비율로 고정해 스크롤 없이 화면에 맞춤 */
.bottom-seat {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  z-index: 1000;
  pointer-events: auto;
  max-height: 32vh;
}

.bottom-seat-panel {
  width: 100%;
  height: 100%;
  max-height: 32vh;
  min-height: 180px;
  background: rgba(255, 255, 255, 0.5);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.08);
  backdrop-filter: blur(10px);
  padding: 12px 0 0;
  display: flex;
  flex-direction: column;
}

/* 상단 버튼 영역 (Figma y=808) */
.bottom-seat-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 0 24px 12px;
  flex-shrink: 0;
}

.btn-roulette {
  min-width: 100px;
  height: 48px;
  padding: 0 16px;
  border: none;
  border-radius: 14px;
  background: #ffffff;
  color: #000000;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-roulette:hover {
  background: #f0f0f0;
}

.btn-primary {
  min-width: 110px;
  height: 48px;
  padding: 0 20px;
  border: none;
  border-radius: 14px;
  background: #fff0ea;
  color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.btn-primary:hover {
  background: #ffe4dc;
}

.btn-secondary {
  min-width: 109px;
  height: 47px;
  padding: 0 20px;
  border: 1px solid #dadce0;
  border-radius: 14px;
  background: #fff;
  color: #3c4043;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 15px;
  cursor: pointer;
  transition: background 0.2s, border-color 0.2s;
}

.btn-secondary:hover {
  background: #f8f9fa;
  border-color: #bdc1c6;
}

/* 빈 상태 */
.bottom-seat-content {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px 32px;
}

.bottom-seat-content.empty-state {
  min-height: 100px;
  text-align: center;
}

.bottom-seat-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 26px;
  color: #202124;
  margin: 0;
  line-height: 1.35;
}

/* 추천 카드 영역: 최대 3개 카드 노출, 나머지는 가로 스크롤 */
.recommendation-cards {
  padding: 0 0 24px;
  overflow-x: auto;
  overflow-y: hidden;
  flex: 1;
  min-height: 0;
  max-width: calc(3 * 383px + 2 * 9px); /* 카드 3개 + 간격 2 */
  margin: 0 auto;
  justify-content: flex-start; /* 부모의 center 덮어쓰기 - 첫 카드가 보이도록 */
  align-items: stretch;
  -webkit-overflow-scrolling: touch;
  scroll-behavior: smooth;
  overscroll-behavior-x: contain;
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.2) rgba(0, 0, 0, 0.06);
}

.recommendation-cards::-webkit-scrollbar {
  height: 8px;
}

.recommendation-cards::-webkit-scrollbar-track {
  background: rgba(0, 0, 0, 0.06);
  border-radius: 4px;
}

.recommendation-cards::-webkit-scrollbar-thumb {
  background: rgba(0, 0, 0, 0.2);
  border-radius: 4px;
}

.recommendation-cards::-webkit-scrollbar-thumb:hover {
  background: rgba(0, 0, 0, 0.35);
}

.recommendation-cards-inner {
  display: flex;
  gap: 9px;
  padding: 0 24px 8px;
  height: 100%;
  width: max-content;
  box-sizing: border-box;
}

/* 단일 카드 (Figma: 383×167, rx 23.5, stroke #DADCE0) */
.recommendation-card {
  position: relative;
  flex-shrink: 0;
  width: 383px;
  min-width: 383px;
  height: 167px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 23.5px;
  overflow: hidden;
  display: flex;
  align-items: stretch;
  cursor: pointer;
  transition: box-shadow 0.2s;
}

.recommendation-card:hover {
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
}

/* 순번 배지 (Figma: 32×32, rx 6, #3C4043, 이미지 좌상단) */
.card-number-badge {
  position: absolute;
  top: 24px;
  left: 24px;
  width: 32px;
  height: 32px;
  border-radius: 6px;
  background: #3c4043;
  color: #fff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2;
}

/* 이미지 영역 (Figma: 120×120, rx 6, 패딩 24px) */
.card-image-wrap {
  position: relative;
  flex-shrink: 0;
  width: 120px;
  height: 120px;
  margin: 24px 0 0 24px;
  border-radius: 6px;
  overflow: hidden;
  background: #f5f5f5;
}

.card-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

/* 찜하기 버튼 (이미지 하단 오버레이, Figma circle r=21 black 60%) */
.card-favorite {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 42px;
  height: 42px;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
  transition: background 0.2s;
}

.card-favorite:hover {
  background: rgba(0, 0, 0, 0.75);
}

.card-favorite.active {
  background: rgba(255, 85, 49, 0.9);
}

.card-favorite-icon {
  width: 24px;
  height: 24px;
  filter: brightness(0) invert(1);
}

.card-favorite.active .card-favorite-icon {
  filter: brightness(0) saturate(100%) invert(43%) sepia(96%) saturate(1690%) hue-rotate(345deg) brightness(101%) contrast(101%);
}

/* 식당 정보 영역 */
.card-info {
  flex: 1;
  min-width: 0;
  padding: 24px 8px 24px 16px;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.card-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  color: #3c4043;
  margin: 0;
  line-height: 1.3;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  line-clamp: 2;
  -webkit-box-orient: vertical;
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 8px;
  flex-shrink: 0;
  padding: 20px 16px 0 0;
}

.card-action-btn {
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  transition: background 0.2s;
}

.card-action-btn:hover {
  background: rgba(0, 0, 0, 0.06);
}

.card-action-btn img {
  width: 24px;
  height: 24px;
  object-fit: contain;
}

.card-categories {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
  align-items: center;
}

.card-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 13px;
  color: #3c4043;
  line-height: 1.35;
}

.card-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.card-star-icon {
  width: 16px;
  height: 16px;
  object-fit: contain;
}

.card-rating-value {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  color: #3c4043;
}

.card-distance {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 13px;
  color: #3c4043;
}

/* ── 새로고침 애니메이션 ── */

/* 1) 리프레시 아이콘 스핀 */
.card-action-btn.is-spinning .refresh-icon {
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  from { transform: rotate(0deg); }
  to   { transform: rotate(360deg); }
}

/* 2) 로딩 중 카드: 컨텐츠 살짝 desaturate + shimmer 오버레이 */
.recommendation-card.is-refreshing {
  pointer-events: none;
}

.recommendation-card.is-refreshing .card-image-wrap,
.recommendation-card.is-refreshing .card-info {
  opacity: 0.45;
  filter: grayscale(0.4);
  transition: opacity 0.3s, filter 0.3s;
}

.card-shimmer-overlay {
  position: absolute;
  inset: 0;
  z-index: 3;
  border-radius: inherit;
  overflow: hidden;
  background: linear-gradient(
    90deg,
    transparent 0%,
    rgba(255, 255, 255, 0.55) 50%,
    transparent 100%
  );
  background-size: 200% 100%;
  animation: shimmer 1.4s ease-in-out infinite;
}

@keyframes shimmer {
  0%   { background-position: 200% 0; }
  100% { background-position: -200% 0; }
}

/* 3) 새 카드 등장: 슬라이드-인 + 페이드 + 약한 스케일 */
.recommendation-card.is-swapped {
  animation: cardSwapIn 0.45s cubic-bezier(0.22, 1, 0.36, 1) both;
}

@keyframes cardSwapIn {
  0% {
    opacity: 0;
    transform: translateY(18px) scale(0.96);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

/* 반응형: 1920 레이아웃 기준, 작은 화면에서 카드 축소 */
@media (max-width: 768px) {
  .bottom-seat-panel {
    min-height: 160px;
    max-height: 32vh;
  }

  .bottom-seat-actions {
    padding: 0 16px 10px;
  }

  .btn-primary,
  .btn-secondary {
    min-width: 90px;
    height: 44px;
    font-size: 14px;
  }

  .recommendation-cards {
    max-width: calc(3 * 320px + 2 * 9px);
  }

  .recommendation-card {
    width: 320px;
    min-width: 320px;
    height: 150px;
  }

  .card-image-wrap {
    width: 100px;
    height: 100px;
    margin: 20px 0 0 20px;
  }

  .card-number-badge {
    top: 20px;
    left: 20px;
    width: 28px;
    height: 28px;
    font-size: 14px;
  }

  .card-info {
    padding: 20px 16px 20px 12px;
  }

  .card-name {
    font-size: 14px;
  }

  .bottom-seat-text {
    font-size: 20px;
  }
}
</style>
