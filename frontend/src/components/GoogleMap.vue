<script setup>
import { ref, onMounted, watch } from 'vue'
import currentLocationIcon from '@/assets/current-location-icon.svg'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'
import markerPinletIcon from '@/assets/marker-pin-icon.svg'
import favoriteHeartIcon from '@/assets/favorite-heart-icon.svg'
import refreshIcon from '@/assets/refresh-icon.svg'
import externalLinkIcon from '@/assets/external-link-icon.svg'

const props = defineProps({
  apiKey: {
    type: String,
    required: true
  },
  center: {
    type: Object,
    default: () => ({ lat: 37.5045, lng: 127.0489 }) // 신논현역 좌표
  },
  zoom: {
    type: Number,
    default: 16
  },
  markers: {
    type: Array,
    default: () => []
  },
  routes: {
    type: Array,
    default: () => []
  }
})

const mapContainer = ref(null)
let map = null
let markers = []
let infoWindows = []
let directionsService = null
let directionsRenderer = null
let currentLocationButton = null

// 새로고침 중인 마커 인덱스 (인포윈도우 애니메이션용)
let infoWindowRefreshingIndex = -1
let lastOpenInfoWindowIndex = -1

onMounted(() => {
  if (window.google && window.google.maps) {
    initMap()
  } else {
    loadGoogleMapsScript()
  }
})

const loadGoogleMapsScript = () => {
  // 이미 스크립트가 로드 중이거나 로드된 경우 확인
  const existingScript = document.querySelector('script[src*="maps.googleapis.com"]')
  if (existingScript) {
    if (window.google && window.google.maps) {
      initMap()
    } else {
      existingScript.addEventListener('load', () => {
        initMap()
      })
    }
    return
  }

  const script = document.createElement('script')
  script.src = `https://maps.googleapis.com/maps/api/js?key=${props.apiKey}&libraries=places,directions&language=ko`
  script.async = true
  script.defer = true
  script.onload = () => {
    initMap()
  }
  script.onerror = () => {
    console.error('Google Maps 스크립트 로드 실패')
  }
  document.head.appendChild(script)
}

const initMap = () => {
  if (!mapContainer.value) return

  map = new window.google.maps.Map(mapContainer.value, {
    center: props.center,
    zoom: props.zoom,
    mapTypeControl: true,
    mapTypeControlOptions: {
      style: window.google.maps.MapTypeControlStyle.HORIZONTAL_BAR,
      position: window.google.maps.ControlPosition.TOP_LEFT
    },
    zoomControl: true,
    zoomControlOptions: {
      position: window.google.maps.ControlPosition.TOP_RIGHT
    },
    streetViewControl: false,
    fullscreenControl: false
  })

  directionsService = new window.google.maps.DirectionsService()
  directionsRenderer = new window.google.maps.DirectionsRenderer({
    map: map,
    suppressMarkers: true
  })

  // 지도가 완전히 로드된 후 현재 위치 버튼 추가 (확대/축소 버튼 아래에 위치하도록)
  window.google.maps.event.addListenerOnce(map, 'idle', () => {
    // 약간의 지연을 두어 확대/축소 버튼이 완전히 렌더링된 후 추가
    setTimeout(() => {
      createCurrentLocationButton()
    }, 200)
  })

  updateMarkers()
  updateRoutes()
}

let currentLocationMarker = null

