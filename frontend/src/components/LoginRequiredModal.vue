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

<style scoped src="./LoginRequiredModal.css"></style>
