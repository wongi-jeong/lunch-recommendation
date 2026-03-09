import { ref, computed } from 'vue'

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
      .map((item) => ({
        id: item.id ?? item.googlePlaceId ?? item.name ?? '',
        name: item.name ?? '',
        address: item.address ?? '',
        googleMapsUri: item.googleMapsUri ?? '',
        latitude: item.latitude ?? null,
        longitude: item.longitude ?? null,
        rating: typeof item.rating === 'number' ? item.rating : null,
        distanceMeters: item.distanceMeters ?? null,
        photoName: item.photoName ?? null,
        categories: Array.isArray(item.categories) ? item.categories : [],
        favoritedAt: item.favoritedAt ?? 0
      }))
      .filter((item) => item.id)
  } catch {
    return []
  }
}

const favorites = ref(loadFromStorage())

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

  const index = favorites.value.findIndex((f) => f.id === item.id)
  if (index !== -1) {
    const existing = favorites.value[index]
    favorites.value.splice(index, 1)
    favorites.value.unshift({
      ...existing,
      ...item,
      favoritedAt: item.favoritedAt ?? Date.now()
    })
  } else {
    favorites.value.unshift({
      ...item,
      favoritedAt: item.favoritedAt ?? Date.now()
    })
  }

  saveToStorage()
}

function removeFavorite(id) {
  if (!id) return
  const index = favorites.value.findIndex((f) => f.id === id)
  if (index === -1) return
  favorites.value.splice(index, 1)
  saveToStorage()
}

function isFavorite(id) {
  if (!id) return false
  return favorites.value.some((f) => f.id === id)
}

export function useFavorites() {
  return {
    favorites: sortedFavorites,
    rawFavorites: favorites,
    addFavorite,
    removeFavorite,
    isFavorite
  }
}