// 현재 위치 버튼 생성
const createCurrentLocationButton = () => {
  if (!map) return

  // 확대/축소 버튼 컨테이너 찾기
  const mapDiv = mapContainer.value
  const zoomControls = mapDiv.querySelector('.gmnoprint')
  
  if (!zoomControls) {
    // 확대/축소 버튼을 찾을 수 없으면 일반적인 방법으로 추가
    const buttonContainer = document.createElement('div')
    buttonContainer.className = 'current-location-button-container'
    buttonContainer.style.cssText = 'margin-top: 10px; display: block; width: 40px; height: 40px;'
    
    const button = document.createElement('button')
    button.className = 'current-location-button'
    button.type = 'button'
    button.title = '현재 위치로 이동'
    
    const iconImg = document.createElement('img')
    iconImg.src = currentLocationIcon
    iconImg.alt = '현재 위치'
    iconImg.className = 'current-location-icon'
    button.appendChild(iconImg)
    
    button.addEventListener('click', () => {
      moveToCurrentLocation()
    })
    
    buttonContainer.appendChild(button)
    map.controls[window.google.maps.ControlPosition.TOP_RIGHT].push(buttonContainer)
    currentLocationButton = buttonContainer
    return
  }

  // 확대/축소 버튼 컨테이너의 높이와 위치 정보 가져오기
  const zoomControlsHeight = parseInt(zoomControls.getAttribute('data-control-height')) || 81
  const zoomControlsStyle = window.getComputedStyle(zoomControls)
  const zoomControlsTop = parseInt(zoomControlsStyle.top) || 0
  const zoomControlsRight = parseInt(zoomControlsStyle.right) || 40
  const zoomControlsMargin = parseInt(zoomControlsStyle.margin) || 10
  
  // 확대/축소 버튼 아래에 위치하도록 top 값 계산
  // 확대/축소 버튼 높이 + margin + 간격(10px)
  const currentLocationTop = zoomControlsTop + zoomControlsHeight + zoomControlsMargin + 10
  
  // 버튼 컨테이너 생성
  const buttonContainer = document.createElement('div')
  buttonContainer.className = 'current-location-button-container'
  buttonContainer.style.cssText = `
    position: absolute !important;
    right: ${zoomControlsRight}px !important;
    top: ${currentLocationTop}px !important;
    width: 40px !important;
    height: 40px !important;
    display: block !important;
    z-index: 1000 !important;
  `
  
  const button = document.createElement('button')
  button.className = 'current-location-button'
  button.type = 'button'
  button.title = '현재 위치로 이동'
  
  // SVG 아이콘을 img 태그로 추가
  const iconImg = document.createElement('img')
  iconImg.src = currentLocationIcon
  iconImg.alt = '현재 위치'
  iconImg.className = 'current-location-icon'
  button.appendChild(iconImg)
  
  button.addEventListener('click', () => {
    moveToCurrentLocation()
  })
  
  buttonContainer.appendChild(button)
  
  // 확대/축소 버튼과 같은 부모 요소에 추가
  const zoomParent = zoomControls.parentElement
  if (zoomParent) {
    zoomParent.appendChild(buttonContainer)
  } else {
    // 폴백: map div에 직접 추가
    mapDiv.appendChild(buttonContainer)
  }
  
  currentLocationButton = buttonContainer
}

// 현재 위치로 이동
const moveToCurrentLocation = () => {
  if (!map) return

  if (!navigator.geolocation) {
    alert('이 브라우저는 위치 정보를 지원하지 않습니다.')
    return
  }

  const button = currentLocationButton?.querySelector('.current-location-button')
  if (button) {
    button.disabled = true
    button.style.opacity = '0.6'
  }

  navigator.geolocation.getCurrentPosition(
    (position) => {
      const userLocation = {
        lat: position.coords.latitude,
        lng: position.coords.longitude
      }

      // 지도 중심 이동
      map.setCenter(userLocation)
      map.setZoom(16)

      // 현재 위치 마커 업데이트
      if (currentLocationMarker) {
        currentLocationMarker.setPosition(userLocation)
      } else {
        currentLocationMarker = new window.google.maps.Marker({
          position: userLocation,
          map: map,
          icon: {
            path: window.google.maps.SymbolPath.CIRCLE,
            scale: 8,
            fillColor: '#4285F4',
            fillOpacity: 1,
            strokeColor: 'white',
            strokeWeight: 2
          },
          zIndex: 1000
        })
      }


      if (button) {
        button.disabled = false
        button.style.opacity = '1'
      }
    },
    (error) => {
      console.error('위치 정보를 가져올 수 없습니다:', error)
      let errorMessage = '위치 정보를 가져올 수 없습니다.'
      switch (error.code) {
        case error.PERMISSION_DENIED:
          errorMessage = '위치 정보 사용 권한이 거부되었습니다.'
          break
        case error.POSITION_UNAVAILABLE:
          errorMessage = '위치 정보를 사용할 수 없습니다.'
          break
        case error.TIMEOUT:
          errorMessage = '위치 정보 요청 시간이 초과되었습니다.'
          break
      }
      alert(errorMessage)

      if (button) {
        button.disabled = false
        button.style.opacity = '1'
      }
    },
    {
      enableHighAccuracy: true,
      timeout: 10000,
      maximumAge: 0
    }
  )
}

