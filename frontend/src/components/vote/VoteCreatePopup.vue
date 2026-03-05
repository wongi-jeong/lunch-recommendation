<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['close', 'go-vote', 'copy-link'])

const handleOverlayClick = () => {
  emit('close')
}

const handleCloseClick = () => {
  emit('close')
}

const handleGoVote = () => {
  emit('go-vote')
}

const handleCopyLink = () => {
  emit('copy-link')
}
</script>

<template>
  <Transition name="popup-fade">
    <div v-if="visible" class="popup-overlay" @click.self="handleOverlayClick">
      <div class="popup-card">
        <div class="popup-close-row">
          <button class="popup-close-btn" @click="handleCloseClick" aria-label="닫기">
            <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
              <path
                d="M15 5L5 15M5 5l10 10"
                stroke="#5F6368"
                stroke-width="1.8"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>
        <div class="popup-content">
          <div class="popup-icon">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none" xmlns="http://www.w3.org/2000/svg">
              <circle cx="16" cy="16" r="16" fill="#FF5531" />
              <path
                d="M10 16.5l3.2 3.2L22 11"
                stroke="white"
                stroke-width="2.4"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <p class="popup-title">투표가 생성되었습니다!</p>
          <p class="popup-description">
            팀원들이 바로 참여할 수 있도록 아래 버튼으로 링크를 공유해 보세요.
          </p>
        </div>
        <div class="popup-action">
          <div class="popup-action-row">
            <button class="go-vote-btn" @click="handleGoVote">투표하러가기</button>
            <button class="popup-share-outline-btn" @click="handleCopyLink">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none">
                <path
                  d="M9.16667 14.1667H5.83333C4.68056 14.1667 3.69792 13.7604 2.88542 12.9479C2.07292 12.1354 1.66667 11.1528 1.66667 10C1.66667 8.84722 2.07292 7.86458 2.88542 7.05208C3.69792 6.23958 4.68056 5.83333 5.83333 5.83333H9.16667V7.5H5.83333C5.13889 7.5 4.54861 7.74306 4.0625 8.22917C3.57639 8.71528 3.33333 9.30556 3.33333 10C3.33333 10.6944 3.57639 11.2847 4.0625 11.7708C4.54861 12.2569 5.13889 12.5 5.83333 12.5H9.16667V14.1667ZM6.66667 10.8333V9.16667H13.3333V10.8333H6.66667ZM10.8333 14.1667V12.5H14.1667C14.8611 12.5 15.4514 12.2569 15.9375 11.7708C16.4236 11.2847 16.6667 10.6944 16.6667 10C16.6667 9.30556 16.4236 8.71528 15.9375 8.22917C15.4514 7.74306 14.8611 7.5 14.1667 7.5H10.8333V5.83333H14.1667C15.3194 5.83333 16.3021 6.23958 17.1146 7.05208C17.9271 7.86458 18.3333 8.84722 18.3333 10C18.3333 11.1528 17.9271 12.1354 17.1146 12.9479C16.3021 13.7604 15.3194 14.1667 14.1667 14.1667H10.8333Z"
                  fill="#5f6368"
                />
              </svg>
              링크 공유
            </button>
          </div>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped>
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
