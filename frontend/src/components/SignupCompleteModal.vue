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

<style scoped src="./SignupCompleteModal.css"></style>

