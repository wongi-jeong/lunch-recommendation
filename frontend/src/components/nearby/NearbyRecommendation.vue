<script setup>
import { ref, onMounted } from 'vue'
import { useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import GoogleMap from '../GoogleMap.vue'
import FilterPanel from './FilterPanel.vue'
import BottomSeat from '../BottomSeat.vue'
import LunchRoulettePopup from './LunchRoulettePopup.vue'
import LoginRequiredModal from '../LoginRequiredModal.vue'

const router = useRouter()
const { isLoggedIn } = useAuth()

const apiKey = ref('')
const filters = ref({
  distance: 300,
  foodTypes: ['korean'],
  openOnly: false
})

// 지도에 표시할 마커 (검색 후 업데이트됨)
const markers = ref([])

// 지도에 표시할 경로 (검색 후 업데이트됨)
const routes = ref([])

const mapCenter = ref({ lat: 37.5045, lng: 127.0489 })

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
    
    // 지도 중심 업데이트
    mapCenter.value = userLocation
    
    // 백엔드 API 호출 (categories 리스트 전달)
    const params = new URLSearchParams({
      lat: String(userLocation.lat),
      lng: String(userLocation.lng),
      radius: String(radius)
    })
    filterCategories.forEach(cat => params.append('categories', cat))
    
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
    
    // 마커 업데이트 - 검색된 식당들을 지도에 표시
    if (data.length > 0) {
      const filtered = []
      data.forEach((restaurant, i) => {
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
      markers.value = filtered
      
      // 경로 업데이트 - 현재 위치에서 첫 번째 식당까지의 경로
      if (data[0].latitude && data[0].longitude) {
        routes.value = [{
          origin: userLocation,
          destination: { 
            lat: data[0].latitude, 
            lng: data[0].longitude 
          },
          color: '#34A853'
        }]
      }
    } else {
      markers.value = []
      routes.value = []
      alert('주변에 검색된 식당이 없습니다. 검색 반경을 늘려보세요.')
    }
    
  } catch (error) {
    console.error('추천 요청 실패:', error.message)
    alert(error.message)
  } finally {
    isLoading.value = false
  }
}

// 카드 새로고침: 해당 인덱스의 식당을 현재 필터 조건에 맞는 새 식당으로 교체
const handleRefresh = async (index) => {
  if (!googleMapRef.value) return

  const restaurant = restaurants.value[index]
  bottomSeatRef.value?.startRefreshing?.(restaurant, index)

  try {
    const userLocation = await googleMapRef.value.getCurrentUserLocation()
    const radius = filters.value.distance
    const filterCategories = (filters.value.foodTypes || [])
      .map(id => FOOD_TYPE_TO_CATEGORY[id])
      .filter(Boolean)

    if (!filterCategories.length) return

    const excludePlaceIds = restaurants.value
      .map(r => r.googlePlaceId)
      .filter(Boolean)

    const params = new URLSearchParams({
      lat: String(userLocation.lat),
      lng: String(userLocation.lng),
      radius: String(radius),
      maxResults: '1'
    })
    filterCategories.forEach(cat => params.append('categories', cat))
    excludePlaceIds.forEach(id => params.append('excludePlaceIds', id))

    const response = await fetch(`/api/restaurants/nearby?${params.toString()}`)
    if (!response.ok) throw new Error(`API 요청 실패: ${response.status}`)

    const data = await response.json()
    if (data.length > 0) {
      restaurants.value[index] = data[0]

      const filtered = []
      restaurants.value.forEach((r, i) => {
        if (r.latitude && r.longitude) {
          filtered.push({
            lat: r.latitude,
            lng: r.longitude,
            color: filtered.length === 0 ? '#34A853' : '#FF5531',
            name: r.name,
            rating: r.rating,
            address: r.address,
            googleMapsUri: r.googleMapsUri,
            distanceMeters: r.distanceMeters,
            photoName: r.photoName,
            categories: filterCategories.length > 0 ? filterCategories : ['식당'],
            openNow: r.openNow,
            restaurantIndex: i
          })
        }
      })
      markers.value = filtered
    } else {
      bottomSeatRef.value?.resetRefreshing()
      alert('추가로 추천할 수 있는 식당이 없습니다.')
    }
  } catch (error) {
    bottomSeatRef.value?.resetRefreshing()
    console.error('식당 새로고침 실패:', error.message)
    alert('새로고침에 실패했습니다: ' + error.message)
  }
}

// 카드 클릭 시 해당 가게 위치로 지도 줌 + 인포윈도우 표시
const handleCardSelect = (restaurant) => {
  if (googleMapRef.value?.focusOnRestaurant && restaurant?.latitude && restaurant?.longitude) {
    googleMapRef.value.focusOnRestaurant(restaurant)
  }
}

const handleVoteCreate = () => {
  if (!restaurants.value.length) return
  if (!isLoggedIn.value) {
    showLoginRequiredModal.value = true
    return
  }

  const filterCategories = (filters.value.foodTypes || [])
    .map(id => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)

  const data = restaurants.value.map(r => ({
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
}

/** 로그인 필요 모달에서 "로그인하기" 클릭: 추천 데이터를 저장한 뒤 로그인 페이지로 이동. 로그인 후 투표 생성 페이지로 복귀하기 위함. */
const goToLoginFromModal = () => {
  if (!restaurants.value.length) {
    showLoginRequiredModal.value = false
    router.push('/login')
    return
  }
  const filterCategories = (filters.value.foodTypes || [])
    .map(id => FOOD_TYPE_TO_CATEGORY[id])
    .filter(Boolean)
  const data = restaurants.value.map(r => ({
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
    : restaurants.value.slice(0, 5)

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
      categories: filterCategories
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
        @toggle-favorite="(restaurant) => { /* TODO: 즐겨찾기 연동 */ }"
        @refresh="handleRefresh"
      />
      <div v-else class="map-error">
        <p>Google Maps API 키가 설정되지 않았습니다.</p>
        <p>환경변수 GOOGLE_MAPS_API_KEY를 설정해주세요.</p>
      </div>
      <BottomSeat
        ref="bottomSeatRef"
        :has-results="restaurants.length > 0"
        :restaurants="restaurants"
        :active-categories="(filters.foodTypes || []).map(id => FOOD_TYPE_TO_CATEGORY[id]).filter(Boolean)"
        @recommend="handleRecommend"
        @select="handleCardSelect"
        @roulette="rouletteOpen = true"
        @refresh="handleRefresh"
        @vote-create="handleVoteCreate"
      />
      <LunchRoulettePopup
        v-model="rouletteOpen"
        :restaurants="restaurants"
        @share="handleShare"
      />
      <LoginRequiredModal
        :visible="showLoginRequiredModal"
        @close="closeLoginRequiredModal"
        @confirm="goToLoginFromModal"
      />
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
</style>
