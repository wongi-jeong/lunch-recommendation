<script setup>
import { ref, onMounted, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useFavorites } from '@/composables/useFavorites'
import GoogleMap from '../GoogleMap.vue'
import FilterPanel from './FilterPanel.vue'
import BottomSeat from '../BottomSeat.vue'
import LunchRoulettePopup from './LunchRoulettePopup.vue'
import LoginRequiredModal from '../LoginRequiredModal.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()
const { favorites, addFavorite, removeFavorite } = useFavorites()

const apiKey = ref('')
const filters = ref({
  distance: 300,
  foodTypes: ['korean'],
  openOnly: false
})

// 추천 요청 시 사용자 위치 (경로 표시용)
const lastUserLocation = ref(null)

const mapCenter = ref({ lat: 37.5045, lng: 127.0489 })

// 표시용 목록 (API에서 openOnly 파라미터로 이미 필터링된 결과가 옴)
const displayRestaurants = computed(() => restaurants.value)

// 지도에 표시할 마커 (displayRestaurants 기준)
const markers = computed(() => {
  const list = displayRestaurants.value
  const filterCategories = (filters.value.foodTypes || [])
    .map((id) => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)
  const filtered = []
  list.forEach((restaurant, i) => {
    if (restaurant.latitude && restaurant.longitude) {
      filtered.push({
        lat: restaurant.latitude,
        lng: restaurant.longitude,
        color: filtered.length === 0 ? '#34A853' : '#FF5531',
        name: restaurant.name,
        rating: restaurant.rating,
        address: restaurant.address,
        googleMapsUri: restaurant.googleMapsUri,
        distanceMeters: restaurant.distanceMeters,
        photoName: restaurant.photoName,
        categories: filterCategories.length > 0 ? filterCategories : ['식당'],
        openNow: restaurant.openNow,
        restaurantIndex: i
      })
    }
  })
  return filtered
})

// 지도에 표시할 경로 (현재 위치 → 첫 번째 표시 식당)
const routes = computed(() => {
  const list = displayRestaurants.value
  const origin = lastUserLocation.value
  if (!origin || list.length === 0 || !list[0].latitude || !list[0].longitude) return []
  return [
    {
      origin,
      destination: { lat: list[0].latitude, lng: list[0].longitude },
      color: '#34A853'
    }
  ]
})

// GoogleMap 컴포넌트 ref
const googleMapRef = ref(null)

// 로딩 상태
const isLoading = ref(false)

const bottomSeatRef = ref(null)

onMounted(() => {
  apiKey.value = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || import.meta.env.GOOGLE_MAPS_API_KEY || ''
  
  if (!apiKey.value) {
    console.warn('Google Maps API 키가 설정되지 않았습니다.')
    console.warn('환경변수 GOOGLE_MAPS_API_KEY 또는 .env 파일의 VITE_GOOGLE_MAPS_API_KEY를 설정해주세요.')
  }
})

// 검색된 식당 목록
const restaurants = ref([])

// 룰렛 팝업 표시
const rouletteOpen = ref(false)
// 로그인 필요 모달
const showLoginRequiredModal = ref(false)
const loginModalAction = ref(null) // 'vote-create' | 'favorite' | null

// 검색 결과 없음 / 안내 모달 (가운데 정렬, 디자인 통일)
const showNoResultsModal = ref(false)
const noResultsModalMessage = ref('')
const noResultsModalTitle = ref('')

const openNoResultsModal = (title, message) => {
  noResultsModalTitle.value = title
  noResultsModalMessage.value = message
  showNoResultsModal.value = true
}
const closeNoResultsModal = () => {
  showNoResultsModal.value = false
}

const MAX_RADIUS = 1200 // FilterPanel distanceOptions 최대값

const favoriteIds = computed(() => favorites.value.map((f) => f.id))

