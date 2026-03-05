<script setup>
const props = defineProps({
  visible: {
    type: Boolean,
    required: true
  },
  title: {
    type: String,
    required: true
  },
  link: {
    type: String,
    required: true
  }
})

const emit = defineEmits([
  'close',
  'share-kakao',
  'share-facebook',
  'share-x',
  'share-band',
  'share-email',
  'copy-link'
])

const handleOverlayClick = () => {
  emit('close')
}

const handleCloseClick = () => {
  emit('close')
}
</script>

<template>
  <Transition name="share-modal-fade">
    <div v-if="visible" class="share-overlay" @click.self="handleOverlayClick">
      <div class="share-modal">
        <div class="share-modal-header">
          <span class="share-modal-title">{{ title }}</span>
          <button class="share-modal-close" @click="handleCloseClick" aria-label="닫기">
            <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
              <path
                d="M18 6L6 18M6 6l12 12"
                stroke="#5F6368"
                stroke-width="2"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </button>
        </div>
        <div class="share-platforms">
          <button class="share-platform-btn" @click="$emit('share-kakao')">
            <div class="share-platform-icon" style="background-color: #FEE500;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 3C6.477 3 2 6.463 2 10.691c0 2.724 1.8 5.113 4.508 6.46-.2.743-.722 2.694-.828 3.112-.13 .518.19 .511.399 .372.164-.109 2.612-1.776 3.67-2.497.733 .104 1.488 .162 2.251 .162 5.523 0 10-3.463 10-7.691S17.523 3 12 3z"
                  fill="#3C1E1E"
                />
              </svg>
            </div>
            <span>카카오톡</span>
          </button>
          <button class="share-platform-btn" @click="$emit('share-facebook')">
            <div class="share-platform-icon" style="background-color: #1877F2;">
              <svg width="24" height="24" viewBox="0 0 24 24" fill="none">
                <path
                  d="M24 12c0-6.627-5.373-12-12-12S0 5.373 0 12c0 5.99 4.388 10.954 10.125 11.854V15.47H7.078V12h3.047V9.356c0-3.007 1.791-4.668 4.533-4.668 1.312 0 2.686 .235 2.686 .235v2.953H15.83c-1.491 0-1.956 .925-1.956 1.875V12h3.328l-.532 3.469h-2.796v8.385C19.612 22.954 24 17.99 24 12z"
                  fill="white"
                />
              </svg>
            </div>
            <span>Facebook</span>
          </button>
          <button class="share-platform-btn" @click="$emit('share-x')">
            <div class="share-platform-icon" style="background-color: #000;">
              <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                <path
                  d="M18.244 2.25h3.308l-7.227 8.26 8.502 11.24H16.17l-5.214-6.817L4.99 21.75H1.68l7.73-8.835L1.254 2.25H8.08l4.713 6.231zm-1.161 17.52h1.833L7.084 4.126H5.117z"
                  fill="white"
                />
              </svg>
            </div>
            <span>X</span>
          </button>
          <button class="share-platform-btn" @click="$emit('share-band')">
            <div class="share-platform-icon" style="background-color: #06CF5E;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path
                  d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm0 3c1.66 0 3 1.34 3 3s-1.34 3-3 3-3-1.34-3-3 1.34-3 3-3zm0 14.2c-2.5 0-4.71-1.28-6-3.22.03-1.99 4-3.08 6-3.08 1.99 0 5.97 1.09 6 3.08-1.29 1.94-3.5 3.22-6 3.22z"
                  fill="white"
                />
              </svg>
            </div>
            <span>밴드</span>
          </button>
          <button class="share-platform-btn" @click="$emit('share-email')">
            <div class="share-platform-icon" style="background-color: #9AA0A6;">
              <svg width="20" height="20" viewBox="0 0 24 24" fill="none">
                <path
                  d="M20 4H4c-1.1 0-2 .9-2 2v12c0 1.1 .9 2 2 2h16c1.1 0 2-.9 2-2V6c0-1.1-.9-2-2-2zm0 4l-8 5-8-5V6l8 5 8-5v2z"
                  fill="white"
                />
              </svg>
            </div>
            <span>이메일</span>
          </button>
        </div>
        <div class="share-link-box">
          <span class="share-link-box-text">{{ link }}</span>
          <button class="share-link-copy-btn" @click="$emit('copy-link')">복사</button>
        </div>
      </div>
    </div>
  </Transition>
</template>
