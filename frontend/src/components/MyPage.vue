<script setup>
import { ref, computed, watch, onMounted, onUnmounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import { useFavorites } from '@/composables/useFavorites'
import profileIcon from '@/assets/icon-profile-default.svg'
import profileEditIcon from '@/assets/icon-profile-edit.svg'
import profileAvatar1 from '@/assets/avatar-1.png'
import profileAvatar2 from '@/assets/avatar-2.png'
import profileAvatar3 from '@/assets/avatar-3.png'
import profileAvatar4 from '@/assets/avatar-4.png'
import profileAvatar5 from '@/assets/avatar-5.png'
import profileAvatar6 from '@/assets/avatar-6.png'
import profileAvatar7 from '@/assets/avatar-7.png'
import profileAvatar8 from '@/assets/avatar-8.png'
import defaultThumbnail from '@/assets/img-placeholder-restaurant.png'
import externalLinkIcon from '@/assets/icon-external-link.svg'
import ProfileChangeModal from '@/components/ProfileChangeModal.vue'
/* Figma 47:3141 저장한 필터 에셋 */
import iconArrowPagination from '@/assets/icon-arrow-pagination.svg'
import iconStarFilled from '@/assets/icon-star-filled.svg'
import iconEdit from '@/assets/icon-edit.svg'
import iconCheckboxOutline from '@/assets/icon-checkbox-outline.svg'
import iconCheckboxChecked from '@/assets/icon-checkbox-checked.svg'

const route = useRoute()
const router = useRouter()
const { getToken, clearAuth, profileImageIndex: authProfileIndex, setProfileImageIndex: setAuthProfileIndex } = useAuth()
const profileOptions = [profileAvatar1, profileAvatar2, profileAvatar3, profileAvatar4, profileAvatar5, profileAvatar6, profileAvatar7, profileAvatar8]

const currentProfileImage = ref(profileAvatar1)
const { removeFavorite: removeLocalFavorite, resetOnLogout } = useFavorites()
const showProfileChangeModal = ref(false)
const memberEmail = ref('')
const emailLoadError = ref('')
const ongoingVotes = ref([])
const endedVotes = ref([])

const PAGE_SIZE = 5
const ongoingPage = ref(1)
const endedPage = ref(1)

// 룰렛 결과 히스토리 (로컬 저장)
const ROULETTE_HISTORY_KEY_PREFIX = 'rouletteHistory:'
const rouletteHistory = ref([])

// 즐겨찾기 (최근 즐겨찾기 한 장소) - 회원 DB 기반
const memberFavorites = ref([])

// 저장한 필터 (내 저장 섹션)
const savedFilters = ref([])
const FOOD_TYPE_LABELS = {
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
const DISTANCE_LABELS = { 300: '300m', 800: '800m', 1200: '1.2km' }

// 실시간 남은 시간 계산용
const now = ref(Date.now())
let timerInterval = null

function applyOngoingVoteTimers() {
  if (!Array.isArray(ongoingVotes.value) || ongoingVotes.value.length === 0) return

  const nowMs = Date.now()
  const stillOngoing = []
  const newlyEnded = []

  for (const vote of ongoingVotes.value) {
    if (!vote || !vote.timer || vote.timer === 'none' || !vote.createdAt) {
      stillOngoing.push(vote)
      continue
    }

    const durationMs =
      {
        '10min': 10 * 60 * 1000,
        '30min': 30 * 60 * 1000,
        '1hour': 60 * 60 * 1000
      }[vote.timer] || 0

    if (!durationMs) {
      stillOngoing.push(vote)
      continue
    }

    const endTime = new Date(vote.createdAt).getTime() + durationMs
    if (Number.isNaN(endTime) || nowMs < endTime) {
      stillOngoing.push(vote)
      continue
    }

    // 타이머가 만료된 투표는 진행중 목록에서 제거하고 종료 목록으로 이동
    const endedAt = vote.endedAt || new Date(endTime).toISOString()
    newlyEnded.push({
      ...vote,
      ended: true,
      endedAt
    })
  }

  if (newlyEnded.length > 0) {
    ongoingVotes.value = stillOngoing
    endedVotes.value = [...endedVotes.value, ...newlyEnded]
  }
}

const latestFavorites = computed(() => {
  if (!memberFavorites.value || memberFavorites.value.length === 0) return []
  return memberFavorites.value.slice(0, 3)
})

const getFavoriteImage = (item) => {
  const name = item?.photoName != null
    ? (Array.isArray(item.photoName) ? item.photoName[0] : item.photoName)
    : null
  if (name) {
    return `/api/restaurants/photo?name=${encodeURIComponent(name)}`
  }
  return defaultThumbnail
}

const getFavoriteCategory = (item) => {
  if (!item) return '식당'
  if (Array.isArray(item.categories) && item.categories.length > 0) {
    return item.categories[0]
  }
  return '식당'
}

const getFavoriteLink = (item) => {
  if (!item) return ''
  if (item.googleMapsUri) return item.googleMapsUri
  if (item.latitude != null && item.longitude != null) {
    const lat = Number(item.latitude)
    const lng = Number(item.longitude)
    if (!Number.isNaN(lat) && !Number.isNaN(lng)) {
      return `https://www.google.com/maps/search/?api=1&query=${lat},${lng}`
    }
  }
  return ''
}

const openFavoriteExternal = (item) => {
  const url = getFavoriteLink(item)
  if (!url) return
  window.open(url, '_blank', 'noopener')
}

const handleToggleFavoriteFromMyPage = (item) => {
  if (!item?.id) return
  const token = getToken()
  if (!token) return

   // 로컬 즐겨찾기(근처 추천용)에서도 함께 제거
  removeLocalFavorite(item.id)

  fetch(`/api/favorites/${encodeURIComponent(item.id)}`, {
    method: 'DELETE',
    headers: {
      'X-Auth-Token': token
    }
  })
    .then(() => fetchMemberFavorites())
    .catch(() => fetchMemberFavorites())
}

const totalOngoingPages = computed(() => {
  return Math.max(1, Math.ceil(ongoingVotes.value.length / PAGE_SIZE))
})

const totalEndedPages = computed(() => {
  return Math.max(1, Math.ceil(endedVotes.value.length / PAGE_SIZE))
})

// 마이페이지 메인 섹션 (내 정보 / 내 저장 / 지난 결과 내역)
const MAIN_SECTION = {
  INFO: 'info',
  SAVED: 'saved',
  HISTORY: 'history'
}

const activeMainSection = ref(MAIN_SECTION.INFO)

// 마이페이지 내 하위 탭 (내 정보 / 개인정보동의 / 회원탈퇴)
const MY_SUB_SECTION = {
  INFO: 'info',
  PRIVACY: 'privacy',
  WITHDRAW: 'withdraw'
}

const activeMySubSection = ref(MY_SUB_SECTION.INFO)

// 마이페이지 사이드 메뉴 토글 (펼치기/접기)
const isMyMenuOpen = ref(true)

// 내 저장 섹션: 정렬·페이지네이션
const placeSortOrder = ref('desc')
const placePage = ref(1)
const PLACE_PAGE_SIZE = 12

// 저장한 필터 페이지네이션 (Figma 47:3141)
const filterPage = ref(1)
const FILTER_PAGE_SIZE = 6
const totalSavedFilterPages = computed(() =>
  Math.max(1, Math.ceil(savedFilters.value.length / FILTER_PAGE_SIZE))
)
const pagedSavedFilters = computed(() => {
  const start = (filterPage.value - 1) * FILTER_PAGE_SIZE
  return savedFilters.value.slice(start, start + FILTER_PAGE_SIZE)
})
function setSavedFilterPage(n) {
  const total = totalSavedFilterPages.value
  filterPage.value = Math.max(1, Math.min(n, total))
}

// 삭제 확인 팝업 (별 옆에 표시)
const confirmDeleteFilterId = ref(null)

// 저장한 필터 수정 모달 (Figma 47:3112) / 추가 모달
const editingFilter = ref(null)
const showAddFilterModal = ref(false)
const editForm = ref({
  name: '',
  distance: 300,
  foodTypes: [],
  openOnly: false
})
const DISTANCE_OPTIONS = [300, 800, 1200]
const FOOD_TYPE_ENTRIES = Object.entries(FOOD_TYPE_LABELS)
const editFormSubmitting = ref(false)

function openAddFilterModal() {
  closeDeleteConfirm()
  showAddFilterModal.value = true
  editingFilter.value = null
  editForm.value = {
    name: '',
    distance: 300,
    foodTypes: ['korean'],
    openOnly: false
  }
}

function openEditModal(sf) {
  closeDeleteConfirm()
  showAddFilterModal.value = false
  editingFilter.value = sf
  editForm.value = {
    name: sf.name || '',
    distance: sf.distance ?? 300,
    foodTypes: Array.isArray(sf.foodTypes) ? [...sf.foodTypes] : [],
    openOnly: !!sf.openOnly
  }
}

function closeEditModal() {
  editingFilter.value = null
  showAddFilterModal.value = false
}

function toggleEditFoodType(ftId) {
  const arr = editForm.value.foodTypes
  const idx = arr.indexOf(ftId)
  if (idx === -1) arr.push(ftId)
  else arr.splice(idx, 1)
}

function isEditFoodTypeSelected(ftId) {
  return editForm.value.foodTypes.includes(ftId)
}

function isAllEditFoodTypesSelected() {
  const selected = editForm.value.foodTypes
  const allIds = FOOD_TYPE_ENTRIES.map(([id]) => id)
  return allIds.length > 0 && allIds.every((id) => selected.includes(id))
}

function toggleAllEditFoodTypes() {
  if (isAllEditFoodTypesSelected()) {
    editForm.value.foodTypes = []
  } else {
    editForm.value.foodTypes = FOOD_TYPE_ENTRIES.map(([id]) => id)
  }
}

function selectAllEditFoodTypes() {
  editForm.value.foodTypes = FOOD_TYPE_ENTRIES.map(([id]) => id)
}

async function saveEditFilter() {
  if (editFormSubmitting.value) return
  const name = (editForm.value.name || '').trim() || '새 필터'
  const isEdit = !!editingFilter.value
  const id = editingFilter.value?.id
  if (isEdit && !id) return
  editFormSubmitting.value = true
  try {
    const token = getToken()
    if (!token) return
    if (isEdit) {
      const res = await fetch(`/api/saved-filters/${id}`, {
        method: 'PUT',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': token
        },
        body: JSON.stringify({
          name,
          distance: editForm.value.distance,
          foodTypes: editForm.value.foodTypes,
          openOnly: editForm.value.openOnly
        })
      })
      if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        alert(data.error || '수정에 실패했어요.')
        return
      }
    } else {
      const res = await fetch('/api/saved-filters', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': token
        },
        body: JSON.stringify({
          name: name.substring(0, 12),
          distance: editForm.value.distance,
          foodTypes: editForm.value.foodTypes,
          openOnly: editForm.value.openOnly
        })
      })
      if (!res.ok) {
        const data = await res.json().catch(() => ({}))
        alert(data.error || '저장에 실패했어요.')
        return
      }
    }
    fetchSavedFilters()
    closeEditModal()
  } finally {
    editFormSubmitting.value = false
  }
}

