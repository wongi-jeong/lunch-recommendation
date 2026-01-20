<script setup>
import { ref, onMounted } from 'vue'
import GoogleMap from './GoogleMap.vue'
import FilterPanel from './FilterPanel.vue'

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

onMounted(() => {
  apiKey.value = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || import.meta.env.GOOGLE_MAPS_API_KEY || ''
  
  if (!apiKey.value) {
    console.warn('Google Maps API 키가 설정되지 않았습니다.')
    console.warn('환경변수 GOOGLE_MAPS_API_KEY 또는 .env 파일의 VITE_GOOGLE_MAPS_API_KEY를 설정해주세요.')
  }
})

// 검색된 식당 목록
const restaurants = ref([])

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
    
    console.log('=== 추천 요청 데이터 ===')
    console.log('사용자 위치 (위도):', userLocation.lat)
    console.log('사용자 위치 (경도):', userLocation.lng)
    console.log('검색 반경 (미터):', radius)
    
    // 지도 중심 업데이트
    mapCenter.value = userLocation
    
    // 백엔드 API 호출
    const response = await fetch(
      `/api/restaurants/nearby?lat=${userLocation.lat}&lng=${userLocation.lng}&radius=${radius}`
    )
    
    if (!response.ok) {
      throw new Error(`API 요청 실패: ${response.status} ${response.statusText}`)
    }
    
    const data = await response.json()
    restaurants.value = data
    
    console.log('=== 검색 결과 ===')
    console.log('검색된 식당 수:', data.length)
    console.log('식당 목록:', data)
    
    // 마커 업데이트 - 검색된 식당들을 지도에 표시
    if (data.length > 0) {
      markers.value = data
        .filter(restaurant => restaurant.latitude && restaurant.longitude)
        .map((restaurant, index) => ({
          lat: restaurant.latitude,
          lng: restaurant.longitude,
          color: index === 0 ? '#34A853' : '#FF5531', // 첫 번째 식당은 녹색으로 표시
          name: restaurant.name,
          rating: restaurant.rating
        }))
      
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
      />
      <div v-else class="map-error">
        <p>Google Maps API 키가 설정되지 않았습니다.</p>
        <p>환경변수 GOOGLE_MAPS_API_KEY를 설정해주세요.</p>
      </div>
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
  height: calc(100vh - 80px);
  overflow: hidden;
}

.map-container {
  width: 100%;
  height: 100%;
  position: relative;
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
