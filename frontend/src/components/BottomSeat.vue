<template>
  <div class="bottom-seat">
    <div v-if="!hasResults" class="bottom-seat-content empty-state">
      <p class="bottom-seat-text">필터를 이용해 식당을 추천 받아보세요!</p>
    </div>
    <div v-else class="bottom-seat-content restaurant-list">
      <div class="restaurant-list-container">
        <div
          v-for="(restaurant, index) in restaurants"
          :key="index"
          class="restaurant-card"
        >
          <!-- 순번 배지 -->
          <div class="restaurant-number-badge">{{ index + 1 }}</div>
          
          <!-- 음식 이미지 -->
          <div class="restaurant-image-container">
            <img
              :src="getRestaurantImage(restaurant)"
              :alt="restaurant.name"
              class="restaurant-image"
              @error="handleImageError"
            />
            <!-- 찜하기 아이콘 -->
            <button class="favorite-button" @click="toggleFavorite(restaurant)">
              <img src="@/assets/heart-icon.svg" alt="찜하기" class="heart-icon" />
            </button>
          </div>
          
          <!-- 식당 정보 -->
          <div class="restaurant-info">
            <div class="restaurant-header">
              <h3 class="restaurant-name">{{ restaurant.name }}</h3>
              <div class="restaurant-actions">
                <button class="action-button" @click="refreshRestaurant(restaurant)">
                  <img src="@/assets/refresh-icon.svg" alt="새로고침" class="action-icon" />
                </button>
                <button class="action-button" @click="openExternalLink(restaurant)">
                  <img src="@/assets/external-link-icon.svg" alt="외부 링크" class="action-icon" />
                </button>
              </div>
            </div>
            
            <div class="restaurant-category">
              {{ getCategoryLabel(restaurant) }} {{ getBusinessStatus(restaurant) }}
            </div>
            
            <div class="restaurant-details">
              <div v-if="restaurant.rating" class="restaurant-rating">
                <img src="@/assets/star-icon.svg" alt="별점" class="star-icon" />
                <span class="rating-value">{{ restaurant.rating.toFixed(1) }}</span>
              </div>
              <div v-if="restaurant.distanceMeters" class="restaurant-distance">
                현 위치에서 {{ formatDistance(restaurant.distanceMeters) }}
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import defaultThumbnail from '@/assets/restaurnt_thumbnail_default_image.png'

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

const favorites = ref(new Set())

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
  const restaurantId = restaurant.id || restaurant.name
  if (favorites.value.has(restaurantId)) {
    favorites.value.delete(restaurantId)
  } else {
    favorites.value.add(restaurantId)
  }
}

const refreshRestaurant = (restaurant) => {
  // 새로고침 로직 (필요시 구현)
  console.log('새로고침:', restaurant.name)
}

const openExternalLink = (restaurant) => {
  if (restaurant.googleMapsUri) {
    window.open(restaurant.googleMapsUri, '_blank')
  }
}

const getCategoryLabel = (restaurant) => {
  // 카테고리 정보가 있다면 사용, 없으면 기본값
  if (restaurant.categories && Array.isArray(restaurant.categories) && restaurant.categories.length > 0) {
    return restaurant.categories[0]
  }
  return '식당'
}

const getBusinessStatus = (restaurant) => {
  // 영업 상태 정보가 있다면 사용
  if (restaurant.businessStatus) {
    return restaurant.businessStatus
  }
  // 기본값으로 영업중 표시
  return '영업중'
}

const formatDistance = (meters) => {
  if (meters < 1000) {
    return `${meters}m`
  }
  return `${(meters / 1000).toFixed(1)}km`
}
</script>

<style scoped>
.bottom-seat {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 320px;
  z-index: 1000;
  pointer-events: auto;
}

.bottom-seat-content {
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  padding: 24px 32px;
  backdrop-filter: blur(10px);
}

.bottom-seat-content.empty-state {
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.bottom-seat-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 26px;
  color: #202124;
  margin: 0;
  line-height: 1.35;
  letter-spacing: 0;
}

.restaurant-list {
  padding: 24px 32px;
  overflow-x: auto;
  overflow-y: hidden;
}

.restaurant-list-container {
  display: flex;
  gap: 20px;
  height: 100%;
  padding-bottom: 8px;
}

.restaurant-card {
  position: relative;
  min-width: 280px;
  width: 280px;
  height: 100%;
  background-color: white;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
  display: flex;
  flex-direction: column;
  cursor: pointer;
  transition: transform 0.2s, box-shadow 0.2s;
}

.restaurant-card:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.restaurant-number-badge {
  position: absolute;
  top: 12px;
  left: 12px;
  width: 40px;
  height: 40px;
  background-color: #FF5531;
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: white;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.2);
}

.restaurant-image-container {
  position: relative;
  width: 100%;
  height: 180px;
  overflow: hidden;
  background-color: #f5f5f5;
}

.restaurant-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.favorite-button {
  position: absolute;
  top: 12px;
  right: 12px;
  width: 36px;
  height: 36px;
  background-color: rgba(255, 255, 255, 0.9);
  border-radius: 50%;
  border: none;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 10;
  transition: background-color 0.2s;
  box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
}

.favorite-button:hover {
  background-color: white;
}

.heart-icon {
  width: 20px;
  height: 20px;
}

.restaurant-info {
  flex: 1;
  padding: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.restaurant-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 8px;
}

.restaurant-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  color: #202124;
  margin: 0;
  flex: 1;
  line-height: 1.3;
}

.restaurant-actions {
  display: flex;
  gap: 4px;
  flex-shrink: 0;
}

.action-button {
  width: 28px;
  height: 28px;
  background-color: transparent;
  border: none;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: background-color 0.2s;
  padding: 0;
}

.action-button:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.action-icon {
  width: 16px;
  height: 16px;
}

.restaurant-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  color: #5F6368;
}

.restaurant-details {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-top: auto;
}

.restaurant-rating {
  display: flex;
  align-items: center;
  gap: 4px;
}

.star-icon {
  width: 16px;
  height: 16px;
}

.rating-value {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  color: #202124;
}

.restaurant-distance {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 13px;
  color: #5F6368;
}

/* 스크롤바 스타일링 */
.restaurant-list::-webkit-scrollbar {
  height: 8px;
}

.restaurant-list::-webkit-scrollbar-track {
  background: transparent;
}

.restaurant-list::-webkit-scrollbar-thumb {
  background: #e8eaed;
  border-radius: 4px;
}

.restaurant-list::-webkit-scrollbar-thumb:hover {
  background: #dadce0;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .bottom-seat {
    height: 280px;
  }

  .bottom-seat-content {
    padding: 16px 20px;
  }

  .restaurant-card {
    min-width: 240px;
    width: 240px;
  }

  .restaurant-image-container {
    height: 150px;
  }

  .bottom-seat-text {
    font-size: 20px;
  }
}
</style>