const sortedSavedPlaces = computed(() => {
  const list = [...(memberFavorites.value || [])]
  const order = placeSortOrder.value === 'asc' ? 1 : -1
  list.sort((a, b) => {
    const at = (a.favoritedAt && new Date(a.favoritedAt).getTime()) || 0
    const bt = (b.favoritedAt && new Date(b.favoritedAt).getTime()) || 0
    return order * (at - bt)
  })
  return list
})

const totalSavedPlacePages = computed(() =>
  Math.max(1, Math.ceil(sortedSavedPlaces.value.length / PLACE_PAGE_SIZE))
)

const pagedSavedPlaces = computed(() => {
  const start = (placePage.value - 1) * PLACE_PAGE_SIZE
  return sortedSavedPlaces.value.slice(start, start + PLACE_PAGE_SIZE)
})

function setSavedPlacePage(p) {
  const n = Number(p)
  if (Number.isNaN(n) || n < 1 || n > totalSavedPlacePages.value) return
  placePage.value = n
}

function getSavedFavoriteImage(item) {
  if (item?.photoName) {
    const name = Array.isArray(item.photoName) ? item.photoName[0] : item.photoName
    if (name) return `/api/restaurants/photo?name=${encodeURIComponent(name)}`
  }
  return defaultThumbnail
}

const pagedOngoingVotes = computed(() => {
  const start = (ongoingPage.value - 1) * PAGE_SIZE
  return ongoingVotes.value.slice(start, start + PAGE_SIZE)
})

const sortedEndedVotes = computed(() => {
  return [...endedVotes.value].sort((a, b) => {
    const aTime = new Date(a.endedAt || a.createdAt || 0).getTime()
    const bTime = new Date(b.endedAt || b.createdAt || 0).getTime()
    return bTime - aTime
  })
})

const pagedEndedVotes = computed(() => {
  const start = (endedPage.value - 1) * PAGE_SIZE
  return sortedEndedVotes.value.slice(start, start + PAGE_SIZE)
})

const combinedRecentResults = computed(() => {
  const voteItems = sortedEndedVotes.value.map((v) => ({
    type: 'vote',
    id: v.id,
    title: v.title,
    createdAt: v.endedAt || v.createdAt
  }))

  const rouletteItems = (rouletteHistory.value || []).map((r) => ({
    type: 'roulette',
    id: r.id,
    winnerName: r.winnerName,
    shareData: r.shareData,
    createdAt: r.createdAt
  }))

  return [...voteItems, ...rouletteItems]
    .filter((item) => item.createdAt)
    .sort((a, b) => {
      const aTime = new Date(a.createdAt || 0).getTime()
      const bTime = new Date(b.createdAt || 0).getTime()
      return bTime - aTime
    })
})

const limitedRecentResults = computed(() => {
  return combinedRecentResults.value.slice(0, 3)
})

// 지난 결과 내역 전용 페이지네이션 / 필터
const HISTORY_PAGE_SIZE = 10
const historyPage = ref(1)
const historyFilter = ref('all') // 'all' | 'vote' | 'roulette'

const filteredHistoryResults = computed(() => {
  if (historyFilter.value === 'vote') {
    return combinedRecentResults.value.filter((item) => item.type === 'vote')
  }
  if (historyFilter.value === 'roulette') {
    return combinedRecentResults.value.filter((item) => item.type === 'roulette')
  }
  return combinedRecentResults.value
})

const totalHistoryPages = computed(() => {
  return Math.max(1, Math.ceil(filteredHistoryResults.value.length / HISTORY_PAGE_SIZE))
})

const pagedHistoryResults = computed(() => {
  const start = (historyPage.value - 1) * HISTORY_PAGE_SIZE
  return filteredHistoryResults.value.slice(start, start + HISTORY_PAGE_SIZE)
})

watch(historyFilter, () => {
  historyPage.value = 1
})

const setHistoryPage = (page) => {
  const target = Number(page)
  if (Number.isNaN(target)) return
  if (target < 1 || target > totalHistoryPages.value) return
  historyPage.value = target
}

const goPrevHistoryPage = () => {
  if (historyPage.value > 1) {
    historyPage.value -= 1
  }
}

const goNextHistoryPage = () => {
  if (historyPage.value < totalHistoryPages.value) {
    historyPage.value += 1
  }
}

function applyProfileIndex(idx) {
  const n = typeof idx === 'number' ? idx : parseInt(idx, 10)
  if (!Number.isNaN(n) && n >= 0 && n < profileOptions.length) {
    currentProfileImage.value = profileOptions[n]
    setAuthProfileIndex(n)
  }
}

async function fetchMe() {
  const token = getToken()
  if (!token) {
    emailLoadError.value = '로그인이 필요합니다.'
    return
  }
  try {
    const res = await fetch('/api/auth/me', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (!res.ok) {
      const data = await res.json().catch(() => ({}))
      emailLoadError.value = data.message || '회원 정보를 불러오지 못했습니다.'
      return
    }
    const data = await res.json()
    memberEmail.value = data.email || ''
    const idx = data.profileImageIndex ?? data.profile_image_index
    applyProfileIndex(idx)
  } catch (err) {
    emailLoadError.value = '네트워크 오류가 발생했습니다.'
  }
}

async function fetchOngoingVotes() {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch('/api/votes/me/ongoing', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (res.ok) {
      const data = await res.json()
      ongoingVotes.value = Array.isArray(data) ? data : []
      applyOngoingVoteTimers()
    } else {
      ongoingVotes.value = []
    }
  } catch {
    ongoingVotes.value = []
  }
}

async function fetchEndedVotes() {
  const token = getToken()
  if (!token) return
  try {
    const res = await fetch('/api/votes/me/ended', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (res.ok) {
      const data = await res.json()
      endedVotes.value = Array.isArray(data) ? data : []
    } else {
      endedVotes.value = []
    }
  } catch {
    endedVotes.value = []
  }
}

async function loadRouletteHistory() {
  const token = getToken()
  if (!token) {
    rouletteHistory.value = []
    return
  }
  try {
    const res = await fetch('/api/roulette-history/me', {
      headers: {
        'X-Auth-Token': token
      },
      cache: 'no-store'
    })
    if (!res.ok) {
      rouletteHistory.value = []
      return
    }
    const data = await res.json()
    rouletteHistory.value = Array.isArray(data) ? data : []
  } catch {
    rouletteHistory.value = []
  }
}

async function fetchMemberFavorites() {
  const token = getToken()
  if (!token) {
    memberFavorites.value = []
    return
  }
  try {
    const res = await fetch('/api/favorites/me', {
      headers: {
        'X-Auth-Token': token
      },
      cache: 'no-store'
    })
    if (!res.ok) {
      memberFavorites.value = []
      return
    }
    const data = await res.json()
    memberFavorites.value = Array.isArray(data) ? data : []
  } catch {
    memberFavorites.value = []
  }
}

async function fetchSavedFilters() {
  const token = getToken()
  if (!token) {
    savedFilters.value = []
    return
  }
  try {
    const res = await fetch('/api/saved-filters/me', {
      headers: { 'X-Auth-Token': token },
      cache: 'no-store'
    })
    if (!res.ok) {
      savedFilters.value = []
      return
    }
    const data = await res.json()
    savedFilters.value = Array.isArray(data) ? data : []
  } catch {
    savedFilters.value = []
  }
}

function openDeleteConfirm(id) {
  if (confirmDeleteFilterId.value === id) {
    closeDeleteConfirm()
  } else {
    confirmDeleteFilterId.value = id
  }
}

function closeDeleteConfirm() {
  confirmDeleteFilterId.value = null
}

function doDeleteSavedFilter(id) {
  deleteSavedFilter(id)
  closeDeleteConfirm()
}

async function deleteSavedFilter(id) {
  const token = getToken()
  if (!token || !id) return
  try {
    const res = await fetch(`/api/saved-filters/${id}`, {
      method: 'DELETE',
      headers: { 'X-Auth-Token': token }
    })
    if (res.ok) fetchSavedFilters()
  } catch {
    // ignore
  }
}

function getSavedFilterDistanceLabel(distance) {
  return DISTANCE_LABELS[distance] || `${distance}m`
}

function getSavedFilterFoodTypeLabel(id) {
  return FOOD_TYPE_LABELS[id] || id
}

function getOngoingTimerDisplay(vote) {
  if (!vote || !vote.timer || vote.timer === 'none' || !vote.createdAt) return null

  const durationMs =
    {
      '10min': 10 * 60 * 1000,
      '30min': 30 * 60 * 1000,
      '1hour': 60 * 60 * 1000
    }[vote.timer] || 0

  if (!durationMs) return null

  const endTime = new Date(vote.createdAt).getTime() + durationMs
  const remaining = endTime - now.value
  if (!Number.isFinite(remaining)) return null

  if (remaining <= 0) {
    return {
      hours: '00',
      minutes: '00',
      seconds: '00'
    }
  }

  const totalSec = Math.floor(remaining / 1000)
  return {
    hours: String(Math.floor(totalSec / 3600)).padStart(2, '0'),
    minutes: String(Math.floor((totalSec % 3600) / 60)).padStart(2, '0'),
    seconds: String(totalSec % 60).padStart(2, '0')
  }
}

watch(ongoingVotes, () => {
  ongoingPage.value = 1
})

watch(endedVotes, () => {
  endedPage.value = 1
})

const setOngoingPage = (page) => {
  const target = Number(page)
  if (Number.isNaN(target)) return
  if (target < 1 || target > totalOngoingPages.value) return
  ongoingPage.value = target
}

const goPrevOngoingPage = () => {
  if (ongoingPage.value > 1) {
    setOngoingPage(ongoingPage.value - 1)
  }
}

const goNextOngoingPage = () => {
  if (ongoingPage.value < totalOngoingPages.value) {
    setOngoingPage(ongoingPage.value + 1)
  }
}

const ongoingPageItems = computed(() => {
  const total = totalOngoingPages.value
  const items = []

  // 총 페이지가 10 이하이면 모두 노출
  if (total <= 10) {
    for (let i = 1; i <= total; i += 1) {
      items.push({ type: 'number', value: i, label: String(i), key: `page-${i}` })
    }
    return items
  }

  // 총 페이지가 10을 초과하면:
  // 1~8 페이지, 9번째는 ..., 10번째는 마지막 페이지
  for (let i = 1; i <= 8; i += 1) {
    items.push({ type: 'number', value: i, label: String(i), key: `page-${i}` })
  }
  items.push({ type: 'ellipsis', value: null, label: '...', key: 'page-ellipsis' })
  items.push({ type: 'number', value: total, label: String(total), key: `page-${total}` })

  return items
})

const goPrevEndedPage = () => {
  if (endedPage.value > 1) {
    endedPage.value -= 1
  }
}

const goNextEndedPage = () => {
  if (endedPage.value < totalEndedPages.value) {
    endedPage.value += 1
  }
}

function formatVoteDate(isoString) {
  if (!isoString) return ''
  const d = new Date(isoString)
  return d.toLocaleString('ko-KR', {
    year: 'numeric',
    month: 'short',
    day: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
    hour12: true
  })
}

function syncSectionFromRoute() {
  if (route.path !== '/my') return
  const { section, filter } = route.query

  if (section === 'saved') {
    activeMainSection.value = MAIN_SECTION.SAVED
  } else if (section === 'history') {
    activeMainSection.value = MAIN_SECTION.HISTORY
  } else {
    activeMainSection.value = MAIN_SECTION.INFO
  }

  if (filter === 'vote' || filter === 'roulette') {
    historyFilter.value = filter
  } else {
    historyFilter.value = 'all'
  }
}

// 회원 탈퇴 (비밀번호는 한 번만 입력, 서버 DB에서 검증)
const withdrawPassword = ref('')
const withdrawReasonCode = ref('')
const withdrawAgree = ref(false)
const withdrawError = ref('')
const withdrawSubmitting = ref(false)

const WITHDRAW_REASONS = [
  { value: '', label: '선택하지 않음' },
  { value: 'no_use', label: '서비스를 잘 사용하지 않음' },
  { value: 'another_service', label: '다른 서비스를 이용할 예정' },
  { value: 'privacy', label: '개인정보가 걱정됨' },
  { value: 'unsatisfied', label: '서비스가 마음에 들지 않음' },
  { value: 'other', label: '기타' }
]

const canSubmitWithdraw = computed(() =>
  withdrawPassword.value.trim().length > 0 && withdrawAgree.value
)

async function submitWithdraw() {
  if (!canSubmitWithdraw.value || withdrawSubmitting.value) return
  withdrawError.value = ''
  withdrawSubmitting.value = true
  const token = getToken()
  if (!token) {
    withdrawError.value = '로그인이 필요합니다.'
    withdrawSubmitting.value = false
    return
  }
  try {
    const res = await fetch('/api/auth/withdraw', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        'X-Auth-Token': token
      },
      body: JSON.stringify({
        password: withdrawPassword.value,
        reasonCode: withdrawReasonCode.value || undefined
      }),
      cache: 'no-store'
    })
    const data = res.ok ? {} : await res.json().catch(() => ({}))
    if (!res.ok) {
      withdrawError.value = data.message || '탈퇴 처리에 실패했습니다. 비밀번호를 확인해주세요.'
      withdrawSubmitting.value = false
      return
    }
    clearAuth()
    resetOnLogout()
    withdrawPassword.value = ''
    withdrawReasonCode.value = ''
    withdrawAgree.value = false
    router.replace('/')
  } catch {
    withdrawError.value = '네트워크 오류가 발생했습니다.'
  } finally {
    withdrawSubmitting.value = false
  }
}

