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
        <div class="popup-contents">
          <p class="popup-title">
            로그인하셔야
            <br />
            이용할 수 있는 서비스입니다
          </p>
          <div class="popup-buttons">
            <button class="popup-btn popup-btn--primary" type="button" @click="handleConfirm">
              로그인하기
            </button>
            <button class="popup-btn popup-btn--secondary" type="button" @click="handleCloseClick">
              닫기
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
  background-color: #ffffff;
  border-radius: 32px;
  padding: 40px;
  width: 100%;
  max-width: min(360px, 90vw);
  box-sizing: border-box;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  align-items: center;
}

.popup-contents {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 24px;
  width: 100%;
}

.popup-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 22px;
  line-height: 1.35;
  color: #31373c;
  text-align: center;
  margin: 0;
}

.popup-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: stretch;
}

.popup-btn {
  height: 56px;
  border-radius: 16px;
  padding: 0 20px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  text-align: center;
  cursor: pointer;
  transition: background-color 0.2s, border-color 0.2s, color 0.2s;
  border: none;
  box-sizing: border-box;
}

.popup-btn--primary {
  flex: 1;
  min-width: 0;
  background-color: #fff0ea;
  color: #ff5531;
}

.popup-btn--primary:hover {
  background-color: #ffe0d6;
}

.popup-btn--primary:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.popup-btn--secondary {
  flex-shrink: 0;
  background-color: #ffffff;
  border: 1px solid #dadce0;
  color: #3c4043;
}

.popup-btn--secondary:hover {
  border-color: #bdc1c6;
  background-color: #f8f9fa;
}

.popup-btn--secondary:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
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
