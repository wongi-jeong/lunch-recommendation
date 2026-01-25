<template>
  <div v-if="!hasResults" class="bottom-seat">
    <div class="bottom-seat-content">
      <p class="bottom-seat-text">필터를 이용해 식당을 추천 받아보세요!</p>
    </div>
  </div>
</template>

<script setup>
defineProps({
  hasResults: {
    type: Boolean,
    default: false
  }
})
</script>

<style scoped>
.bottom-seat {
  position: absolute;
  bottom: 0;
  left: 0;
  right: 0;
  width: 100%;
  height: 280px;
  z-index: 1000;
  pointer-events: none;
}

.bottom-seat-content {
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.5);
  border-radius: 24px 24px 0 0;
  box-shadow: 0 -2px 8px rgba(0, 0, 0, 0.1);
  padding: 16px 32px;
  display: flex;
  align-items: center;
  justify-content: center;
  text-align: center;
}

.bottom-seat-text {
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 26px;
  color: #202124;
  margin: 0;
  line-height: 1.35;
  letter-spacing: 0;
}

/* 반응형 디자인 */
@media (max-width: 768px) {
  .bottom-seat {
    height: 280px;
  }

  .bottom-seat-content {
    padding: 14px 24px;
  }

  .bottom-seat-text {
    font-size: 16px;
  }
}
</style>