watch(
  totalSavedFilterPages,
  (total) => {
    if (filterPage.value > total) {
      filterPage.value = Math.max(1, total)
    }
  }
)

watch(
  () => route.fullPath,
  (fullPath) => {
    if (route.path === '/my') {
      applyProfileIndex(authProfileIndex.value)
      fetchMe()
      fetchOngoingVotes()
      fetchEndedVotes()
      loadRouletteHistory()
      fetchMemberFavorites()
      fetchSavedFilters()
      syncSectionFromRoute()
    }
  },
  { immediate: true }
)

const handlePageshow = () => {
  if (route.path === '/my') {
    fetchMe()
    fetchOngoingVotes()
    fetchEndedVotes()
    loadRouletteHistory()
    fetchMemberFavorites()
    fetchSavedFilters()
  }
}

onMounted(() => {
  timerInterval = setInterval(() => {
    now.value = Date.now()
    applyOngoingVoteTimers()
  }, 1000)
  window.addEventListener('pageshow', handlePageshow)
})

onUnmounted(() => {
  if (timerInterval) {
    clearInterval(timerInterval)
    timerInterval = null
  }
  window.removeEventListener('pageshow', handlePageshow)
})

// 사이드 프로필에 표시할 이름: 이메일에서 @ 앞부분 / @이후
const profileDisplayParts = computed(() => {
  const email = memberEmail.value
  if (!email) return { name: '-', domain: '' }
  const at = email.indexOf('@')
  if (at < 0) return { name: email, domain: '' }
  return { name: email.slice(0, at), domain: email.slice(at) }
})

const openProfileChangeModal = () => {
  showProfileChangeModal.value = true
}

const closeProfileChangeModal = () => {
  showProfileChangeModal.value = false
}

const onProfileConfirm = async (selectedIndex) => {
  const index = typeof selectedIndex === 'number' ? selectedIndex : profileOptions.indexOf(selectedIndex)
  if (import.meta.env.DEV) {
    console.log('[MyPage] onProfileConfirm 호출', { selectedIndex, index, profileOptionsLength: profileOptions.length })
  }
  if (index < 0 || index >= profileOptions.length) {
    showProfileChangeModal.value = false
    return
  }
  const selected = profileOptions[index]
  currentProfileImage.value = selected
  showProfileChangeModal.value = false

  const token = getToken()
  if (import.meta.env.DEV) {
    console.log('[MyPage] PATCH 요청 예정', { token: token ? `${token.slice(0, 4)}...` : null, profileImageIndex: index })
  }
  if (token) {
    try {
      const res = await fetch('/api/auth/me', {
        method: 'PATCH',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': token
        },
        body: JSON.stringify({ profileImageIndex: index }),
        cache: 'no-store'
      })
      const data = await res.json().catch(() => ({}))
      if (import.meta.env.DEV) {
        console.log('[MyPage] PATCH 응답', { ok: res.ok, status: res.status, data })
      }
      if (res.ok) {
        emailLoadError.value = ''
        const idx = data.profileImageIndex ?? data.profile_image_index
        applyProfileIndex(idx)
      } else {
        emailLoadError.value = '프로필 저장에 실패했습니다. 다시 시도해주세요.'
      }
    } catch (err) {
      if (import.meta.env.DEV) {
        console.error('[MyPage] PATCH 실패', err)
      }
      emailLoadError.value = '프로필 저장에 실패했습니다. 다시 시도해주세요.'
    }
  }
}

const currentProfileIndex = computed(() => {
  const idx = profileOptions.indexOf(currentProfileImage.value)
  return idx >= 0 ? idx : 0
})
</script>

