<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useAuth } from '@/composables/useAuth'
import VoteCreateOptionList from './VoteCreateOptionList.vue'
import VoteCreateTimerSection from './VoteCreateTimerSection.vue'
import VoteCreatePopup from './VoteCreatePopup.vue'
import defaultThumbnail from '@/assets/img-placeholder-restaurant.png'

const router = useRouter()
const route = useRoute()
const { getToken } = useAuth()

const today = new Date()
const defaultTitle = `${today.getFullYear()}년 ${today.getMonth() + 1}월 ${today.getDate()}일 점심 메뉴 투표`

const voteTitle = ref(defaultTitle)
const isEditingTitle = ref(false)
const titleInputRef = ref(null)
const timerOption = ref('none')

const restaurants = ref([])
const selectedItems = ref(new Set())
const showPopup = ref(false)
const shareLink = ref('')
const createdVoteId = ref('')

onMounted(() => {
  const raw = route.query.data
  if (raw) {
    try {
      const parsed = JSON.parse(decodeURIComponent(raw))
      if (Array.isArray(parsed)) {
        restaurants.value = parsed
        parsed.forEach((_, i) => selectedItems.value.add(i))
      }
    } catch (e) {
      console.error('투표 데이터 파싱 실패:', e)
    }
  }
})

const toggleSelect = (index) => {
  const next = new Set(selectedItems.value)
  if (next.has(index)) {
    next.delete(index)
  } else {
    next.add(index)
  }
  selectedItems.value = next
}

const canCreate = computed(() => selectedItems.value.size >= 2)

const startEditTitle = () => {
  isEditingTitle.value = true
  setTimeout(() => titleInputRef.value?.focus(), 0)
}

const finishEditTitle = () => {
  isEditingTitle.value = false
  if (!voteTitle.value.trim()) {
    voteTitle.value = defaultTitle
  }
}

const getPhotoUrl = (restaurant) => {
  if (restaurant.photoName && Array.isArray(restaurant.photoName) && restaurant.photoName.length > 0) {
    return `/api/restaurants/photo?name=${encodeURIComponent(restaurant.photoName[0])}`
  }
  return defaultThumbnail
}

const handleImageError = (event) => {
  event.target.src = defaultThumbnail
}

const getBusinessStatus = (restaurant) => {
  if (restaurant.openNow === true) return '영업중'
  if (restaurant.openNow === false) return '영업 종료'
  return '정보 없음'
}

const openExternalLink = (restaurant) => {
  const placeId = restaurant?.googlePlaceId
  if (!placeId) return
  const url = `https://www.google.com/maps/place/?q=place_id:${encodeURIComponent(placeId)}`
  window.open(url, '_blank', 'noopener,noreferrer')
}

const handleCreate = async () => {
  if (!canCreate.value) return

  const selected = restaurants.value
    .filter((_, i) => selectedItems.value.has(i))
    .map(r => ({
      name: r.name,
      googlePlaceId: r.googlePlaceId,
      address: r.address,
      photoName: r.photoName,
      rating: r.rating,
      openNow: r.openNow ?? null,
      categories: r.categories
    }))

  const payload = {
    title: voteTitle.value,
    timer: timerOption.value,
    options: selected
  }

  let voteId = crypto.randomUUID().slice(0, 8)
  const token = getToken()
  let createdByMemberId = null

  if (token) {
    try {
      const res = await fetch('/api/votes', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          'X-Auth-Token': token
        },
        body: JSON.stringify({ id: voteId, ...payload })
      })
      if (!res.ok) {
        const err = await res.json().catch(() => ({}))
        throw new Error(err.message || '투표 생성에 실패했습니다.')
      }
      const data = await res.json()
      if (data.id) voteId = data.id
      if (data.creatorMemberId != null) createdByMemberId = data.creatorMemberId
    } catch (e) {
      console.error('투표 API 저장 실패:', e)
      alert(e.message || '투표 저장에 실패했습니다. 다시 시도해주세요.')
      return
    }
  }

  const voteRecord = {
    ...payload,
    id: voteId,
    createdAt: new Date().toISOString(),
    voters: [],
    ...(createdByMemberId != null && { createdByMemberId })
  }

  localStorage.setItem(`vote_${voteId}`, JSON.stringify(voteRecord))
  createdVoteId.value = voteId
  shareLink.value = `${window.location.origin}/vote/${voteId}`
  showPopup.value = true
}