const updateMarkers = () => {
  const wasRefreshingIndex = infoWindowRefreshingIndex
  if (infoWindowRefreshingIndex >= 0) infoWindowRefreshingIndex = -1

  // 기존 마커 및 InfoWindow 제거
  markers.forEach(marker => marker.setMap(null))
  markers = []
  infoWindows.forEach((iw) => iw && iw.close())
  infoWindows = []

  // 현재 위치 마커 추가 (파란색 원)
  if (props.center) {
    if (currentLocationMarker) {
      currentLocationMarker.setMap(null)
    }
    currentLocationMarker = new window.google.maps.Marker({
      position: props.center,
      map: map,
      icon: {
        path: window.google.maps.SymbolPath.CIRCLE,
        scale: 8,
        fillColor: '#4285F4',
        fillOpacity: 1,
        strokeColor: 'white',
        strokeWeight: 2
      },
      zIndex: 1000
    })
  }

  // 새 마커 추가
  props.markers.forEach((markerData, index) => {
    const marker = new window.google.maps.Marker({
      position: { lat: markerData.lat, lng: markerData.lng },
      map: map,
      label: {
        text: String(index + 1),
        color: 'white',
        fontSize: '14px',
        fontWeight: 'bold'
      },
      icon: {
        url: markerPinletIcon,
        scaledSize: new window.google.maps.Size(36, 48),
        anchor: new window.google.maps.Point(18, 48),
        labelOrigin: new window.google.maps.Point(18, 20)
      }
    })
    
    // InfoWindow 생성 및 마커 클릭 이벤트 추가
    if (markerData.name) {
      const isSwapped = index === infoWindowRefreshingIndex
      const infoContent = createInfoWindowContent(markerData, index, { isRefreshing: false, isSwapped })
      const infoWindow = new window.google.maps.InfoWindow({
        content: infoContent
      })

      window.google.maps.event.addListener(infoWindow, 'domready', () => {
        const container = infoWindow.getContent()
        if (container && typeof container.querySelector === 'function') {
          const favoriteBtn = container.querySelector('[data-action="favorite"]')
          if (favoriteBtn) {
            favoriteBtn.addEventListener('click', (e) => {
              e.preventDefault()
              e.stopPropagation()
              emit('toggleFavorite', markerData)
            })
          }
        }
      })

      marker.addListener('click', () => {
        infoWindows.forEach((iw) => iw && iw.close())
        lastOpenInfoWindowIndex = index
        infoWindow.open(map, marker)
      })

      infoWindows.push(infoWindow)
    } else {
      infoWindows.push(null)
    }

    markers.push(marker)
  })

  // 새로고침 완료 후 해당 인덱스의 인포윈도우 재오픈 및 줌인
  if (wasRefreshingIndex >= 0) {
    const markerIdx = props.markers.findIndex((m) => (m.restaurantIndex ?? -1) === wasRefreshingIndex)
    const idx = markerIdx >= 0 ? markerIdx : wasRefreshingIndex
    if (idx >= 0 && markers[idx] && infoWindows[idx]) {
      // 새로고침된 식당 위치로 지도 줌인
      const pos = markers[idx].getPosition()
      if (pos && map) {
        map.panTo(pos)
        map.setZoom(17)
      }
      lastOpenInfoWindowIndex = idx
      infoWindows[idx].open(map, markers[idx])
    }
  }
}

