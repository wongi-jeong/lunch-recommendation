import { ref, computed } from 'vue'

const AUTH_TOKEN_KEY = 'authToken'
const PROFILE_IMAGE_INDEX_KEY = 'profileImageIndex'

const token = ref(sessionStorage.getItem(AUTH_TOKEN_KEY) ?? '')
const profileImageIndex = ref(
  (() => {
    const v = sessionStorage.getItem(PROFILE_IMAGE_INDEX_KEY)
    if (v === null || v === '') return 0
    const n = parseInt(v, 10)
    return Number.isNaN(n) ? 0 : Math.max(0, Math.min(7, n))
  })()
)

export function useAuth() {
  const isLoggedIn = computed(() => !!token.value)

  function setToken(value) {
    token.value = value ?? ''
    if (value != null && value !== '') {
      sessionStorage.setItem(AUTH_TOKEN_KEY, value)
    } else {
      sessionStorage.removeItem(AUTH_TOKEN_KEY)
      sessionStorage.removeItem(PROFILE_IMAGE_INDEX_KEY)
    }
  }

  function setProfileImageIndex(index) {
    const n = Math.max(0, Math.min(7, Number(index) || 0))
    profileImageIndex.value = n
    if (isLoggedIn.value) {
      sessionStorage.setItem(PROFILE_IMAGE_INDEX_KEY, String(n))
    }
  }

  function setFromMemberResponse(data) {
    if (!data) return
    if (data.id != null) setToken(String(data.id))
    else if (data.email) setToken(data.email)
    const idx = data.profileImageIndex ?? data.profile_image_index
    const n = typeof idx === 'number' ? idx : parseInt(idx, 10)
    if (!Number.isNaN(n)) setProfileImageIndex(n)
  }

  function clearAuth() {
    token.value = ''
    profileImageIndex.value = 0
    sessionStorage.removeItem(AUTH_TOKEN_KEY)
    sessionStorage.removeItem(PROFILE_IMAGE_INDEX_KEY)
  }

  function getToken() {
    const t = sessionStorage.getItem(AUTH_TOKEN_KEY)
    if (t !== null) token.value = t
    return token.value
  }

  return {
    token,
    profileImageIndex,
    isLoggedIn,
    setToken,
    setProfileImageIndex,
    setFromMemberResponse,
    clearAuth,
    getToken
  }
}
