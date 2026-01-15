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

const markers = ref([
  { lat: 37.5045, lng: 127.0489, color: '#FF5531' },
  { lat: 37.5050, lng: 127.0495, color: '#FF5531' },
  { lat: 37.5040, lng: 127.0490, color: '#34A853' },
  { lat: 37.5055, lng: 127.0485, color: '#FF5531' },
  { lat: 37.5035, lng: 127.0500, color: '#34A853' },
  { lat: 37.5060, lng: 127.0490, color: '#FF5531' },
  { lat: 37.5040, lng: 127.0505, color: '#FF5531' },
  { lat: 37.5050, lng: 127.0510, color: '#34A853' },
  { lat: 37.5030, lng: 127.0480, color: '#FF5531' },
  { lat: 37.5065, lng: 127.0505, color: '#FF5531' },
  { lat: 37.5045, lng: 127.0515, color: '#34A853' },
  { lat: 37.5025, lng: 127.0495, color: '#FF5531' }
])

const routes = ref([
  {
    origin: { lat: 37.5045, lng: 127.0489 },
    destination: { lat: 37.5040, lng: 127.0500 },
    color: '#FF5531'
  },
  {
    origin: { lat: 37.5050, lng: 127.0495 },
    destination: { lat: 37.5035, lng: 127.0505 },
    color: '#34A853'
  }
])

const mapCenter = ref({ lat: 37.5045, lng: 127.0489 })

onMounted(() => {
  apiKey.value = import.meta.env.VITE_GOOGLE_MAPS_API_KEY || import.meta.env.GOOGLE_MAPS_API_KEY || ''
  
  if (!apiKey.value) {
    console.warn('Google Maps API 키가 설정되지 않았습니다.')
    console.warn('환경변수 GOOGLE_MAPS_API_KEY 또는 .env 파일의 VITE_GOOGLE_MAPS_API_KEY를 설정해주세요.')
  }
})

const handleRecommend = () => {
  console.log('추천 받기:', filters.value)
}
</script>

<template>
  <div class="nearby-recommendation">
    <div class="map-container">
      <GoogleMap
        v-if="apiKey"
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