const normalizeRestaurantForFavorite = (item) => {
  if (!item) return null
  const id =
    item.googlePlaceId ||
    item.placeId ||
    item.id ||
    (typeof item.name === 'string' ? item.name : null)

  if (!id) return null

  return {
    id,
    name: item.name || '',
    address: item.address || '',
    googleMapsUri: item.googleMapsUri || '',
    latitude: item.latitude ?? item.lat ?? null,
    longitude: item.longitude ?? item.lng ?? null,
    rating: typeof item.rating === 'number' ? item.rating : null,
    distanceMeters: item.distanceMeters ?? null,
    photoName: Array.isArray(item.photoName) ? item.photoName[0] : item.photoName ?? null,
    categories: Array.isArray(item.categories) ? item.categories : [],
    favoritedAt: Date.now()
  }
}

const toggleFavoriteRestaurant = (rawItem) => {
  if (!isLoggedIn.value) {
    loginModalAction.value = 'favorite'
    showLoginRequiredModal.value = true
    return
  }

  const normalized = normalizeRestaurantForFavorite(rawItem)
  if (!normalized) return

  const exists = favorites.value.some((f) => f.id === normalized.id)
  if (exists) {
    removeFavorite(normalized.id)
  } else {
    addFavorite(normalized)
  }
}

// FilterPanel foodTypes id → 백엔드 categories(한글 라벨) 매핑
const FOOD_TYPE_TO_CATEGORY = {
  korean: '한식',
  chinese: '중식',
  japanese: '일식',
  western: '양식',
  vegan: '비건',
  asian: '아시안',
  fastfood: '패스트푸드',
  meat: '고기',
  noodle: '면/국물',
  cafe: '카페'
}

// Google Places placeTypes → 카테고리 (BottomSeat와 동일 매핑)
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
  salad_bar: '비건'
}