const closePopup = () => {
  showPopup.value = false
}

const goToVote = () => {
  showPopup.value = false
  router.push(`/vote/${createdVoteId.value}`)
}

const goBack = () => {
  router.back()
}
</script>

<template>
  <div class="vote-create-page" :class="{ 'page-disabled': showPopup }">
    <div class="vote-create-form">
      <!-- 제목 영역 -->
      <div class="editable-title">
        <div class="section-title-row">
          <input
            v-if="isEditingTitle"
            ref="titleInputRef"
            v-model="voteTitle"
            class="title-input"
            maxlength="30"
            @blur="finishEditTitle"
            @keyup.enter="finishEditTitle"
          />
          <h2 v-else class="title-text">{{ voteTitle }}</h2>
          <button class="edit-icon-btn" @click="startEditTitle" aria-label="제목 수정">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M16.474 5.408l2.118 2.118m-.756-3.982L12.109 9.27a2.118 2.118 0 00-.58 1.082L11 13l2.648-.53a2.118 2.118 0 001.082-.58l5.727-5.727a1.853 1.853 0 10-2.621-2.621z" stroke="#5F6368" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
              <path d="M19 15v3a2 2 0 01-2 2H6a2 2 0 01-2-2V7a2 2 0 012-2h3" stroke="#5F6368" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </button>
        </div>
      </div>

      <!-- 옵션 목록 -->
      <VoteCreateOptionList
        :restaurants="restaurants"
        :selected-items="selectedItems"
        :get-photo-url="getPhotoUrl"
        :handle-image-error="handleImageError"
        :open-external-link="openExternalLink"
        :get-business-status="getBusinessStatus"
        :toggle-select="toggleSelect"
      />

      <!-- 타이머 섹션 -->
      <VoteCreateTimerSection v-model="timerOption" />

      <!-- 하단 버튼 영역 -->
      <div class="form-actions">
        <button class="back-btn" @click="goBack" aria-label="뒤로가기">
          <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M20 8L12 16L20 24" stroke="#3C4043" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
          </svg>
        </button>
        <button
          class="create-btn"
          :disabled="!canCreate"
          @click="handleCreate"
        >
          투표 생성하기
        </button>
      </div>
    </div>

    <!-- 투표 생성 팝업 오버레이 -->
    <VoteCreatePopup
      :visible="showPopup"
      @close="closePopup"
      @go-vote="goToVote"
    />
  </div>
</template>

<style scoped>
.vote-create-page {
  flex: 1;
  min-height: 0;
  display: flex;
  justify-content: flex-start;
  align-items: flex-start;
  background-color: #eff2f5;
  padding: 80px 0 120px;
  overflow-y: auto;
}

.vote-create-form {
  background-color: #fff;
  border-radius: 64px;
  padding: 64px 64px 120px;
  display: flex;
  flex-direction: column;
  align-items: center;
  width: 792px;
  max-width: 100%;
  margin: 0 auto;
  flex-shrink: 0;
}

.editable-title {
  width: 664px;
  padding-bottom: 32px;
}

.section-title-row {
  display: flex;
  align-items: center;
  gap: 4px;
  overflow: hidden;
}

.title-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  margin: 0;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  flex: 1;
  min-width: 0;
}

.title-input {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 24px;
  line-height: 1.35;
  color: #3c4043;
  border: none;
  border-bottom: 2px solid #ff5531;
  outline: none;
  padding: 0 0 4px;
  background: transparent;
  flex: 1;
  min-width: 0;
  width: 0;
}

.edit-icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 48px;
  height: 48px;
  background: transparent;
  border: none;
  border-radius: 14px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.edit-icon-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

/* 하단 버튼 */
.form-actions {
  width: 664px;
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 0 96px;
}

.back-btn {
  width: 56px;
  height: 56px;
  border: 1px solid #dadce0;
  border-radius: 16px;
  background-color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.2s;
}

.back-btn:hover {
  background-color: #f5f5f5;
}

.create-btn {
  flex: 1;
  height: 56px;
  border: none;
  border-radius: 16px;
  background-color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.2s;
}

.create-btn:hover {
  background-color: #e6442a;
}

.create-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

/* 페이지 비활성화 */
.vote-create-page.page-disabled {
  pointer-events: none;
  user-select: none;
}

.vote-create-page.page-disabled .vote-create-form {
  filter: brightness(0.7);
  opacity: 0.6;
}
</style>