<template>
  <div class="mypage">
    <aside class="side-menu" aria-label="마이페이지 메뉴">
      <div class="side-menu-contents">
        <div class="profile">
          <div class="profile-thumbnail">
            <img :src="currentProfileImage" alt="" class="profile-thumbnail-img" aria-hidden="true" />
          </div>
          <p class="profile-name">
            <template v-if="memberEmail">{{ profileDisplayParts.name }}<br />{{ profileDisplayParts.domain }}</template>
            <template v-else-if="emailLoadError">{{ emailLoadError }}</template>
            <template v-else>로딩 중...</template>
          </p>
        </div>
        <div class="divider" aria-hidden="true"></div>
        <div
          class="menu-1depth menu-1depth--with-arrow menu-1depth--clickable"
          role="button"
          tabindex="0"
          @click.stop="isMyMenuOpen = !isMyMenuOpen"
        >
          <p class="menu-1depth-title">마이페이지</p>
          <span
            class="menu-arrow"
            :class="{ 'menu-arrow--collapsed': !isMyMenuOpen }"
            aria-hidden="true"
          >
            ▼
          </span>
        </div>
        <nav
          class="sub-menu"
          aria-label="마이페이지 하위 메뉴"
          v-show="isMyMenuOpen"
        >
          <div
            class="sub-menu-item"
            :class="{
              'sub-menu-item--selected':
                activeMainSection === MAIN_SECTION.INFO && activeMySubSection === MY_SUB_SECTION.INFO
            }"
            role="button"
            tabindex="0"
            @click.stop="
              activeMySubSection = MY_SUB_SECTION.INFO;
              activeMainSection = MAIN_SECTION.INFO
            "
          >
            내 정보
          </div>
          <div
            class="sub-menu-item"
            :class="{
              'sub-menu-item--selected':
                activeMainSection === MAIN_SECTION.INFO && activeMySubSection === MY_SUB_SECTION.PRIVACY
            }"
            role="button"
            tabindex="0"
            @click.stop="
              activeMySubSection = MY_SUB_SECTION.PRIVACY;
              activeMainSection = MAIN_SECTION.INFO
            "
          >
            개인정보동의
          </div>
          <div
            class="sub-menu-item"
            :class="{
              'sub-menu-item--selected':
                activeMainSection === MAIN_SECTION.INFO && activeMySubSection === MY_SUB_SECTION.WITHDRAW
            }"
            role="button"
            tabindex="0"
            @click.stop="
              activeMySubSection = MY_SUB_SECTION.WITHDRAW;
              activeMainSection = MAIN_SECTION.INFO
            "
          >
            회원탈퇴
          </div>
        </nav>
        <button
          type="button"
          class="menu-1depth menu-1depth--history"
          :class="{ 'menu-1depth--selected': activeMainSection === MAIN_SECTION.SAVED }"
          @click.stop="
            activeMainSection = MAIN_SECTION.SAVED;
            router.replace({ path: '/my', query: { section: 'saved' } })
          "
        >
          <p class="menu-1depth-title">내 저장</p>
        </button>
        <button
          type="button"
          class="menu-1depth menu-1depth--history"
          :class="{ 'menu-1depth--selected': activeMainSection === MAIN_SECTION.HISTORY }"
          @click.stop="
            activeMainSection = MAIN_SECTION.HISTORY;
            router.replace({
              path: '/my',
              query: {
                section: 'history',
                filter: historyFilter
              }
            })
          "
        >
          <p class="menu-1depth-title">지난 결과 내역</p>
        </button>
      </div>
    </aside>

    <main class="mypage-main">
      <!-- 내 정보 / 개인정보동의 / 회원탈퇴 탭 섹션 -->
      <template v-if="activeMainSection === MAIN_SECTION.INFO">
        <!-- 내 정보 (기존 마이페이지) -->
        <template v-if="activeMySubSection === MY_SUB_SECTION.INFO">
          <section class="section" aria-labelledby="section-info-title">
            <h2 id="section-info-title" class="section-title">내 정보</h2>
            <div class="section-contents section-contents--row">
              <div class="thumbnail-wrap">
                <div class="thumbnail thumbnail--large">
                  <img :src="currentProfileImage" alt="" class="thumbnail-img" aria-hidden="true" />
                  <button
                    type="button"
                    class="thumbnail-edit-btn"
                    aria-label="프로필 사진 변경"
                    @click="openProfileChangeModal"
                  >
                    <img :src="profileEditIcon" alt="" class="thumbnail-edit-icon" width="32" height="32" aria-hidden="true" />
                  </button>
                </div>
              </div>
              <div class="account">
                <div class="account-row">
                  <span class="account-label">이메일</span>
                  <span class="account-value">{{ memberEmail || (emailLoadError || '로딩 중...') }}</span>
                </div>
                <div class="account-row">
                  <span class="account-label">비밀번호</span>
                  <span class="account-value">********</span>
                  <button type="button" class="btn-outlined">비밀번호 변경</button>
                </div>
              </div>
            </div>
          </section>

          <section class="section" aria-labelledby="section-vote-title">
            <h2 id="section-vote-title" class="section-title">현재 진행중인 투표</h2>
            <div v-if="ongoingVotes.length === 0" class="section-contents section-contents--empty">
              <p class="empty-text">진행중인 투표가 없어요</p>
            </div>
            <div v-else class="section-contents section-contents--list section-contents--ongoing-votes">
              <router-link
                v-for="v in pagedOngoingVotes"
                :key="v.id"
                :to="`/vote/${v.id}`"
                class="vote-list-item"
              >
                <div class="vote-list-main">
                  <span class="result-label result-label--vote">투표</span>
                  <span class="vote-list-title">{{ v.title }}</span>
                </div>
                <div class="vote-list-meta">
                  <div
                    v-if="getOngoingTimerDisplay(v)"
                    class="vote-list-timer-row"
                  >
                    <span class="vote-list-timer-label">종료까지</span>
                    <span class="vote-list-timer">
                      {{ getOngoingTimerDisplay(v).hours }}시간
                      {{ getOngoingTimerDisplay(v).minutes }}분
                      {{ getOngoingTimerDisplay(v).seconds }}초 남음
                    </span>
                  </div>
                  <span class="vote-list-date">{{ formatVoteDate(v.createdAt) }}</span>
                </div>
              </router-link>
            </div>
            <div v-if="ongoingVotes.length > PAGE_SIZE" class="pagination">
              <button
                type="button"
                class="pagination-arrow"
                aria-label="이전"
                :disabled="ongoingPage === 1"
                @click="goPrevOngoingPage"
              >
                ‹
              </button>
              <button
                v-for="item in ongoingPageItems"
                :key="item.key"
                type="button"
                class="pagination-page"
                :class="{
                  'pagination-page--current': item.type === 'number' && item.value === ongoingPage,
                  'pagination-page--ellipsis': item.type === 'ellipsis'
                }"
                :disabled="item.type === 'ellipsis'"
                @click="item.type === 'number' ? setOngoingPage(item.value) : null"
              >
                {{ item.label }}
              </button>
              <button
                type="button"
                class="pagination-arrow"
                aria-label="다음"
                :disabled="ongoingPage === totalOngoingPages"
                @click="goNextOngoingPage"
              >
                ›
              </button>
            </div>
          </section>

          <section class="section" aria-labelledby="section-fav-title">
            <div class="section-head">
              <h2 id="section-fav-title" class="section-title">최근 즐겨찾기 한 장소</h2>
              <button
                type="button"
                class="btn-ghost"
                @click="
                  activeMainSection = MAIN_SECTION.SAVED;
                  router.replace({ path: '/my', query: { section: 'saved' } })
                "
              >
                전체보기
              </button>
            </div>
            <div v-if="latestFavorites.length === 0" class="section-contents section-contents--empty">
              <p class="empty-text">즐겨찾기한 장소가 없어요</p>
            </div>
            <div v-else class="section-contents">
              <ul class="favorites-list">
                <li
                  v-for="item in latestFavorites"
                  :key="item.id"
                  class="favorite-item"
                >
                  <div class="favorite-card">
                    <div class="favorite-thumb-wrap">
                      <img
                        :src="getFavoriteImage(item)"
                        :alt="item.name"
                        class="favorite-thumb"
                        @error="(e) => { e.target.src = defaultThumbnail }"
                      />
                      <button
                        type="button"
                        class="favorite-heart-button"
                        aria-label="즐겨찾기 해제"
                        @click.stop="handleToggleFavoriteFromMyPage(item)"
                      >
                        <svg
                          class="favorite-heart-icon"
                          viewBox="0 0 24 24"
                          aria-hidden="true"
                        >
                          <path
                            class="favorite-heart-path"
                            d="M12 20.25c-.32 0-.64-.1-.9-.3-.76-.56-1.45-1.09-2.08-1.56-2.53-1.92-4.42-3.36-4.42-5.89C4.6 9.5 6.1 8 7.92 8c1.12 0 2.12.52 2.78 1.39L12 10.9l1.3-1.51C13.96 8.52 14.96 8 16.08 8 17.9 8 19.4 9.5 19.4 12.5c0 2.53-1.89 3.97-4.42 5.89-.63.47-1.32 1-2.08 1.56-.26.2-.58.3-.9.3Z"
                          />
                        </svg>
                      </button>
                    </div>
                    <div class="favorite-info">
                      <div class="favorite-header-row">
                        <p class="favorite-name">
                          {{ item.name }}
                        </p>
                        <button
                          type="button"
                          class="favorite-external-btn"
                          aria-label="지도에서 보기"
                          @click="openFavoriteExternal(item)"
                        >
                          <img
                            :src="externalLinkIcon"
                            alt=""
                            class="favorite-external-icon"
                            aria-hidden="true"
                          />
                        </button>
                      </div>
                      <p class="favorite-category">
                        {{ getFavoriteCategory(item) }}
                      </p>
                    </div>
                  </div>
                </li>
              </ul>
            </div>
          </section>

          <section class="section section--bottom-space" aria-labelledby="section-result-title">
            <div class="section-head">
              <h2 id="section-result-title" class="section-title">최근 진행한 결과</h2>
              <button
                type="button"
                class="btn-ghost"
                @click="
                  activeMainSection = MAIN_SECTION.HISTORY;
                  router.replace({
                    path: '/my',
                    query: {
                      section: 'history',
                      filter: historyFilter
                    }
                  })
                "
              >
                전체보기
              </button>
            </div>
            <div
              v-if="limitedRecentResults.length === 0"
              class="section-contents section-contents--empty"
            >
              <p class="empty-text">진행한 결과가 없어요</p>
            </div>
            <div v-else class="section-contents section-contents--list">
              <router-link
                v-for="item in limitedRecentResults"
                :key="`${item.type}-${item.id}-${item.createdAt}`"
                :to="
                  item.type === 'vote'
                    ? `/vote/${item.id}`
                    : { name: 'rouletteShare', query: { data: item.shareData, fromHistory: 'true' } }
                "
                class="vote-list-item"
              >
                <div class="vote-list-main">
                  <span
                    class="result-label"
                    :class="item.type === 'vote' ? 'result-label--vote' : 'result-label--roulette'"
                  >
                    {{ item.type === 'vote' ? '투표' : '룰렛' }}
                  </span>
                  <span class="vote-list-title">
                    {{ item.type === 'vote' ? item.title : '오늘 점심은 여기로 결정!' }}
                  </span>
                </div>
                <span class="vote-list-date">{{ formatVoteDate(item.createdAt) }}</span>
              </router-link>
            </div>
          </section>
        </template>

        <!-- 개인정보동의 페이지 -->
        <template v-else-if="activeMySubSection === MY_SUB_SECTION.PRIVACY">
          <section class="section section--bottom-space" aria-labelledby="section-privacy-title">
            <h2 id="section-privacy-title" class="section-title">개인정보 동의</h2>
            <div class="section-contents">
              <p class="account-value">
                서비스 이용을 위해 동의한 개인정보 처리 및 수집 항목을 확인할 수 있는 페이지입니다.
              </p>
              <p class="account-value">
                추후 실제 약관 내용과 동의 내역을 조회·변경할 수 있도록 확장할 수 있어요.
              </p>
            </div>
          </section>
        </template>

        <!-- 회원탈퇴 페이지 -->
        <template v-else-if="activeMySubSection === MY_SUB_SECTION.WITHDRAW">
          <section class="section section--bottom-space withdraw-section" aria-labelledby="section-withdraw-title">
            <h2 id="section-withdraw-title" class="section-title">회원 탈퇴</h2>

            <div class="withdraw-notice">
              <p class="withdraw-notice-text">
                탈퇴 시 계정 정보, 즐겨찾기, 투표·룰렛 기록 등 모든 데이터가 삭제되며 <strong>복구할 수 없습니다</strong>.
              </p>
              <p class="withdraw-notice-text">
                탈퇴를 진행하려면 아래에서 비밀번호를 입력하고 안내에 동의해 주세요.
              </p>
            </div>

            <div class="section-contents withdraw-form">
              <div v-if="withdrawError" class="withdraw-alert" role="alert">
                <span class="withdraw-alert-icon" aria-hidden="true">!</span>
                <p class="withdraw-alert-message">{{ withdrawError }}</p>
              </div>

              <div class="account-row withdraw-row">
                <label for="withdraw-password" class="account-label">비밀번호 확인</label>
                <input
                  id="withdraw-password"
                  v-model="withdrawPassword"
                  type="password"
                  class="withdraw-input"
                  placeholder="현재 비밀번호를 입력하세요"
                  autocomplete="current-password"
                  :disabled="withdrawSubmitting"
                />
              </div>

              <div class="account-row withdraw-row">
                <label for="withdraw-reason" class="account-label">탈퇴 사유</label>
                <select
                  id="withdraw-reason"
                  v-model="withdrawReasonCode"
                  class="withdraw-select"
                  :disabled="withdrawSubmitting"
                >
                  <option
                    v-for="opt in WITHDRAW_REASONS"
                    :key="opt.value"
                    :value="opt.value"
                  >
                    {{ opt.label }}
                  </option>
                </select>
              </div>

              <div class="withdraw-agree-row">
                <input
                  id="withdraw-agree"
                  v-model="withdrawAgree"
                  type="checkbox"
                  class="withdraw-checkbox"
                  :disabled="withdrawSubmitting"
                />
                <label for="withdraw-agree" class="withdraw-agree-label">
                  탈퇴 시 위 내용을 확인했으며, 삭제·복구 불가에 동의합니다.
                </label>
              </div>

              <div class="withdraw-actions">
                <button
                  type="button"
                  class="btn-withdraw"
                  :disabled="!canSubmitWithdraw || withdrawSubmitting"
                  @click="submitWithdraw"
                >
                  {{ withdrawSubmitting ? '처리 중...' : '회원 탈퇴하기' }}
                </button>
              </div>
            </div>
          </section>
        </template>
      </template>

      <!-- 내 저장 섹션 (저장한 필터 + 저장한 장소) - Figma 47:3141 -->
      <template v-else-if="activeMainSection === MAIN_SECTION.SAVED">
        <section class="saved-section favorite-list" aria-labelledby="section-filter-title" data-name="favoriteList" data-node-id="47:3141">
          <div class="saved-section-head list-title" data-name="listTitle" data-node-id="I47:3141;47:3950">
            <h2 id="section-filter-title" class="saved-section-title list-title__heading" data-node-id="I47:3141;47:3950;47:3624">저장한 필터</h2>
            <button
              type="button"
              class="saved-section-btn list-title__button"
              data-name="button"
              data-node-id="I47:3141;47:3950;47:3625"
              @click="openAddFilterModal"
            >
              <span class="list-title__button-inner" data-node-id="I47:3141;47:3950;47:3626">추가하기</span>
            </button>
          </div>
          <div v-if="savedFilters.length === 0" class="saved-section-contents saved-section-contents--empty">
            <p class="saved-section-empty-text">저장한 필터가 없어요</p>
          </div>
          <div v-else class="saved-filter-section-body">
            <div class="saved-filter-list-contents list-contents--filters" data-name="listContents">
              <div
                v-for="sf in pagedSavedFilters"
                :key="sf.id"
                class="saved-filter-card-figma"
              data-name="filterCard"
              data-node-id="47:3920"
            >
              <div class="saved-filter-card-figma__inner">
                <div class="saved-filter-card-figma__header">
                  <p class="saved-filter-card-figma__name">{{ sf.name }}</p>
                  <div class="saved-filter-card-figma__actions">
                    <div class="saved-filter-card-figma__star-wrap">
                      <button
                        type="button"
                        class="saved-filter-card-figma__star-btn"
                        aria-label="저장 해제(삭제)"
                        aria-expanded="confirmDeleteFilterId === sf.id"
                        @click="openDeleteConfirm(sf.id)"
                      >
                        <img :src="iconStarFilled" alt="" class="saved-filter-card-figma__star" aria-hidden="true" />
                      </button>
                      <div
                        v-if="confirmDeleteFilterId === sf.id"
                        class="saved-filter-card-figma__confirm-popup"
                        role="dialog"
                        aria-label="삭제 확인"
                      >
                        <p class="saved-filter-card-figma__confirm-text">이 필터를 삭제하시겠습니까?</p>
                        <div class="saved-filter-card-figma__confirm-btns">
                          <button type="button" class="saved-filter-card-figma__confirm-cancel" @click="closeDeleteConfirm">취소</button>
                          <button type="button" class="saved-filter-card-figma__confirm-delete" @click="doDeleteSavedFilter(sf.id)">삭제</button>
                        </div>
                      </div>
                    </div>
                    <button
                      type="button"
                      class="saved-filter-card-figma__edit"
                      aria-label="필터 수정"
                      @click="openEditModal(sf)"
                    >
                      <img :src="iconEdit" alt="" class="saved-filter-card-figma__edit-icon" aria-hidden="true" />
                    </button>
                  </div>
                </div>
                <div class="saved-filter-card-figma__distance">
                  <p class="saved-filter-card-figma__label">거리</p>
                  <div class="saved-filter-card-figma__chips">
                    <span
                      v-for="d in DISTANCE_OPTIONS"
                      :key="d"
                      class="saved-filter-chip-figma saved-filter-chip-figma--outlined"
                      :class="{ 'saved-filter-chip-figma--selected': sf.distance === d }"
                    >
                      {{ getSavedFilterDistanceLabel(d) }}
                    </span>
                  </div>
                </div>
                <div class="saved-filter-card-figma__food">
                  <p class="saved-filter-card-figma__label">음식 종류</p>
                  <div class="saved-filter-card-figma__chips">
                    <span
                      v-for="[ftId, label] in FOOD_TYPE_ENTRIES"
                      :key="ftId"
                      class="saved-filter-chip-figma saved-filter-chip-figma--outlined"
                      :class="{ 'saved-filter-chip-figma--selected': (sf.foodTypes || []).includes(ftId) }"
                    >
                      {{ label }}
                    </span>
                  </div>
                </div>
                <div v-if="sf.openOnly" class="saved-filter-card-figma__open-only">
                  <img :src="iconCheckboxChecked" alt="" class="saved-filter-card-figma__checkbox-icon" aria-hidden="true" />
                  <p class="saved-filter-card-figma__checkbox-text">영업중인 가게만 보기</p>
                </div>
              </div>
            </div>
            </div>
          </div>
          <div v-if="savedFilters.length > 0" class="saved-filter-pagination-outer">
            <div
              v-if="totalSavedFilterPages > 1"
              class="saved-section-pagination"
              data-name="pagenation"
              data-node-id="I47:3141;47:3952"
            >
              <button
                type="button"
                class="saved-pagination-arrow"
                aria-label="이전 페이지"
                :disabled="filterPage <= 1"
                @click="setSavedFilterPage(filterPage - 1)"
              >
                ‹
              </button>
              <div class="saved-pagination-numbers">
                <button
                  v-for="n in totalSavedFilterPages"
                  :key="n"
                  type="button"
                  class="saved-pagination-num"
                  :class="{ 'saved-pagination-num--current': n === filterPage }"
                  @click="setSavedFilterPage(n)"
                >
                  {{ n }}
                </button>
              </div>
              <button
                type="button"
                class="saved-pagination-arrow"
                aria-label="다음 페이지"
                :disabled="filterPage >= totalSavedFilterPages"
                @click="setSavedFilterPage(filterPage + 1)"
              >
                ›
              </button>
            </div>
          </div>
        </section>
        <section class="saved-section" aria-labelledby="section-places-title">
          <div class="saved-section-head">
            <h2 id="section-places-title" class="saved-section-title">저장한 장소</h2>
            <div class="saved-section-buttons">
              <button
                type="button"
                class="saved-section-btn"
                :class="{ 'saved-section-btn--active': placeSortOrder === 'desc' }"
                @click="placeSortOrder = 'desc'"
              >
                최신순
              </button>
              <button
                type="button"
                class="saved-section-btn"
                :class="{ 'saved-section-btn--active': placeSortOrder === 'asc' }"
                @click="placeSortOrder = 'asc'"
              >
                오래된순
              </button>
            </div>
          </div>
          <div
            v-if="pagedSavedPlaces.length === 0"
            class="saved-section-contents saved-section-contents--empty"
          >
            <p class="saved-section-empty-text">저장한 장소가 없어요</p>
          </div>
          <div v-else class="saved-section-contents saved-section-contents--cards">
            <ul class="saved-places-list">
              <li
                v-for="item in pagedSavedPlaces"
                :key="item.id"
                class="saved-place-item"
              >
                <div class="saved-place-card">
                  <div class="saved-place-thumb-wrap">
                    <img
                      :src="getSavedFavoriteImage(item)"
                      :alt="item.name"
                      class="saved-place-thumb"
                      @error="(e) => { e.target.src = defaultThumbnail }"
                    />
                    <button
                      type="button"
                      class="saved-place-heart-btn"
                      aria-label="즐겨찾기 해제"
                      @click.stop="handleToggleFavoriteFromMyPage(item)"
                    >
                      <svg class="saved-place-heart-icon" viewBox="0 0 24 24" aria-hidden="true">
                        <path
                          class="saved-place-heart-path"
                          d="M12 20.25c-.32 0-.64-.1-.9-.3-.76-.56-1.45-1.09-2.08-1.56-2.53-1.92-4.42-3.36-4.42-5.89C4.6 9.5 6.1 8 7.92 8c1.12 0 2.12.52 2.78 1.39L12 10.9l1.3-1.51C13.96 8.52 14.96 8 16.08 8 17.9 8 19.4 9.5 19.4 12.5c0 2.53-1.89 3.97-4.42 5.89-.63.47-1.32 1-2.08 1.56-.26.2-.58.3-.9.3Z"
                        />
                      </svg>
                    </button>
                  </div>
                  <div class="saved-place-info">
                    <div class="saved-place-header-row">
                      <p class="saved-place-name">{{ item.name }}</p>
                      <button
                        type="button"
                        class="saved-place-external-btn"
                        aria-label="지도에서 보기"
                        @click="openFavoriteExternal(item)"
                      >
                        <img
                          :src="externalLinkIcon"
                          alt=""
                          class="saved-place-external-icon"
                          aria-hidden="true"
                        />
                      </button>
                    </div>
                    <p class="saved-place-category">{{ getFavoriteCategory(item) }}</p>
                  </div>
                </div>
              </li>
            </ul>
          </div>
          <div
            v-if="totalSavedPlacePages > 1"
            class="saved-section-pagination"
          >
            <button
              type="button"
              class="saved-pagination-arrow"
              aria-label="이전 페이지"
              :disabled="placePage <= 1"
              @click="setSavedPlacePage(placePage - 1)"
            >
              ‹
            </button>
            <div class="saved-pagination-numbers">
              <button
                v-for="n in totalSavedPlacePages"
                :key="n"
                type="button"
                class="saved-pagination-num"
                :class="{ 'saved-pagination-num--current': n === placePage }"
                @click="setSavedPlacePage(n)"
              >
                {{ n }}
              </button>
            </div>
            <button
              type="button"
              class="saved-pagination-arrow"
              aria-label="다음 페이지"
              :disabled="placePage >= totalSavedPlacePages"
              @click="setSavedPlacePage(placePage + 1)"
            >
              ›
            </button>
          </div>
        </section>
      </template>

      <!-- 지난 결과 내역 전용 섹션 -->
      <template v-else-if="activeMainSection === MAIN_SECTION.HISTORY">
        <section class="section section--bottom-space" aria-labelledby="section-history-title">
          <div class="section-head section-head--history">
            <h2 id="section-history-title" class="section-title">지난 결과 내역</h2>
            <div class="history-filters" role="tablist" aria-label="결과 유형 필터">
              <button
                type="button"
                class="history-filter-chip"
                :class="{ 'history-filter-chip--active': historyFilter === 'all' }"
                role="tab"
                :aria-selected="historyFilter === 'all'"
                @click="
                  historyFilter = 'all';
                  router.replace({
                    path: '/my',
                    query: {
                      section: 'history',
                      filter: 'all'
                    }
                  })
                "
              >
                전체
              </button>
              <button
                type="button"
                class="history-filter-chip"
                :class="{ 'history-filter-chip--active': historyFilter === 'vote' }"
                role="tab"
                :aria-selected="historyFilter === 'vote'"
                @click="
                  historyFilter = 'vote';
                  router.replace({
                    path: '/my',
                    query: {
                      section: 'history',
                      filter: 'vote'
                    }
                  })
                "
              >
                투표만
              </button>
              <button
                type="button"
                class="history-filter-chip"
                :class="{ 'history-filter-chip--active': historyFilter === 'roulette' }"
                role="tab"
                :aria-selected="historyFilter === 'roulette'"
                @click="
                  historyFilter = 'roulette';
                  router.replace({
                    path: '/my',
                    query: {
                      section: 'history',
                      filter: 'roulette'
                    }
                  })
                "
              >
                룰렛만
              </button>
            </div>
          </div>

          <div class="history-section-body">
            <!-- 리스트 영역: 고정 높이 슬롯 → 카드 개수와 무관하게 섹션 크기 동일 -->
            <div class="history-list-slot">
              <div
                v-if="filteredHistoryResults.length === 0"
                class="section-contents section-contents--empty section-contents--history-empty"
              >
                <p class="empty-text">진행한 결과가 없어요</p>
              </div>
              <div
                v-else
                class="section-contents section-contents--list section-contents--history-list"
              >
                <router-link
                  v-for="item in pagedHistoryResults"
                  :key="`${item.type}-${item.id}-${item.createdAt}`"
                  :to="
                    item.type === 'vote'
                      ? `/vote/${item.id}`
                      : { name: 'rouletteShare', query: { data: item.shareData, fromHistory: 'true' } }
                  "
                  class="vote-list-item"
                  :class="{ 'vote-list-item--roulette': item.type === 'roulette' }"
                >
                  <div class="vote-list-main">
                    <span
                      class="result-label"
                      :class="item.type === 'vote' ? 'result-label--vote' : 'result-label--roulette'"
                    >
                      {{ item.type === 'vote' ? '투표' : '룰렛' }}
                    </span>
                    <span class="vote-list-title">
                      {{ item.type === 'vote' ? item.title : '오늘 점심은 여기로 결정!' }}
                    </span>
                  </div>
                  <span class="vote-list-date">{{ formatVoteDate(item.createdAt) }}</span>
                </router-link>
              </div>
            </div>

            <!-- 페이지네이션 슬롯: 항상 같은 높이 예약 → 버튼 위치 완전 고정 -->
            <div class="history-pagination-slot">
              <div
                v-if="filteredHistoryResults.length > 0"
                class="pagination pagination--history"
              >
                <button
                  type="button"
                  class="pagination-arrow"
                  aria-label="이전"
                  :disabled="historyPage === 1"
                  @click="goPrevHistoryPage"
                >
                  ‹
                </button>
                <button
                  v-for="page in totalHistoryPages"
                  :key="`history-page-${page}`"
                  type="button"
                  class="pagination-page"
                  :class="{ 'pagination-page--current': page === historyPage }"
                  @click="setHistoryPage(page)"
                >
                  {{ page }}
                </button>
                <button
                  type="button"
                  class="pagination-arrow"
                  aria-label="다음"
                  :disabled="historyPage === totalHistoryPages"
                  @click="goNextHistoryPage"
                >
                  ›
                </button>
              </div>
            </div>
          </div>
        </section>
      </template>
    </main>

    <!-- 저장한 필터 수정/추가 모달 (Figma 47:3112) -->
    <Teleport to="body">
      <div
        v-if="editingFilter || showAddFilterModal"
        class="edit-filter-modal-backdrop"
        role="dialog"
        aria-modal="true"
        :aria-label="editingFilter ? '필터 수정' : '필터 추가'"
        @click.self="closeEditModal"
      >
        <div class="edit-filter-modal" data-node-id="47:3112">
          <div class="edit-filter-modal__contents">
            <div class="edit-filter-modal__title-row">
              <input
                v-model="editForm.name"
                type="text"
                class="edit-filter-modal__input"
                placeholder="새 필터"
                maxlength="12"
                aria-label="필터 이름"
              />
              <img :src="iconStarFilled" alt="" class="edit-filter-modal__star" aria-hidden="true" />
            </div>
            <div class="edit-filter-modal__section">
              <p class="edit-filter-modal__label">거리</p>
              <div class="edit-filter-modal__chips">
                <button
                  v-for="d in DISTANCE_OPTIONS"
                  :key="d"
                  type="button"
                  class="edit-filter-modal__chip"
                  :class="{ 'edit-filter-modal__chip--selected': editForm.distance === d }"
                  @click="editForm.distance = d"
                >
                  {{ getSavedFilterDistanceLabel(d) }}
                </button>
              </div>
            </div>
            <div class="edit-filter-modal__section">
              <div class="edit-filter-modal__label-row">
                <p class="edit-filter-modal__label">음식 종류</p>
                <button type="button" class="edit-filter-modal__select-all" @click="toggleAllEditFoodTypes">
                  <img
                    :src="isAllEditFoodTypesSelected() ? iconCheckboxChecked : iconCheckboxOutline"
                    alt=""
                    class="edit-filter-modal__select-all-icon"
                    aria-hidden="true"
                  />
                  <span class="edit-filter-modal__select-all-text">전체 선택</span>
                </button>
              </div>
              <div class="edit-filter-modal__chips edit-filter-modal__chips--wrap">
                <button
                  v-for="[ftId, label] in FOOD_TYPE_ENTRIES"
                  :key="ftId"
                  type="button"
                  class="edit-filter-modal__chip"
                  :class="{ 'edit-filter-modal__chip--selected': isEditFoodTypeSelected(ftId) }"
                  @click="toggleEditFoodType(ftId)"
                >
                  {{ label }}
                </button>
              </div>
            </div>
            <div class="edit-filter-modal__checkbox-row">
              <button
                type="button"
                class="edit-filter-modal__checkbox"
                aria-pressed="editForm.openOnly"
                @click="editForm.openOnly = !editForm.openOnly"
              >
                <img
                  :src="editForm.openOnly ? iconCheckboxChecked : iconCheckboxOutline"
                  alt=""
                  class="edit-filter-modal__checkbox-icon"
                  aria-hidden="true"
                />
              </button>
              <span class="edit-filter-modal__checkbox-text">영업중인 가게만 보기</span>
            </div>
            <div class="edit-filter-modal__buttons">
              <button
                type="button"
                class="edit-filter-modal__btn edit-filter-modal__btn--primary"
                :disabled="editFormSubmitting"
                @click="saveEditFilter"
              >
                저장하기
              </button>
              <button type="button" class="edit-filter-modal__btn edit-filter-modal__btn--secondary" @click="closeEditModal">
                닫기
              </button>
            </div>
          </div>
        </div>
      </div>
    </Teleport>

    <ProfileChangeModal
      :show="showProfileChangeModal"
      :profile-options="profileOptions"
      :current-value="currentProfileIndex"
      @close="closeProfileChangeModal"
      @confirm="onProfileConfirm"
    />
  </div>
