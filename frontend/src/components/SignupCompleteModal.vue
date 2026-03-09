<script setup>
defineProps({
  visible: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['close', 'confirm'])

const handleOverlayClick = () => {
  emit('close')
}

const handleCloseClick = () => {
  emit('close')
}

const handleConfirm = () => {
  emit('confirm')
}
</script>

<template>
  <Transition name="popup-fade">
    <div v-if="visible" class="popup-overlay" @click.self="handleOverlayClick">
      <div class="popup-card">
        <div class="popup-close-row">
          <button class="popup-close-btn" type="button" @click="handleCloseClick" aria-label="닫기">
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
                d="M11 16.5l3 3 7-7"
                stroke="white"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </div>
          <p class="popup-title">회원가입이 완료되었어요</p>
          <p class="popup-description">
            이제 MECHU와 함께 점심 추천을 받아보세요.
          </p>
        </div>
        <div class="popup-action">
          <button class="popup-primary-btn" type="button" @click="handleConfirm">
            확인
          </button>
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

.popup-primary-btn {
  width: 100%;
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

.popup-primary-btn:hover {
  background-color: #e6442a;
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

