<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  }
})

const emit = defineEmits(['close', 'go-vote'])

const handleOverlayClick = () => {
  emit('close')
}

const handleCloseClick = () => {
  emit('close')
}

const handleGoVote = () => {
  emit('go-vote')
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
          <button class="go-vote-btn" @click="handleGoVote">투표하러가기</button>
        </div>
      </div>
    </div>
  </Transition>
</template>

<style scoped src="./VoteCreatePopup.css"></style>