</template>

<style scoped>
.mypage {
  display: flex;
  width: 100%;
  min-height: 100%;
  flex: 1;
  background: linear-gradient(135deg, #f8f9fa 0%, #f1f3f4 100%);
}

.side-menu {
  flex-shrink: 0;
  width: 320px;
  padding: 80px 24px;
  box-sizing: border-box;
  background-color: #ffffff;
  border-right: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 2px 0 16px rgba(0, 0, 0, 0.03);
}

.side-menu-contents {
  display: flex;
  flex-direction: column;
  gap: 40px;
  width: 100%;
  max-width: 260px;
}

.profile {
  display: flex;
  gap: 20px;
  align-items: center;
}

.profile-thumbnail {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 4px;
  border: 2px solid #ffffff;
  box-shadow: 0 0 0 2px #ffe0d8;
  overflow: hidden;
  background-color: #e8eaed;
}

.profile-thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.profile-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 18px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
  white-space: nowrap;
} 

.divider {
  width: 100%;
  height: 1px;
  background-color: #e8eaed;
  flex-shrink: 0;
}

.menu-1depth {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 20px;
  width: 100%;
  box-sizing: border-box;
  border-radius: 999px;
  transition:
    background-color 0.18s ease-out,
    box-shadow 0.18s ease-out,
    transform 0.16s ease-out;
}

