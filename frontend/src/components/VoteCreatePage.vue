<script setup>
import { ref, computed, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import defaultThumbnail from '@/assets/restaurant-thumbnail-default.png'

const router = useRouter()
const route = useRoute()

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

const handleCreate = () => {
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

  const voteId = crypto.randomUUID().slice(0, 8)
  const voteRecord = {
    ...payload,
    id: voteId,
    createdAt: new Date().toISOString(),
    voters: []
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

const copyShareLink = async () => {
  try {
    await navigator.clipboard.writeText(shareLink.value)
  } catch {
    // fallback: silent fail
  }
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
      <div class="option-list-section">
        <div class="option-list">
          <div
            v-for="(restaurant, index) in restaurants"
            :key="index"
            class="option-item"
            :class="{ selected: selectedItems.has(index) }"
            @click="toggleSelect(index)"
          >
            <div class="option-img">
              <img
                :src="getPhotoUrl(restaurant)"
                :alt="restaurant.name"
                class="option-img-photo"
                @error="handleImageError"
              />
            </div>
            <div class="option-text">
              <div class="option-info">
                <span class="option-store-name">{{ restaurant.name }}</span>
                <span class="option-separator">|</span>
                <span class="option-category">
                  {{ (restaurant.categories || [])[0] || '식당' }}
                </span>
                <span class="option-dot">·</span>
                <span class="option-rating">
                  <img src="@/assets/star-icon.svg" alt="별점" class="option-star-icon" />
                  {{ restaurant.rating ? restaurant.rating.toFixed(1) : '0.0' }}
                </span>
                <span class="option-dot">·</span>
                <span
                  class="option-status"
                  :class="{
                    'status-open': restaurant.openNow === true,
                    'status-closed': restaurant.openNow === false
                  }"
                >
                  {{ getBusinessStatus(restaurant) }}
                </span>
              </div>
            </div>
            <div class="option-check">
              <svg v-if="selectedItems.has(index)" width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531"/>
                <path d="M10 16l4 4 8-8" stroke="white" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
              <svg v-else width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="16" cy="16" r="15" stroke="#BDC1C6" stroke-width="2"/>
              </svg>
            </div>
          </div>
        </div>
      </div>

      <!-- 타이머 섹션 -->
      <div class="timer-section">
        <div class="timer-header">
          <div class="timer-label">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path d="M12 6v6l4 2" stroke="#202124" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
              <circle cx="12" cy="12" r="9" stroke="#202124" stroke-width="2"/>
            </svg>
            <span class="timer-text">타이머</span>
          </div>
          <div class="timer-options">
            <label class="radio-option" :class="{ active: timerOption === 'none' }">
              <input type="radio" v-model="timerOption" value="none" class="radio-hidden" />
              <span class="radio-circle" :class="{ filled: timerOption === 'none' }"></span>
              <span class="radio-label">타이머 없음(직접 종료)</span>
            </label>
            <label class="radio-option" :class="{ active: timerOption === '10min' }">
              <input type="radio" v-model="timerOption" value="10min" class="radio-hidden" />
              <span class="radio-circle" :class="{ filled: timerOption === '10min' }"></span>
              <span class="radio-label">10분 후</span>
            </label>
            <label class="radio-option" :class="{ active: timerOption === '30min' }">
              <input type="radio" v-model="timerOption" value="30min" class="radio-hidden" />
              <span class="radio-circle" :class="{ filled: timerOption === '30min' }"></span>
              <span class="radio-label">30분 후</span>
            </label>
            <label class="radio-option" :class="{ active: timerOption === '1hour' }">
              <input type="radio" v-model="timerOption" value="1hour" class="radio-hidden" />
              <span class="radio-circle" :class="{ filled: timerOption === '1hour' }"></span>
              <span class="radio-label">1시간 후</span>
            </label>
          </div>
        </div>
      </div>

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
    <Transition name="popup-fade">
      <div v-if="showPopup" class="popup-overlay" @click.self="closePopup">
        <div class="popup-card">
          <div class="popup-close-row">
            <button class="popup-close-btn" @click="closePopup" aria-label="닫기">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path d="M15 5L5 15M5 5l10 10" stroke="#5F6368" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round"/>
              </svg>
            </button>
          </div>
          <div class="popup-content">
            <div class="popup-icon">
              <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
                <circle cx="16" cy="16" r="16" fill="#FF5531" />
                <path d="M10 16.5l3.2 3.2L22 11" stroke="white" stroke-width="2.4" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </div>
            <p class="popup-title">투표가 생성되었습니다!</p>
            <p class="popup-description">팀원들이 바로 참여할 수 있도록 아래 버튼으로 링크를 공유해 보세요.</p>
          </div>
          <div class="popup-action">
            <div class="popup-action-row">
              <button class="go-vote-btn" @click="goToVote">투표하러가기</button>
              <button class="popup-share-outline-btn" @click="copyShareLink">
                <svg width="20" height="20" viewBox="0 0 20 20" fill="none"><path d="M9.16667 14.1667H5.83333C4.68056 14.1667 3.69792 13.7604 2.88542 12.9479C2.07292 12.1354 1.66667 11.1528 1.66667 10C1.66667 8.84722 2.07292 7.86458 2.88542 7.05208C3.69792 6.23958 4.68056 5.83333 5.83333 5.83333H9.16667V7.5H5.83333C5.13889 7.5 4.54861 7.74306 4.0625 8.22917C3.57639 8.71528 3.33333 9.30556 3.33333 10C3.33333 10.6944 3.57639 11.2847 4.0625 11.7708C4.54861 12.2569 5.13889 12.5 5.83333 12.5H9.16667V14.1667ZM6.66667 10.8333V9.16667H13.3333V10.8333H6.66667ZM10.8333 14.1667V12.5H14.1667C14.8611 12.5 15.4514 12.2569 15.9375 11.7708C16.4236 11.2847 16.6667 10.6944 16.6667 10C16.6667 9.30556 16.4236 8.71528 15.9375 8.22917C15.4514 7.74306 14.8611 7.5 14.1667 7.5H10.8333V5.83333H14.1667C15.3194 5.83333 16.3021 6.23958 17.1146 7.05208C17.9271 7.86458 18.3333 8.84722 18.3333 10C18.3333 11.1528 17.9271 12.1354 17.1146 12.9479C16.3021 13.7604 15.3194 14.1667 14.1667 14.1667H10.8333Z" fill="#5f6368"/></svg>
                링크 공유
              </button>
            </div>
          </div>
        </div>
      </div>
    </Transition>
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

/* 옵션 목록 */
.option-list-section {
  width: 664px;
  padding-bottom: 40px;
}

.option-list {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.option-item {
  display: flex;
  align-items: center;
  height: 100px;
  border: 1px solid #bdc1c6;
  border-radius: 24px;
  overflow: hidden;
  cursor: pointer;
  transition: border-color 0.2s, box-shadow 0.2s;
  background-color: #fff;
}

.option-item:hover {
  border-color: #9aa0a6;
}

.option-item.selected {
  border-color: #ff5531;
  box-shadow: 0 0 0 1px #ff5531;
}

.option-img {
  width: 80px;
  height: 80px;
  background-color: #dadce0;
  border-radius: 12px;
  margin-left: 15px;
  flex-shrink: 0;
  overflow: hidden;
}

.option-img-photo {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.option-text {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 32px;
  flex: 1;
  min-width: 0;
}

.option-number {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
  flex-shrink: 0;
}

.option-info {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.option-store-name {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #5f6368;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.option-separator {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 16px;
  color: #5f6368;
  flex-shrink: 0;
}

.option-category {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
  white-space: nowrap;
  flex-shrink: 0;
}

.option-rating {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-family: 'Pretendard', sans-serif;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
  font-weight: 600;
}

.option-status {
  font-family: 'Pretendard', sans-serif;
  font-size: 16px;
  line-height: 1.35;
  font-weight: 600;
}

.option-dot {
  color: #9aa0a6;
}

.status-open {
  color: #34a853;
}

.status-closed {
  color: #ea4335;
}

.option-star-icon {
  width: 20px;
  height: 20px;
  object-fit: contain;
}

.option-check {
  margin-right: 31px;
  margin-left: auto;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 타이머 섹션 */
.timer-section {
  width: 664px;
  padding-bottom: 48px;
}

.timer-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.timer-label {
  display: flex;
  align-items: center;
  gap: 8px;
}

.timer-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #202124;
}

.timer-options {
  display: flex;
  align-items: center;
  gap: 12px;
}

.radio-option {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
}

.radio-hidden {
  position: absolute;
  opacity: 0;
  width: 0;
  height: 0;
  pointer-events: none;
}

.radio-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #bdc1c6;
  display: flex;
  align-items: center;
  justify-content: center;
  flex-shrink: 0;
  transition: border-color 0.2s;
}

.radio-circle.filled {
  border-color: #ff5531;
}

.radio-circle.filled::after {
  content: '';
  width: 12px;
  height: 12px;
  border-radius: 50%;
  background-color: #ff5531;
}

.radio-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 14px;
  line-height: 1.35;
  color: #9aa0a6;
  white-space: nowrap;
}

.radio-option.active .radio-label {
  color: #202124;
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

/* 팝업 오버레이 */
.popup-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.3);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  pointer-events: all;
}

/* 팝업 카드 */
.popup-card {
  background-color: #fff;
  border-radius: 48px;
  padding: 24px 32px 56px;
  width: 480px;
  max-width: 90vw;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 32px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  overflow: hidden;
}

.popup-close-row {
  width: 100%;
  display: flex;
  justify-content: flex-end;
}

.popup-close-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  border: none;
  background: transparent;
  border-radius: 12px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.popup-close-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
}

.popup-content {
  width: 100%;
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
}

.popup-icon {
  width: 56px;
  height: 56px;
  border-radius: 28px;
  background-color: #fff5f3;
  display: flex;
  align-items: center;
  justify-content: center;
}

.popup-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 22px;
  line-height: 1.35;
  color: #000;
  text-align: center;
  margin: 0;
}

.popup-description {
  font-family: 'Pretendard', sans-serif;
  font-weight: 400;
  font-size: 14px;
  line-height: 1.5;
  color: #5f6368;
  margin: 0;
  text-align: center;
}

.popup-action {
  width: 100%;
  box-sizing: border-box;
}

.popup-action-row {
  display: flex;
  gap: 12px;
  width: 100%;
}

.go-vote-btn {
  flex: 1;
  height: 64px;
  border: none;
  border-radius: 20px;
  background-color: #ff5531;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #fff;
  cursor: pointer;
  transition: background-color 0.15s;
}

.go-vote-btn:hover {
  background-color: #e6442a;
}

.popup-share-outline-btn {
  height: 64px;
  padding: 0 28px;
  border-radius: 20px;
  background-color: #fff;
  border: 1.5px solid #dadce0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 16px;
  line-height: 1.35;
  color: #5f6368;
  cursor: pointer;
  transition: border-color 0.2s, color 0.2s, background-color 0.2s;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  flex-shrink: 0;
  white-space: nowrap;
}

.popup-share-outline-btn:hover {
  border-color: #ff5531;
  color: #ff5531;
  background-color: #fff5f3;
}

.popup-share-outline-btn:hover svg path {
  fill: #ff5531;
}

/* 팝업 트랜지션 */
.popup-fade-enter-active,
.popup-fade-leave-active {
  transition: opacity 0.25s ease;
}

.popup-fade-enter-active .popup-card,
.popup-fade-leave-active .popup-card {
  transition: transform 0.25s ease, opacity 0.25s ease;
}

.popup-fade-enter-from,
.popup-fade-leave-to {
  opacity: 0;
}

.popup-fade-enter-from .popup-card {
  transform: scale(0.92);
  opacity: 0;
}

.popup-fade-leave-to .popup-card {
  transform: scale(0.92);
  opacity: 0;
}
</style>