// InfoWindow 내용 생성 함수 (이미지와 동일한 레이아웃: 이미지+정보, 하트, 액션 아이콘)
const createInfoWindowContent = (markerData, index, opts = {}) => {
  const { isRefreshing = false, isSwapped = false } = opts
  const name = markerData.name || '식당 이름 없음'
  const category = (markerData.categories && markerData.categories[0]) || '식당'
  const mapsUri = markerData.googleMapsUri || ''
  const directionsUri = markerData.lat && markerData.lng
    ? `https://www.google.com/maps/dir/?api=1&destination=${markerData.lat},${markerData.lng}`
    : mapsUri

  const firstPhotoName = markerData.photoName && Array.isArray(markerData.photoName) && markerData.photoName.length > 0
    ? markerData.photoName[0]
    : null
  const thumbnailSrc = firstPhotoName
    ? `/api/restaurants/photo?name=${encodeURIComponent(firstPhotoName)}`
    : defaultThumbnail

  // escape for HTML
  const esc = (s) => (s || '').replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/"/g, '&quot;')

  const refreshBtnDisabled = isRefreshing ? ' disabled' : ''
  const cardClasses = ['info-window-card']
  if (isRefreshing) cardClasses.push('iw-refreshing')
  if (isSwapped) cardClasses.push('iw-swapped')

  return `
    <style>
      .iw-refreshing { pointer-events: none; }
      .iw-refreshing .iw-image-wrap, .iw-refreshing .iw-info { opacity: 0.45; filter: grayscale(0.4); transition: opacity 0.3s, filter 0.3s; }
      .iw-shimmer { position: absolute; inset: 0; z-index: 3; border-radius: inherit; overflow: hidden;
        background: linear-gradient(90deg, transparent 0%, rgba(255,255,255,0.55) 50%, transparent 100%);
        background-size: 200% 100%; animation: iw-shimmer 1.4s ease-in-out infinite; }
      @keyframes iw-shimmer { 0% { background-position: 200% 0; } 100% { background-position: -200% 0; } }
      .iw-refresh-spin { animation: iw-spin 0.8s linear infinite; }
      @keyframes iw-spin { from { transform: rotate(0deg); } to { transform: rotate(360deg); } }
      .iw-swapped { animation: iw-swap-in 0.45s cubic-bezier(0.22, 1, 0.36, 1) both; }
      @keyframes iw-swap-in { 0% { opacity: 0; transform: translateY(18px) scale(0.96); } 100% { opacity: 1; transform: translateY(0) scale(1); } }
    </style>
    <div class="${cardClasses.join(' ')}" data-marker-idx="${index}" style="
      position: relative; display: flex; padding: 12px; width: 260px; font-family: 'Pretendard', sans-serif;
      background: #fff; border-radius: 12px; box-sizing: border-box; gap: 12px;
    ">
      ${isRefreshing ? '<div class="iw-shimmer"></div>' : ''}
      <div class="iw-image-wrap" style="
        position: relative; flex-shrink: 0; width: 88px; height: 88px; border-radius: 8px;
        overflow: hidden; background: #f5f5f5;
      ">
        <img src="${thumbnailSrc}" alt="${esc(name)}" style="
          width: 100%; height: 100%; object-fit: cover;
        " onerror="this.src='${defaultThumbnail}'" />
        <button type="button" class="info-window-favorite" data-action="favorite" title="찜하기" style="
          position: absolute; right: 0; bottom: 0; width: 32px; height: 32px; border: none;
          border-radius: 50%; background: rgba(0,0,0,0.5); display: flex; align-items: center; justify-content: center;
          cursor: pointer; padding: 0;
        ">
          <img src="${favoriteHeartIcon}" alt="찜" style="width: 18px; height: 18px; filter: brightness(0) invert(1);" />
        </button>
      </div>
      <div class="iw-info" style="flex: 1; min-width: 0; display: flex; flex-direction: column; gap: 4px;">
        <div class="info-window-header" style="display: flex; align-items: flex-start; justify-content: space-between; gap: 6px;">
          <h3 class="info-window-name" style="
            font-size: 15px; font-weight: 700; color: #3c4043; margin: 0; line-height: 1.3;
            overflow: hidden; text-overflow: ellipsis; display: -webkit-box; -webkit-line-clamp: 2; -webkit-box-orient: vertical;
          ">${esc(name)}</h3>
          <div class="info-window-actions" style="display: flex; gap: 4px; flex-shrink: 0;">
            <button type="button" class="info-window-action" data-action="refresh" title="새로고침"${refreshBtnDisabled} style="
              width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
              border: none; border-radius: 4px; background: transparent; cursor: pointer; padding: 0; transition: background 0.2s;
            " onclick="event.preventDefault();event.stopPropagation();(window.__lunchMapRefresh||function(){})(${markerData.restaurantIndex ?? index})">
              <img src="${refreshIcon}" alt="새로고침" class="${isRefreshing ? 'iw-refresh-spin' : ''}" style="width: 18px; height: 18px; object-fit: contain;" />
            </button>
            <a href="${mapsUri || directionsUri}" target="_blank" rel="noopener" class="info-window-action" title="Google 지도" style="
              width: 24px; height: 24px; display: flex; align-items: center; justify-content: center;
              border-radius: 4px; text-decoration: none; transition: background 0.2s;
            ">
              <img src="${externalLinkIcon}" alt="외부 링크" style="width: 18px; height: 18px; object-fit: contain;" />
            </a>
          </div>
        </div>
        <p class="info-window-category" style="
          font-size: 12px; font-weight: 500; color: #3c4043; margin: 0;
        ">${esc(category)}</p>
      </div>
    </div>
  `
}