.menu-1depth--clickable {
  cursor: pointer;
}

.menu-1depth--clickable:hover {
  background-color: #fff0ea;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.menu-1depth--history {
  border: none;
  background: transparent;
  cursor: pointer;
}

.menu-1depth--history:hover {
  background-color: #fff0ea;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.04);
  transform: translateY(-1px);
}

.menu-1depth--history:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.menu-1depth--selected {
  background-color: #fff0ea;
}

.menu-1depth--selected .menu-1depth-title {
  color: #ff5531;
}

.menu-1depth-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.menu-arrow {
  flex-shrink: 0;
  font-size: 14px;
  color: #5f6368;
  transition: transform 0.2s ease-out;
}

.menu-arrow--collapsed {
  transform: rotate(-90deg);
}

.sub-menu {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-left: 30px;
  width: 100%;
  box-sizing: border-box;
}

.sub-menu-item {
  padding: 12px 20px;
  border-radius: 999px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #5f6368;
  transition:
    background-color 0.18s ease-out,
    color 0.18s ease-out,
    transform 0.14s ease-out;
}

.sub-menu-item:hover:not(.sub-menu-item--selected) {
  background-color: #fff0ea;
  color: #3c4043;
  transform: translateX(2px);
}

.sub-menu-item--selected {
  background-color: #fff0ea;
  font-weight: 700;
  color: #ff5531;
}

/* 내 저장 섹션 (저장한 필터 + 저장한 장소) */
.saved-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 1254px;
  margin: 0;
}

.saved-section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.saved-section-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 28px;
  line-height: 1.35;
  color: #31373c;
  margin: 0;
}

.saved-section-btn {
  padding: 8px 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.saved-section-btn:hover {
  background-color: #f1f3f4;
}

.saved-section-btn--active {
  font-weight: 700;
}

.saved-section-buttons {
  display: flex;
  align-items: center;
  gap: 4px;
}

.saved-section-contents {
  width: 100%;
}

.saved-section-contents--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.saved-section-empty-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 24px;
  line-height: normal;
  color: #dadce0;
  margin: 0;
}

/* 저장한 필터 목록 (Figma 47:3141) */
.favorite-list.saved-section {
  gap: 24px;
}

.list-title__heading {
  font-size: var(--font-size-xxlarge, 28px);
  color: #31373c;
}

.list-title__button {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 8px 12px;
  border-radius: 10px;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  text-decoration: none;
  border: none;
  cursor: pointer;
  font-family: inherit;
}

.list-title__button:hover {
  background-color: #f1f3f4;
}

