<script setup>
import { ref, onMounted, watch } from 'vue'
import pegmanIcon from '@/assets/pegman-offscreen-2x.svg'
import defaultThumbnail from '@/assets/restaurnt_thumbnail_default_image.png'

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
    iconImg.src = pegmanIcon
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
  iconImg.src = pegmanIcon
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
  // 기존 마커 및 InfoWindow 제거
  markers.forEach(marker => marker.setMap(null))
  markers = []
  infoWindows.forEach(infoWindow => infoWindow.close())
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
        path: window.google.maps.SymbolPath.CIRCLE,
        scale: 20,
        fillColor: markerData.color || '#FF5531',
        fillOpacity: 1,
        strokeColor: 'white',
        strokeWeight: 2
      }
    })
    
    // InfoWindow 생성 및 마커 클릭 이벤트 추가
    if (markerData.name) {
      const infoContent = createInfoWindowContent(markerData, index + 1)
      const infoWindow = new window.google.maps.InfoWindow({
        content: infoContent
      })
      
      marker.addListener('click', () => {
        // 다른 InfoWindow 닫기
        infoWindows.forEach(iw => iw.close())
        // 현재 InfoWindow 열기
        infoWindow.open(map, marker)
      })
      
      infoWindows.push(infoWindow)
    }
    
    markers.push(marker)
  })
}

// InfoWindow 내용 생성 함수
const createInfoWindowContent = (markerData, index) => {
  const name = markerData.name || '식당 이름 없음'
  const rating = markerData.rating ? `⭐ ${markerData.rating.toFixed(1)}` : ''
  const address = markerData.address || '주소 정보 없음'
  const distance = markerData.distanceMeters ? `${(markerData.distanceMeters / 1000).toFixed(1)}km` : ''
  const mapsLink = markerData.googleMapsUri ? `<a href="${markerData.googleMapsUri}" target="_blank" style="color: #1976D2; text-decoration: none;">Google 지도에서 보기</a>` : ''
  
  // 썸네일 이미지: RestaurantDto photoName 배열의 0번째로 /api/restaurants/photo 호출, 없거나 실패 시 기본 이미지
  const firstPhotoName = markerData.photoName && Array.isArray(markerData.photoName) && markerData.photoName.length > 0
    ? markerData.photoName[0]
    : null
  const thumbnailSrc = firstPhotoName
    ? `/api/restaurants/photo?name=${encodeURIComponent(firstPhotoName)}`
    : defaultThumbnail
  const thumbnail = `<img src="${thumbnailSrc}" alt="${name}" style="max-width: 250px; max-height: 250px; width: auto; height: auto; object-fit: contain; border-radius: 8px; margin-bottom: 12px; display: block;" onerror="this.src='${defaultThumbnail}'">`
  
  return `
    <div style="padding: 12px; min-width: 200px; max-width: 274px; font-family: 'Pretendard', sans-serif;">
      ${thumbnail}
      <div style="font-size: 16px; font-weight: bold; margin-bottom: 8px; color: #202124;">
        ${index}. ${name}
      </div>
      ${rating ? `<div style="font-size: 14px; color: #5f6368; margin-bottom: 6px;">${rating}</div>` : ''}
      <div style="font-size: 13px; color: #5f6368; margin-bottom: 8px; line-height: 1.4;">
        ${address}
      </div>
      ${distance ? `<div style="font-size: 12px; color: #80868b; margin-bottom: 8px;">거리: ${distance}</div>` : ''}
      ${mapsLink ? `<div style="margin-top: 8px; padding-top: 8px; border-top: 1px solid #e8eaed;">${mapsLink}</div>` : ''}
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
defineExpose({
  getCurrentUserLocation
})
</script>

<template>
  <div ref="mapContainer" class="google-map"></div>
</template>

<style scoped>
.google-map {
  width: 100%;
  height: 100%;
  min-height: calc(100vh - 80px);
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
</style>
