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

<style scoped src="./ProfileChangeModal.css"></style>