const getCategoriesFromApiTypes = (restaurant) => {
  const typesJson = restaurant?.placeTypesJson
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

// 추천 받기 버튼 클릭 핸들러
const handleRecommend = async () => {
  if (isLoading.value) return
  
  isLoading.value = true
  
  try {
    // GoogleMap 컴포넌트에서 현재 위치 가져오기
    if (!googleMapRef.value) {
      throw new Error('지도가 아직 로드되지 않았습니다.')
    }
    
    const userLocation = await googleMapRef.value.getCurrentUserLocation()
    
    // 선택된 거리(radius) 값 가져오기
    const radius = filters.value.distance // 300, 800, 1200 중 하나
    
    // 선택된 음식 종류 → filterCategories 리스트로 변환 (백엔드 한글 라벨)
    const filterCategories = (filters.value.foodTypes || [])
      .map(id => FOOD_TYPE_TO_CATEGORY[id])
      .filter(Boolean)

    if (!filterCategories.length) {
      isLoading.value = false
      return
    }
    
    console.log('=== 추천 요청 데이터 ===')
    console.log('사용자 위치 (위도):', userLocation.lat)
    console.log('사용자 위치 (경도):', userLocation.lng)
    console.log('검색 반경 (미터):', radius)
    console.log('필터 카테고리:', filterCategories)
    
    // 지도 중심 업데이트 및 경로용 위치 저장
    mapCenter.value = userLocation
    lastUserLocation.value = userLocation

    // 백엔드 API 호출 (categories, openOnly 전달)
    const params = new URLSearchParams({
      lat: String(userLocation.lat),
      lng: String(userLocation.lng),
      radius: String(radius)
    })
    filterCategories.forEach((cat) => params.append('categories', cat))
    if (filters.value.openOnly) {
      params.set('openOnly', 'true')
    }

    const response = await fetch(`/api/restaurants/nearby?${params.toString()}`)
    
    if (!response.ok) {
      throw new Error(`API 요청 실패: ${response.status} ${response.statusText}`)
    }
    
    const data = await response.json()
    restaurants.value = data

    // 추천 식당 정보 출력
    console.log('=== 추천 식당 리스트 (최대 5개) ===')
    if (data.length > 0) {
      console.log(`총 ${data.length}개의 식당이 추천되었습니다.`)
      data.forEach((restaurant, index) => {
        const rating = restaurant.rating ? `⭐ ${restaurant.rating}` : '평점 없음'
        const distanceInfo = restaurant.distanceMeters
          ? restaurant.distanceType === 'WALKING'
            ? `${restaurant.distanceMeters}m (도보 거리)`
            : `${restaurant.distanceMeters}m (직선 거리)`
          : '거리 정보 없음'
        const googleMapsLink = restaurant.googleMapsUri
          ? `🔗 ${restaurant.googleMapsUri}`
          : 'Google 지도 링크 없음'
        console.log(`${index + 1}. ${restaurant.name || '이름 없음'}`)
        console.log(`   - 평점: ${rating}`)
        console.log(`   - 주소: ${restaurant.address || '주소 없음'}`)
        console.log(`   - 거리: ${distanceInfo}`)
        console.log(`   - Google 지도: ${googleMapsLink}`)
      })
    } else {
      console.log('검색된 식당이 없습니다.')
    }

    if (data.length === 0) {
      const isMaxRadius = filters.value.distance === MAX_RADIUS
      openNoResultsModal(
        '검색 결과가 없습니다',
        isMaxRadius
          ? "주변에 해당 조건의 식당이 없습니다. 다른 음식 종류를 선택하거나, '영업중인 가게만 보기'를 해제해 보세요."
          : '주변에 검색된 식당이 없습니다. 검색 반경을 늘려보세요.'
      )
    }
  } catch (error) {
    console.error('추천 요청 실패:', error.message)
    openNoResultsModal('추천 요청 실패', error.message)
  } finally {
    isLoading.value = false
  }
}

// 카드 새로고침: 해당 인덱스의 식당을 현재 필터 조건에 맞는 새 식당으로 교체
// index는 표시 목록(displayRestaurants) 기준 인덱스
const handleRefresh = async (index) => {
  if (!googleMapRef.value) return

  const list = displayRestaurants.value
  const restaurant = list[index]
  if (!restaurant) return

  const realIndex = restaurants.value.findIndex((r) => r.googlePlaceId === restaurant.googlePlaceId)
  if (realIndex < 0) return

  bottomSeatRef.value?.startRefreshing?.(restaurant, index)

  try {
    const userLocation = await googleMapRef.value.getCurrentUserLocation()
    const radius = filters.value.distance
    const filterCategories = (filters.value.foodTypes || [])
      .map((id) => FOOD_TYPE_TO_CATEGORY[id])
      .filter(Boolean)

    if (!filterCategories.length) return

    const excludePlaceIds = restaurants.value
      .map((r) => r.googlePlaceId)
      .filter(Boolean)

    const params = new URLSearchParams({
      lat: String(userLocation.lat),
      lng: String(userLocation.lng),
      radius: String(radius),
      maxResults: '1'
    })
    filterCategories.forEach((cat) => params.append('categories', cat))
    excludePlaceIds.forEach((id) => params.append('excludePlaceIds', id))
    if (filters.value.openOnly) {
      params.set('openOnly', 'true')
    }

    const response = await fetch(`/api/restaurants/nearby?${params.toString()}`)
    if (!response.ok) throw new Error(`API 요청 실패: ${response.status}`)

    const data = await response.json()
    if (data.length > 0) {
      restaurants.value[realIndex] = data[0]
    } else {
      bottomSeatRef.value?.resetRefreshing()
      openNoResultsModal('추가 추천 없음', '추가로 추천할 수 있는 식당이 없습니다.')
    }
  } catch (error) {
    bottomSeatRef.value?.resetRefreshing()
    console.error('식당 새로고침 실패:', error.message)
    openNoResultsModal('새로고침 실패', '새로고침에 실패했습니다. ' + error.message)
  }
}

// 카드 클릭 시 해당 가게 위치로 지도 줌 + 인포윈도우 표시
const handleCardSelect = (restaurant) => {
  if (googleMapRef.value?.focusOnRestaurant && restaurant?.latitude && restaurant?.longitude) {
    googleMapRef.value.focusOnRestaurant(restaurant)
  }
}

const handleVoteCreate = () => {
  if (!displayRestaurants.value.length) return
  if (!isLoggedIn.value) {
    loginModalAction.value = 'vote-create'
    showLoginRequiredModal.value = true
    return
  }

  const filterCategories = (filters.value.foodTypes || [])
    .map(id => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)

  const data = displayRestaurants.value.map(r => ({
    name: r.name || '',
    googlePlaceId: r.googlePlaceId || '',
    address: r.address || '',
    photoName: r.photoName || null,
    rating: typeof r.rating === 'number' ? r.rating : null,
    openNow: r.openNow ?? null,
    categories: filterCategories.length > 0 ? filterCategories : ['식당']
  }))

  const encoded = encodeURIComponent(JSON.stringify(data))
  router.push({ name: 'voteCreate', query: { data: encoded } })
}

const closeLoginRequiredModal = () => {
  showLoginRequiredModal.value = false
  loginModalAction.value = null
}

/** 로그인 필요 모달에서 "로그인하기" 클릭: 추천 데이터를 저장한 뒤 로그인 페이지로 이동. 로그인 후 투표 생성 페이지로 복귀하기 위함. */
const goToLoginFromModal = () => {
  const action = loginModalAction.value
  loginModalAction.value = null

  if (action !== 'vote-create') {
    showLoginRequiredModal.value = false
    router.push('/login')
    return
  }

  if (!displayRestaurants.value.length) {
    showLoginRequiredModal.value = false
    router.push('/login')
    return
  }
  const filterCategories = (filters.value.foodTypes || [])
    .map(id => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)
  const data = displayRestaurants.value.map(r => ({
    name: r.name || '',
    googlePlaceId: r.googlePlaceId || '',
    address: r.address || '',
    photoName: r.photoName || null,
    rating: typeof r.rating === 'number' ? r.rating : null,
    openNow: r.openNow ?? null,
    categories: filterCategories.length > 0 ? filterCategories : ['식당']
  }))
  const encoded = encodeURIComponent(JSON.stringify(data))
  try {
    sessionStorage.setItem('pendingVoteCreateData', encoded)
  } catch (e) {
    console.warn('pendingVoteCreateData 저장 실패', e)
  }
  showLoginRequiredModal.value = false
  router.push({ path: '/login', query: { returnUrl: '/vote/create' } })
}

// 룰렛 결과 공유 페이지로 이동
const handleShare = (payload) => {
  const candidates = (payload?.candidates && payload.candidates.length > 0)
    ? payload.candidates
    : displayRestaurants.value.slice(0, 5)

  if (!candidates.length) return

  const winner = payload?.winner || candidates[0]

  const filterCategories = (filters.value.foodTypes || [])
    .map(id => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)

  const data = {
    winnerName: winner?.name || '',
    winnerPlaceId: winner?.googlePlaceId || '',
    restaurants: candidates.map((r) => ({
      name: r?.name || '',
      googlePlaceId: r?.googlePlaceId || '',
      address: r?.address || '',
      photoName: r?.photoName || null,
      rating: typeof r?.rating === 'number' ? r.rating : null,
      distanceMeters: r?.distanceMeters || null,
      openNow: r?.openNow ?? null,
      googleMapsUri: r?.googleMapsUri || '',
      // Roulette 공유 페이지에서는 실제 장소 타입 기반 카테고리 사용
      categories: getCategoriesFromApiTypes(r)
    }))
  }

  const encoded = encodeURIComponent(JSON.stringify(data))

  router.push({
    name: 'rouletteShare',
    query: { data: encoded }
  })
}
</script>

<template>
  <div class="nearby-recommendation">
    <div class="map-container">
      <GoogleMap
        v-if="apiKey"
        ref="googleMapRef"
        :api-key="apiKey"
        :center="mapCenter"
        :zoom="16"
        :markers="markers"
        :routes="routes"
        @toggle-favorite="toggleFavoriteRestaurant"
        @refresh="handleRefresh"
      />
      <div v-else class="map-error">
        <p>Google Maps API 키가 설정되지 않았습니다.</p>
        <p>환경변수 GOOGLE_MAPS_API_KEY를 설정해주세요.</p>
      </div>
      <BottomSeat
        ref="bottomSeatRef"
        :has-results="displayRestaurants.length > 0"
        :restaurants="displayRestaurants"
        :active-categories="(filters.foodTypes || []).map(id => FOOD_TYPE_TO_CATEGORY[id]).filter(Boolean)"
        :favorite-ids="favoriteIds"
        @recommend="handleRecommend"
        @select="handleCardSelect"
        @roulette="rouletteOpen = true"
        @refresh="handleRefresh"
        @toggle-favorite="toggleFavoriteRestaurant"
        @vote-create="handleVoteCreate"
      />
      <LunchRoulettePopup
        v-model="rouletteOpen"
        :restaurants="displayRestaurants"
        @share="handleShare"
      />
      <LoginRequiredModal
        :visible="showLoginRequiredModal"
        @close="closeLoginRequiredModal"
        @confirm="goToLoginFromModal"
      />
      <!-- 검색 결과 없음 / 안내 모달 -->
      <Transition name="popup-fade">
        <div
          v-if="showNoResultsModal"
          class="no-results-overlay"
          @click.self="closeNoResultsModal"
        >
          <div class="no-results-card">
            <div class="no-results-close-row">
              <button
                type="button"
                class="no-results-close-btn"
                aria-label="닫기"
                @click="closeNoResultsModal"
              >
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <path d="M15 5L5 15M5 5l10 10" stroke="#5F6368" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                </svg>
              </button>
            </div>
            <div class="no-results-content">
              <div class="no-results-icon">
                <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                  <circle cx="16" cy="16" r="16" fill="#FF5531" />
                  <path d="M12 12l8 8m0-8l-8 8" stroke="white" stroke-width="2" stroke-linecap="round" />
                </svg>
              </div>
              <p class="no-results-title">{{ noResultsModalTitle }}</p>
              <p class="no-results-description">{{ noResultsModalMessage }}</p>
            </div>
            <div class="no-results-action">
              <button type="button" class="no-results-primary-btn" @click="closeNoResultsModal">
                확인
              </button>
            </div>
          </div>
        </div>
      </Transition>
    </div>
   <FilterPanel
      v-model="filters"
      :loading="isLoading"
      @recommend="handleRecommend"
    /> 
  </div>
</template>

<style scoped>
.nearby-recommendation {
  position: relative;
  width: 100%;
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.map-container {
  flex: 1;
  min-height: 0;
  width: 100%;
  position: relative;
  display: flex;
  flex-direction: column;
}

.map-error {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  background-color: #f5f5f5;
  color: #5f6368;
  font-family: 'Pretendard', sans-serif;
}

.map-error p {
  margin: 8px 0;
  font-size: 16px;
}

/* 검색 결과 없음 / 안내 모달 (LoginRequiredModal과 동일한 디자인 톤) */
.no-results-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  pointer-events: all;
}

.no-results-card {
  background-color: #fff;
  border-radius: 48px;
  padding: 24px 32px 56px;
  width: 480px;
  max-width: 90vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.no-results-close-row {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.no-results-close-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.no-results-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.no-results-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.no-results-icon {
  width: 56px;
  height: 56px;
  border-radius: 28px;
  background-color: #fff5f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-results-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 22px;
  line-height: 1.35;
  color: #000;
  text-align: center;
  margin: 0;
}

.no-results-description {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 14px;
  line-height: 1.5;
  color: #5f6368;
  margin: 0;
  text-align: center;
  white-space: pre-line;
}

.no-results-action {
  width: 100%;
  box-sizing: border-box;
}

.no-results-primary-btn {
  width: 100%;
  height: 64px;
  border: none;
  border-radius: 20px;
  background-color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s;
}

.no-results-primary-btn:hover {
  background-color: #e6442a;
}

.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.popup-fade-enter-active .no-results-card,
.popup-fade-leave-active .no-results-card {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}

.popup-fade-enter-from .no-results-card,
.popup-fade-leave-to .no-results-card {
  transform: scale(0.92);
  opacity: 0;
}
</style>