const routeRenderers = []

const updateRoutes = () => {
  // 기존 경로 렌더러 제거
  routeRenderers.forEach(renderer => renderer.setMap(null))
  routeRenderers.length = 0

  if (!directionsService || !directionsRenderer || props.routes.length === 0) return

  props.routes.forEach((route, index) => {
    if (route.origin && route.destination) {
      const origin = new window.google.maps.LatLng(route.origin.lat, route.origin.lng)
      const destination = new window.google.maps.LatLng(route.destination.lat, route.destination.lng)
      
      directionsService.route(
        {
          origin: origin,
          destination: destination,
          travelMode: window.google.maps.TravelMode.WALKING
        },
        (result, status) => {
          if (status === 'OK') {
            const routeRenderer = new window.google.maps.DirectionsRenderer({
              map: map,
              directions: result,
              suppressMarkers: true,
              polylineOptions: {
                strokeColor: route.color || '#FF5531',
                strokeWeight: 4,
                strokeOpacity: 0.8
              }
            })
            routeRenderers.push(routeRenderer)
          }
        }
      )
    }
  })
}

watch(() => props.markers, () => {
  if (map) updateMarkers()
}, { deep: true })

watch(() => props.routes, () => {
  if (map) updateRoutes()
}, { deep: true })

watch(() => props.center, () => {
  if (map) {
    map.setCenter(props.center)
  }
}, { deep: true })

// 현재 사용자 위치를 Promise로 반환하는 메서드
const getCurrentUserLocation = () => {
  return new Promise((resolve, reject) => {
    if (!navigator.geolocation) {
      reject(new Error('이 브라우저는 위치 정보를 지원하지 않습니다.'))
      return
    }

    navigator.geolocation.getCurrentPosition(
      (position) => {
        const userLocation = {
          lat: position.coords.latitude,
          lng: position.coords.longitude
        }
        
        // 지도 중심 이동 및 마커 업데이트
        if (map) {
          map.setCenter(userLocation)
          map.setZoom(16)
          
          // 현재 위치 마커 업데이트
          if (currentLocationMarker) {
            currentLocationMarker.setPosition(userLocation)
          } else {
            currentLocationMarker = new window.google.maps.Marker({
              position: userLocation,
              map: map,
              icon: {
                path: window.google.maps.SymbolPath.CIRCLE,
                scale: 8,
                fillColor: '#4285F4',
                fillOpacity: 1,
                strokeColor: 'white',
                strokeWeight: 2
              },
              zIndex: 1000
            })
          }
        }
        
        resolve(userLocation)
      },
      (error) => {
        let errorMessage = '위치 정보를 가져올 수 없습니다.'
        switch (error.code) {
          case error.PERMISSION_DENIED:
            errorMessage = '위치 정보 사용 권한이 거부되었습니다.'
            break
          case error.POSITION_UNAVAILABLE:
            errorMessage = '위치 정보를 사용할 수 없습니다.'
            break
          case error.TIMEOUT:
            errorMessage = '위치 정보 요청 시간이 초과되었습니다.'
            break
        }
        reject(new Error(errorMessage))
      },
      {
        enableHighAccuracy: true,
        timeout: 10000,
        maximumAge: 0
      }
    )
  })
}