/* 저장한 필터: 고정 높이 → 잘림 없음 + 페이지네이션 위치 고정 */
.saved-filter-section-body {
  width: 100%;
  height: 960px;
  box-sizing: border-box;
}

.saved-filter-list-contents.list-contents--filters {
  display: flex;
  flex-wrap: wrap;
  align-content: flex-start;
  gap: 18px 24px;
  width: 100%;
  height: 960px;
  overflow: hidden;
  box-sizing: border-box;
}

/* 저장한 필터: 페이지네이션만 section-body 밖 별도 div, 여백 (저장한 장소와 동일 클래스 사용) */
.saved-filter-pagination-outer {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 100%;
  margin-top: 32px;
  min-height: 52px;
  box-sizing: border-box;
}

.saved-filter-card-figma {
  width: 402px;
  min-width: 0;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 24px;
  padding: 24px;
  box-sizing: border-box;
}

.saved-filter-card-figma__inner {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 28px;
  width: 100%;
}

.saved-filter-card-figma__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
  gap: 12px;
}

.saved-filter-card-figma__name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
  flex: 1;
  min-width: 0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.saved-filter-card-figma__actions {
  display: flex;
  align-items: center;
  gap: 4px;
  flex-shrink: 0;
}

.saved-filter-card-figma__star-wrap {
  position: relative;
  flex-shrink: 0;
}

.saved-filter-card-figma__confirm-popup {
  position: absolute;
  top: 100%;
  left: 0;
  margin-top: 8px;
  padding: 12px 14px;
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 12px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.12);
  z-index: 10;
  width: max-content;
  max-width: 280px;
}

/* 삭제 확인 팝업: 문구 위, 버튼 아래·오른쪽 정렬 (일반적인 확인 다이얼로그 패턴) */
.saved-filter-card-figma__confirm-text {
  margin: 0 0 16px 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.45;
  color: #3c4043;
}

.saved-filter-card-figma__confirm-btns {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}

/* 취소 = 보조 액션(테두리만), 삭제 = 주 액션(채움) */
.saved-filter-card-figma__confirm-cancel {
  padding: 8px 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  color: #5f6368;
  background: transparent;
  border: 1px solid #dadce0;
  border-radius: 8px;
  cursor: pointer;
}

.saved-filter-card-figma__confirm-cancel:hover {
  background: #f8f9fa;
  border-color: #bdc1c6;
}

.saved-filter-card-figma__confirm-delete {
  padding: 8px 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  color: #fff;
  background: #ff5531;
  border: none;
  border-radius: 8px;
  cursor: pointer;
}

.saved-filter-card-figma__confirm-delete:hover {
  background: #e64a28;
}

/* 저장한 필터 수정 모달 (Figma 47:3112) */
.edit-filter-modal-backdrop {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
  box-sizing: border-box;
}

.edit-filter-modal {
  background: #fff;
  border: 1px solid #dadce0;
  border-radius: 24px;
  padding: 24px;
  width: 100%;
  max-width: 402px;
  max-height: calc(100vh - 48px);
  overflow-y: auto;
  box-sizing: border-box;
}

.edit-filter-modal__contents {
  display: flex;
  flex-direction: column;
  align-items: stretch;
  gap: 28px;
}

.edit-filter-modal__title-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.edit-filter-modal__input {
  flex: 1;
  min-width: 0;
  height: 48px;
  padding: 0 12px;
  border: 1px solid #dadce0;
  border-radius: 10px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  box-sizing: border-box;
}

.edit-filter-modal__input::placeholder {
  color: #bdc1c6;
}

.edit-filter-modal__star {
  width: 32px;
  height: 32px;
  flex-shrink: 0;
  display: block;
  filter: brightness(0) saturate(100%) invert(81%) sepia(34%) saturate(2472%) hue-rotate(1deg) brightness(102%) contrast(101%);
}

.edit-filter-modal__section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.edit-filter-modal__label {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
}

.edit-filter-modal__label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.edit-filter-modal__select-all {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 0;
  border: none;
  background: none;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  cursor: pointer;
}

.edit-filter-modal__select-all-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: block;
}

.edit-filter-modal__select-all-text {
  font-family: inherit;
  font-weight: inherit;
  font-size: inherit;
  line-height: inherit;
  color: inherit;
}

.edit-filter-modal__chips {
  display: flex;
  flex-wrap: nowrap;
  gap: 10px;
  align-items: center;
}

.edit-filter-modal__chips--wrap {
  flex-wrap: wrap;
}

.edit-filter-modal__chip {
  flex-shrink: 0;
  height: 42px;
  padding: 0 14px;
  border: 1px solid #dadce0;
  border-radius: 12px;
  background: #fff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  cursor: pointer;
  box-sizing: border-box;
}

.edit-filter-modal__chip--selected {
  background: #f1f3f4;
  border-color: #bdc1c6;
  color: #3c4043;
}

.edit-filter-modal__chip:hover {
  background: #f8f9fa;
}

.edit-filter-modal__checkbox-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.edit-filter-modal__checkbox {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
}

.edit-filter-modal__checkbox-icon {
  width: 24px;
  height: 24px;
  display: block;
}

.edit-filter-modal__checkbox-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
}

.edit-filter-modal__buttons {
  display: flex;
  gap: 12px;
  align-items: stretch;
  width: 100%;
}

.edit-filter-modal__btn {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 56px;
  padding: 0 20px;
  border-radius: 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  cursor: pointer;
  border: none;
  box-sizing: border-box;
}

.edit-filter-modal__btn--primary {
  flex: 1;
  background: #fff0ea;
  color: #ff5531;
}

.edit-filter-modal__btn--primary:hover:not(:disabled) {
  background: #ffe0d6;
}

.edit-filter-modal__btn--primary:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.edit-filter-modal__btn--secondary {
  background: #fff;
  border: 1px solid #dadce0;
  color: #3c4043;
}

.edit-filter-modal__btn--secondary:hover {
  background: #f8f9fa;
}

.saved-filter-card-figma__star-btn {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 4px;
}

.saved-filter-card-figma__star-btn:hover {
  background: #f1f3f4;
}

.saved-filter-card-figma__star {
  width: 32px;
  height: 32px;
  display: block;
  /* 금색 (#FFC200) */
  filter: brightness(0) saturate(100%) invert(81%) sepia(34%) saturate(2472%) hue-rotate(1deg) brightness(102%) contrast(101%);
}

.saved-filter-card-figma__edit {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: none;
  cursor: pointer;
  border-radius: 4px;
}

.saved-filter-card-figma__edit:hover {
  background: #f1f3f4;
}

.saved-filter-card-figma__edit-icon {
  width: 24px;
  height: 24px;
  display: block;
}

.saved-filter-card-figma__distance,
.saved-filter-card-figma__food {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
}

.saved-filter-card-figma__label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.saved-filter-card-figma__chips {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
}

.saved-filter-chip-figma {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 42px;
  padding: 8px 14px;
  border-radius: 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  box-sizing: border-box;
}

.saved-filter-chip-figma--outlined {
  background: #ffffff;
  border: 1px solid #dadce0;
}

.saved-filter-chip-figma--selected {
  background: #f1f3f4;
  border-color: #bdc1c6;
  color: #3c4043;
}

.saved-filter-card-figma__open-only {
  display: flex;
  align-items: center;
  gap: 4px;
}

.saved-filter-card-figma__checkbox-icon {
  width: 24px;
  height: 24px;
  flex-shrink: 0;
  display: block;
}

.saved-filter-card-figma__checkbox-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.saved-section-contents--cards {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-start;
  gap: 18px 24px;
  /* 3줄 + 여유만 확보 → 페이지네이션 미세 움직임 방지, 여백은 최소화 */
  min-height: 370px;
}

.saved-places-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 18px 24px;
  align-content: flex-start;
  width: 100%;
}

.saved-place-item {
  list-style: none;
  min-width: 0;
}

.saved-place-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 24px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  box-sizing: border-box;
  width: 100%;
  min-width: 0;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.saved-place-thumb-wrap {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  background-color: #e8eaed;
  position: relative;
}

.saved-place-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.saved-place-heart-btn {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  padding: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.35);
  transition: background 0.18s ease-out, transform 0.18s ease-out;
}

.saved-place-heart-btn:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: translateY(-1px) scale(1.05);
}

.saved-place-heart-icon {
  width: 20px;
  height: 20px;
}

.saved-place-heart-path {
  fill: #ff5531;
  stroke: #ff5531;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.saved-place-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.saved-place-name {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.saved-place-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.saved-place-category {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 13px;
  color: #ff5531;
}

.saved-place-external-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.saved-place-external-btn:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.saved-place-external-icon {
  width: 20px;
  height: 20px;
  display: block;
}

.saved-section-pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 50px;
  padding: 10px 0;
  width: 100%;
}

.saved-pagination-arrow {
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #3c4043;
  cursor: pointer;
  transition: color 0.2s;
}

.saved-pagination-arrow:hover:not(:disabled) {
  color: #ff5531;
}

.saved-pagination-arrow:disabled {
  color: #dadce0;
  cursor: not-allowed;
}

.saved-pagination-numbers {
  display: flex;
  align-items: center;
  gap: 12px;
}

.saved-pagination-num {
  min-width: 32px;
  height: 32px;
  padding: 0 4px;
  border: none;
  border-radius: 4px;
  background: #ffffff;
  border: 1px solid #dadce0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #3c4043;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.saved-pagination-num:hover {
  background-color: #f1f3f4;
}

.saved-pagination-num--current {
  background: #3c4043;
  border-color: #3c4043;
  color: #ffffff;
}

.mypage-main {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 60px;
  padding: 64px 48px 40px 48px;
  box-sizing: border-box;
  background-color: #ffffff;
  border-radius: 24px;
  margin: 32px 32px 24px 24px;
  box-shadow: 0 4px 24px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.04);
}

.section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  width: 100%;
  max-width: 800px;
  margin: 0;
}

.section--bottom-space {
  padding-bottom: 80px;
}

.section-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  width: 100%;
}

.section-head--history {
  align-items: flex-end;
  gap: 16px;
}

.section-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 28px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
}

.section-contents {
  width: 100%;
}

.section-contents--row {
  display: flex;
  gap: 24px;
  align-items: flex-start;
}

.thumbnail-wrap {
  flex-shrink: 0;
}

.thumbnail {
  border-radius: 8px;
  overflow: hidden;
  background-color: #e8eaed;
  position: relative;
}

.thumbnail--large {
  width: 160px;
  height: 160px;
  border: 3px solid #ffffff;
  box-shadow: 0 0 0 3px #ffe0d8;
}

.thumbnail-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumbnail-edit-btn {
  position: absolute;
  right: 4px;
  bottom: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  padding: 0;
  border: none;
  border-radius: 50%;
  background: rgba(0, 0, 0, 0.6);
  color: #ffffff;
  cursor: pointer;
  transition: background-color 0.2s;
}

