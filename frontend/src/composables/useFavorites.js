import { ref, computed } from 'vue'
import { useAuth } from '@/composables/useAuth'

const STORAGE_KEY = 'favoriteRestaurants'

function loadFromStorage() {
  if (typeof window === 'undefined') return []
  try {
    const raw = window.localStorage.getItem(STORAGE_KEY)
    if (!raw) return []
    const parsed = JSON.parse(raw)
    if (!Array.isArray(parsed)) return []
    return parsed
      .filter((item) => item && typeof item === 'object')
      .map((item) => {
        const rawPhoto = item.photoName ?? null
        const photoName = Array.isArray(rawPhoto) ? (rawPhoto[0] ?? null) : rawPhoto
        return {
          id: item.id ?? item.googlePlaceId ?? item.name ?? '',
          name: item.name ?? '',
          address: item.address ?? '',
          googleMapsUri: item.googleMapsUri ?? '',
          latitude: item.latitude ?? null,
          longitude: item.longitude ?? null,
          rating: typeof item.rating === 'number' ? item.rating : null,
          distanceMeters: item.distanceMeters ?? null,
          photoName: photoName != null ? String(photoName) : null,
          categories: Array.isArray(item.categories) ? item.categories : [],
          favoritedAt: item.favoritedAt ?? 0
        }
      })
      .filter((item) => item.id)
  } catch {
    return []
  }
}

const favorites = ref(loadFromStorage())
const { isLoggedIn, getToken } = useAuth()

function saveToStorage() {
  if (typeof window === 'undefined') return
  try {
    window.localStorage.setItem(STORAGE_KEY, JSON.stringify(favorites.value))
  } catch {
    // 저장 실패 시 조용히 무시
  }
}

const sortedFavorites = computed(() =>
  [...favorites.value].sort(
    (a, b) => (b.favoritedAt || 0) - (a.favoritedAt || 0)
  )
)

function addFavorite(item) {
  if (!item || !item.id) return

  // photoName: 추천 API는 배열, 즐겨찾기 API/DB는 문자열. 항상 단일 문자열로 보내야 저장·표시가 됨.
  const photoNameValue = Array.isArray(item.photoName)
    ? (item.photoName[0] ?? null)
    : (item.photoName ?? null)
  const photoNameStr = photoNameValue != null ? String(photoNameValue) : null

  // 클릭 시점의 값을 즉시 스냅샷으로 고정 (비동기 fetch에서 N-1번째가 전송되는 문제 방지)
  const snapshot = {
    id: String(item.id),
    name: String(item.name ?? ''),
    address: String(item.address ?? ''),
    googleMapsUri: String(item.googleMapsUri ?? ''),
    latitude: item.latitude ?? null,
    longitude: item.longitude ?? null,
    rating: typeof item.rating === 'number' ? item.rating : null,
    distanceMeters: item.distanceMeters ?? null,
    photoName: photoNameStr,
    categories: Array.isArray(item.categories) ? [...item.categories] : [],
    favoritedAt: item.favoritedAt ?? Date.now()
  }

  const index = favorites.value.findIndex((f) => f.id === snapshot.id)
  if (index !== -1) {
    const existing = favorites.value[index]
    favorites.value.splice(index, 1)
    favorites.value.unshift({
      ...existing,
      ...snapshot
    })
  } else {
    favorites.value.unshift({ ...snapshot })
  }

  // 로그인 시에는 DB만 사용, 비로그인 시에만 로컬스토리지 저장
  if (!isLoggedIn.value) saveToStorage()

  // 로그인한 경우 서버에도 즐겨찾기 동기화 (스냅샷 사용으로 항상 해당 클릭 식당만 전송)
  try {
    if (isLoggedIn.value) {
      const token = getToken()
      if (token) {
        const payload = {
          id: snapshot.id,
          name: snapshot.name,
          address: snapshot.address,
          googleMapsUri: snapshot.googleMapsUri,
          latitude: snapshot.latitude,
          longitude: snapshot.longitude,
          rating: snapshot.rating,
          distanceMeters: snapshot.distanceMeters,
          photoName: snapshot.photoName,
          categories: snapshot.categories,
          favoritedAt: snapshot.favoritedAt
        }
        fetch('/api/favorites', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            'X-Auth-Token': token
          },
          body: JSON.stringify(payload)
        }).catch(() => {})
      }
    }
  } catch {
    // 네트워크 오류 등은 로컬 즐겨찾기에는 영향 주지 않음
  }
}

function removeFavorite(id) {
  if (!id) return
  const index = favorites.value.findIndex((f) => f.id === id)
  if (index === -1) return
  favorites.value.splice(index, 1)

  if (!isLoggedIn.value) saveToStorage()

  // 로그인한 경우 서버에서도 삭제
  try {
    if (isLoggedIn.value) {
      const token = getToken()
      if (token) {
        fetch(`/api/favorites/${encodeURIComponent(id)}`, {
          method: 'DELETE',
          headers: {
            'X-Auth-Token': token
          }
        }).catch(() => {})
      }
    }
  } catch {
    // 무시
  }
}

function isFavorite(id) {
  if (!id) return false
  return favorites.value.some((f) => f.id === id)
}

/**
 * 로그아웃 시 호출. 메모리의 서버 즐겨찾기를 비우고 로컬스토리지 기준으로만 다시 채움.
 * 로그아웃 후에도 이전 계정의 즐겨찾기가 남아 보이는 현상 방지.
 */
function resetOnLogout() {
  favorites.value = loadFromStorage()
}

/**
 * 로그인 사용자의 즐겨찾기를 서버(DB)에서 불러와 로컬 상태에 반영.
 * 바텀시트 추천 목록에서 이미 즐겨찾기한 식당이 하트로 표시되도록 할 때 사용.
 */
async function syncFromServer() {
  if (typeof window === 'undefined') return
  if (!isLoggedIn.value) return
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch('/api/favorites/me', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (!res.ok) return
    const data = await res.json()
    if (!Array.isArray(data)) return
    favorites.value = data
      .filter((item) => item && item.id)
      .map((item) => ({
        id: String(item.id),
        name: String(item.name ?? ''),
        address: String(item.address ?? ''),
        googleMapsUri: String(item.googleMapsUri ?? ''),
        latitude: item.latitude ?? null,
        longitude: item.longitude ?? null,
        rating: typeof item.rating === 'number' ? item.rating : null,
        distanceMeters: item.distanceMeters ?? null,
        photoName: item.photoName ?? null,
        categories: Array.isArray(item.categories) ? item.categories : [],
        favoritedAt: item.favoritedAt
          ? (typeof item.favoritedAt === 'number'
            ? item.favoritedAt
            : new Date(item.favoritedAt).getTime())
          : 0
      }))
  } catch {
    // 네트워크 오류 시 기존 상태 유지
  }
}

export function useFavorites() {
  return {
    favorites: sortedFavorites,
    rawFavorites: favorites,
    addFavorite,
    removeFavorite,
    isFavorite,
    resetOnLogout,
    syncFromServer
  }
}