// 외부에서 호출 가능하도록 expose
const emit = defineEmits(['refresh', 'toggleFavorite'])

// InfoWindow 내 인라인 onclick에서 호출 (다른 document 컨텍스트 대응)
if (typeof window !== 'undefined') {
  window.__lunchMapRefresh = (idx) => {
    infoWindowRefreshingIndex = idx
    const markerIdx = props.markers.findIndex((m) => (m.restaurantIndex ?? -1) === idx)
    const actualIdx = markerIdx >= 0 ? markerIdx : idx
    const iw = infoWindows[actualIdx]
    const markerData = props.markers[actualIdx]
    if (iw && markerData) {
      iw.setContent(createInfoWindowContent(markerData, actualIdx, { isRefreshing: true }))
    }
    emit('refresh', idx)
  }
}

// 카드 클릭 시 해당 가게 위치로 줌하고 인포윈도우 표시
const focusOnRestaurant = (restaurant) => {
  if (!map || !restaurant?.latitude || !restaurant?.longitude) return
  const lat = Number(restaurant.latitude)
  const lng = Number(restaurant.longitude)
  const idx = props.markers.findIndex(
    (m) => Math.abs(m.lat - lat) < 1e-6 && Math.abs(m.lng - lng) < 1e-6
  )
  if (idx < 0) return
  const markerObj = markers[idx]
  const infoWindowObj = infoWindows[idx]
  if (!markerObj || !infoWindowObj) return
  map.panTo({ lat, lng })
  map.setZoom(17)
  infoWindows.forEach((iw) => iw && iw.close())
  infoWindowObj.open(map, markerObj)
}

defineExpose({
  getCurrentUserLocation,
  focusOnRestaurant
})
</script>

<template>
  <div ref="mapContainer" class="google-map"></div>
</template>

<style scoped>
.google-map {
  width: 100%;
  height: 100%;
  min-height: 0;
  flex: 1;
}

/* 현재 위치 버튼 스타일 */
:deep(.current-location-button-container) {
  margin-top: 10px !important;
  display: block !important;
  clear: both;
  width: 40px;
  height: 40px;
}

:deep(.current-location-button) {
  background-color: white;
  border: none;
  border-radius: 2px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.3);
  cursor: pointer;
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 8px;
  transition: background-color 0.2s, box-shadow 0.2s;
  box-sizing: border-box;
}

:deep(.current-location-button:hover) {
  background-color: #f5f5f5;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.4);
}

:deep(.current-location-button:active) {
  background-color: #e8e8e8;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.3);
}

:deep(.current-location-button:disabled) {
  cursor: not-allowed;
  opacity: 0.6;
}

:deep(.current-location-button .current-location-icon) {
  width: 20px;
  height: 20px;
  display: block;
  object-fit: contain;
}

/* InfoWindow 스타일 오버라이드 — 콘텐츠 완벽 중앙 정렬 */
:deep(.gm-style-iw-chr) {
  display: none !important;
}

:deep(.gm-style-iw-c) {
  padding: 0 !important;
}

:deep(.gm-style-iw-d) {
  overflow: hidden !important;
  padding: 0 !important;
}
</style>
