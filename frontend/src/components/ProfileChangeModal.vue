<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: { type: Boolean, default: false },
  profileOptions: { type: Array, default: () => [] },
  currentValue: { type: [String, Number], default: 0 }
})

const emit = defineEmits(['close', 'confirm'])

const selectedIndex = ref(0)

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      const idx = typeof props.currentValue === 'number' ? props.currentValue : props.profileOptions.findIndex((o) => o === props.currentValue)
      selectedIndex.value = idx >= 0 ? idx : 0
    }
  },
  { immediate: true }
)

watch(
  () => props.currentValue,
  (val) => {
    if (!props.show) return
    const idx = typeof val === 'number' ? val : props.profileOptions.findIndex((o) => o === val)
    selectedIndex.value = idx >= 0 ? idx : 0
  }
)

const select = (index) => {
  selectedIndex.value = index
}

const confirm = () => {
  const index = selectedIndex.value
  const safeIndex = index >= 0 && index < props.profileOptions.length ? index : 0
  if (import.meta.env.DEV) {
    console.log('[ProfileChangeModal] 변경하기 클릭 → emit confirm', { index: safeIndex })
  }
  emit('confirm', safeIndex)
}

const close = () => {
  emit('close')
}

const onDimClick = (e) => {
  if (e.target === e.currentTarget) close()
}
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div
        v-if="show"
        class="profile-change-dim"
        role="dialog"
        aria-modal="true"
        aria-labelledby="profile-change-title"
        @click="onDimClick"
      >
        <div class="profile-change-box">
          <div class="thumbnail-list">
            <button
              v-for="(opt, index) in profileOptions"
              :key="index"
              type="button"
              class="thumbnail-item"
              :class="{ 'thumbnail-item--selected': selectedIndex === index }"
              :aria-pressed="selectedIndex === index"
              :aria-label="`프로필 ${index + 1} 선택`"
              @click="select(index)"
            >
              <img
                v-if="typeof opt === 'string'"
                :src="opt"
                alt=""
                class="thumbnail-item-img"
                aria-hidden="true"
              />
              <div v-else class="thumbnail-item-placeholder" aria-hidden="true" />
              <div v-if="selectedIndex === index" class="thumbnail-item-dim">
                <span class="thumbnail-item-check" aria-hidden="true">
                  <svg width="40" height="40" viewBox="0 0 40 40" fill="none" xmlns="http://www.w3.org/2000/svg">
                    <path d="M10 20l6 6 14-14" stroke="white" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
                  </svg>
                </span>
              </div>
            </button>
          </div>
          <div class="profile-change-buttons">
            <button type="button" class="btn-primary" @click="confirm">
              변경하기
            </button>
            <button type="button" class="btn-cancel" @click="close">
              닫기
            </button>
          </div>
        </div>
      </div>
    </Transition>
  </Teleport>
</template>

<style scoped>
.profile-change-dim {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.8);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
  padding: 24px;
  box-sizing: border-box;
}

.profile-change-box {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 60px;
  width: 100%;
  max-width: 752px;
}

.thumbnail-list {
  display: flex;
  flex-wrap: wrap;
  gap: 27px 37px;
  width: 100%;
  justify-content: center;
}

.thumbnail-item {
  position: relative;
  width: 160px;
  height: 160px;
  padding: 0;
  border: none;
  border-radius: 8px;
  overflow: hidden;
  background: #e8eaed;
  cursor: pointer;
  flex-shrink: 0;
  transition: transform 0.2s, box-shadow 0.2s;
}

.thumbnail-item:hover {
  transform: scale(1.02);
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.thumbnail-item:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.thumbnail-item-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.thumbnail-item-placeholder {
  width: 100%;
  height: 100%;
  background: linear-gradient(135deg, #e8eaed 0%, #dadce0 100%);
}

.thumbnail-item-dim {
  position: absolute;
  inset: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.6);
  border: 2px solid #ffffff;
  border-radius: 8px;
  padding: 8px;
  box-sizing: border-box;
}

.thumbnail-item-check {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 40px;
  height: 40px;
  color: #ffffff;
}

.profile-change-buttons {
  display: flex;
  gap: 12px;
  width: 100%;
  align-items: center;
}

.btn-primary {
  flex: 1;
  height: 56px;
  padding: 0 20px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #ffffff;
  background: #ff5531;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.btn-primary:hover {
  background: #e64a28;
}

.btn-primary:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.btn-cancel {
  height: 56px;
  padding: 0 60px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #3c4043;
  background: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 16px;
  cursor: pointer;
  flex-shrink: 0;
  transition: background-color 0.2s, border-color 0.2s;
}

.btn-cancel:hover {
  background: #f8f9fa;
  border-color: #bdc1c6;
}

.btn-cancel:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.modal-enter-active,
.modal-leave-active {
  transition: opacity 0.2s ease;
}

.modal-enter-active .profile-change-box,
.modal-leave-active .profile-change-box {
  transition: transform 0.2s ease;
}

.modal-enter-from,
.modal-leave-to {
  opacity: 0;
}

.modal-enter-from .profile-change-box,
.modal-leave-to .profile-change-box {
  transform: scale(0.96);
}
</style>