.thumbnail-edit-icon {
  display: block;
  width: 32px;
  height: 32px;
  object-fit: contain;
  object-position: center;
}

.thumbnail-edit-btn:hover {
  background: rgba(0, 0, 0, 0.75);
}

.thumbnail-edit-btn:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.account {
  display: flex;
  flex-direction: column;
  gap: 24px;
  min-width: 0;
}

.account-row {
  display: flex;
  align-items: center;
  gap: 24px;
  flex-wrap: wrap;
}

.account-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  width: 100px;
  flex-shrink: 0;
}

.account-value {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 22px;
  line-height: 1.5;
  color: #3c4043;
}

.btn-outlined {
  height: 48px;
  padding: 12px 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s;
}

.btn-outlined:hover {
  background-color: #f8f9fa;
  border-color: #bdc1c6;
}

.btn-outlined:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.btn-ghost {
  padding: 8px 12px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: none;
  border-radius: 10px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-ghost:hover {
  background-color: #f1f3f4;
}

.btn-ghost:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

/* 회원 탈퇴 섹션 — 라벨·입력 한 축 정렬 */
.withdraw-section {
  max-width: 480px;
}

.withdraw-section .section-title {
  margin-bottom: 24px;
}

.withdraw-notice {
  margin-bottom: 32px;
  padding: 20px 24px;
  background-color: #fff9f8;
  border: 1px solid #ffe0d8;
  border-radius: 16px;
}

.withdraw-notice-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.5;
  color: #3c4043;
  margin: 0 0 8px;
}

.withdraw-notice-text:last-child {
  margin-bottom: 0;
}

.withdraw-notice-text strong {
  color: #d93025;
  font-weight: 700;
}

/* 폼: 세로 간격 통일, 라벨 140px | 필드 240px 축 정렬 */
.withdraw-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.withdraw-alert {
  display: flex;
  align-items: flex-start;
  gap: 10px;
  padding: 14px 16px;
  background-color: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
}

.withdraw-alert-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #dc2626;
  color: #fff;
  font-size: 12px;
  font-weight: 700;
  border-radius: 50%;
}

.withdraw-alert-message {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.45;
  color: #991b1b;
  margin: 0;
  flex: 1;
}

.withdraw-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.withdraw-row .account-label {
  width: 140px;
  flex-shrink: 0;
  white-space: nowrap;
  font-size: 16px;
  font-weight: 600;
}

.withdraw-input,
.withdraw-select {
  width: 240px;
  flex-shrink: 0;
  height: 48px;
  padding: 0 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.35;
  color: #3c4043;
  background-color: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 12px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.withdraw-input::placeholder {
  color: #9aa0a6;
}

.withdraw-input:hover,
.withdraw-select:hover {
  border-color: #bdc1c6;
}

.withdraw-input:focus,
.withdraw-select:focus {
  outline: none;
  border-color: #ff5531;
  box-shadow: 0 0 0 2px rgba(255, 85, 49, 0.2);
}

.withdraw-input:disabled,
.withdraw-select:disabled {
  background-color: #f1f3f4;
  color: #80868b;
  cursor: not-allowed;
}

.withdraw-select {
  cursor: pointer;
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath fill='%235f6368' d='M6 8L1 3h10z'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 14px center;
  padding-right: 40px;
}

.withdraw-agree-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.withdraw-checkbox {
  flex-shrink: 0;
  width: 22px;
  height: 22px;
  accent-color: #ff5531;
  cursor: pointer;
}

.withdraw-checkbox:disabled {
  cursor: not-allowed;
}

.withdraw-agree-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  line-height: 1.5;
  color: #3c4043;
  cursor: pointer;
  user-select: none;
}

.withdraw-actions {
  margin-top: 0;
}

.btn-withdraw {
  width: 240px;
  height: 56px;
  padding: 0 24px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #ffffff;
  background-color: #d93025;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-withdraw:hover:not(:disabled) {
  background-color: #b71c1c;
}

.btn-withdraw:focus-visible {
  outline: 2px solid #d93025;
  outline-offset: 2px;
}

.btn-withdraw:disabled {
  background-color: #dadce0;
  color: #9aa0a6;
  cursor: not-allowed;
}

.section-contents--empty {
  display: flex;
  align-items: center;
  justify-content: center;
  min-height: 100px;
}

.section-contents--list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

/* 지난 결과 내역: 고정 높이 슬롯으로 섹션·버튼 위치 흔들림 제거 */
.history-section-body {
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

/* 리스트 영역: 높이 고정(10개 카드 + gap 수용) → 페이지네이션 침범 방지 */
.history-list-slot {
  /* 10개 카드(약 52px/개) + 9개 gap(8px) = 572px, 여유 포함 620px */
  height: 620px;
  flex-shrink: 0;
  box-sizing: border-box;
}

.section-contents--history-list {
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
}

.section-contents--history-empty {
  height: 100%;
  min-height: 0;
  box-sizing: border-box;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 페이지네이션 슬롯: 항상 같은 높이 예약 → 버튼 위치 완전 고정 */
.history-pagination-slot {
  height: 52px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
}

.pagination--history {
  flex-shrink: 0;
}

.history-filters {
  display: flex;
  gap: 8px;
}

.history-filter-chip {
  padding: 6px 14px;
  border-radius: 999px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  color: #5f6368;
  cursor: pointer;
  transition:
    background-color 0.2s,
    border-color 0.2s,
    color 0.2s;
}

.history-filter-chip--active {
  border-color: #ff5531;
  background-color: #fff0ea;
  color: #ff5531;
}

.history-filter-chip:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.favorites-list {
  list-style: none;
  padding: 0;
  margin: 0;
  display: flex;
  flex-direction: row;
  flex-wrap: nowrap;
  gap: 16px;
}

.favorite-item {
  list-style: none;
}

.favorite-card {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 12px 16px;
  border-radius: 24px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  box-sizing: border-box;
  width: 260px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04);
}

.favorite-thumb-wrap {
  flex-shrink: 0;
  width: 80px;
  height: 80px;
  border-radius: 16px;
  overflow: hidden;
  background-color: #e8eaed;
  position: relative;
}

.favorite-thumb {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.favorite-info {
  flex: 1;
  min-width: 0;
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.favorite-name {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #202124;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.favorite-header-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.favorite-category {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 13px;
  color: #ff5531;
}

.favorite-external-btn {
  flex-shrink: 0;
  width: 28px;
  height: 28px;
  border-radius: 8px;
  border: none;
  background: transparent;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0;
  cursor: pointer;
  transition: background-color 0.2s;
}

.favorite-external-btn:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.favorite-external-icon {
  width: 20px;
  height: 20px;
  display: block;
}

.favorite-heart-button {
  position: absolute;
  right: 0;
  bottom: 0;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  border: none;
  padding: 0;
  background: rgba(0, 0, 0, 0.55);
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  z-index: 1;
  backdrop-filter: blur(4px);
  box-shadow: 0 4px 10px rgba(0, 0, 0, 0.35);
  transition:
    background 0.18s ease-out,
    transform 0.18s ease-out,
    box-shadow 0.18s ease-out;
}

.favorite-heart-button:hover {
  background: rgba(0, 0, 0, 0.7);
  transform: translateY(-1px) scale(1.05);
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.45);
}

.favorite-heart-button:active {
  transform: translateY(0) scale(0.96);
  box-shadow: 0 3px 8px rgba(0, 0, 0, 0.35);
}

.favorite-heart-icon {
  width: 20px;
  height: 20px;
}

.favorite-heart-path {
  fill: #ff5531;
  stroke: #ff5531;
  stroke-width: 1.8;
  stroke-linecap: round;
  stroke-linejoin: round;
}

.vote-list-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  border: 1px solid #dadce0;
  border-radius: 12px;
  text-decoration: none;
  color: inherit;
  transition: border-color 0.2s, background-color 0.2s;
}

.vote-list-item:hover {
  border-color: #ff5531;
  background-color: #fff5f3;
}

.vote-list-item--roulette {
  border-style: solid;
  border-color: #dadce0;
  background-color: #ffffff;
}

.vote-list-main {
  display: flex;
  align-items: center;
  gap: 10px;
  flex: 1;
  min-width: 0;
}

.vote-list-meta {
  display: flex;
  flex-direction: column;
  align-items: flex-end;
  gap: 2px;
}

.vote-list-timer-row {
  display: flex;
  align-items: center;
  gap: 4px;
}

.result-label {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  padding: 4px 10px;
  border-radius: 10px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 12px;
  line-height: 1.35;
  white-space: nowrap;
}

.result-label--vote {
  background-color: #fff0ea;
  border: 1px solid #ff5531;
  color: #ff5531;
}

.result-label--roulette {
  background-color: #f1f3f4;        /* var(--Color-Gray-100, #F1F3F4) */
  border: 1px solid #202124;        /* var(--Color-Gray-900, #202124) */
  color: #3c4043;
}

.vote-list-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  color: #3c4043;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
  margin-right: 12px;
}

.vote-list-date {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 14px;
  color: #5f6368;
  flex-shrink: 0;
}

.vote-list-timer {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 12px;
  color: #ff5531;
}

.vote-list-timer-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 12px;
  color: #5f6368;
}

.empty-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 24px;
  line-height: 1.35;
  color: #dadce0;
  margin: 0;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 16px;
  padding: 10px 0;
}

.pagination-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  padding: 0;
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #5f6368;
  cursor: pointer;
  transition: color 0.2s;
}

.pagination-arrow:hover {
  color: #3c4043;
}

.pagination-arrow:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.pagination-page {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #dadce0;
  background-color: #ffffff;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  color: #5f6368;
  cursor: pointer;
  padding: 0;
  margin: 0;
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
}

.pagination-page:not(.pagination-page--ellipsis):not(.pagination-page--current):hover {
  background-color: #f8f9fa;
  border-color: #bdc1c6;
}

.pagination-page--current {
  background-color: #3c4043;
  color: #ffffff;
  border-color: #3c4043;
}

.pagination-page--ellipsis {
  cursor: default;
}

@media (max-width: 1024px) {
  .mypage {
    flex-direction: column;
  }

  .side-menu {
    width: 100%;
    padding: 24px 20px;
    border-bottom: 1px solid #e8eaed;
    border-right: none;
    box-shadow: none;
  }

  .side-menu-contents {
    width: 100%;
    max-width: none;
    flex-direction: row;
    flex-wrap: wrap;
    gap: 20px;
  }

  .profile {
    width: 100%;
  }

  .divider {
    display: none;
  }

  .menu-1depth,
  .sub-menu {
    padding-left: 0;
  }

  .mypage-main {
    padding: 32px 20px 96px;
    margin: 24px 16px 32px;
    border-radius: 20px;
    box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  }
}
</style>
