<script setup>
import { ref, watch } from 'vue'

const props = defineProps({
  show: {
    type: Boolean,
    default: false
  },
  /** 부모에서 API 실패 시 전달하는 오류 메시지 */
  errorMessage: {
    type: String,
    default: ''
  }
})

const emit = defineEmits(['save', 'close'])

const MAX_NAME_LENGTH = 12
const filterName = ref('')
const saving = ref(false)
const errorMessage = ref('')

watch(
  () => props.show,
  (visible) => {
    if (visible) {
      filterName.value = ''
      errorMessage.value = ''
    }
  }
)

function submit() {
  const name = filterName.value?.trim() || ''
  errorMessage.value = ''
  if (!name) {
    errorMessage.value = '필터 이름을 입력해주세요.'
    return
  }
  if (name.length > MAX_NAME_LENGTH) {
    errorMessage.value = `필터 이름은 최대 ${MAX_NAME_LENGTH}글자까지 입력할 수 있어요.`
    return
  }
  saving.value = true
  emit('save', name.substring(0, MAX_NAME_LENGTH))
  saving.value = false
}

function close() {
  emit('close')
}
</script>

<template>
  <Teleport to="body">
    <div v-if="show" class="save-filter-popup-overlay" role="dialog" aria-modal="true" aria-labelledby="save-filter-title" @click.self="close">
      <div class="save-filter-popup" data-node-id="47:1408">
        <div class="save-filter-popup__contents">
          <h2 id="save-filter-title" class="visually-hidden">내가 자주 사용하는 필터 저장</h2>
          <div class="save-filter-popup__field-wrap">
            <input
              v-model="filterName"
              type="text"
              class="save-filter-popup__input"
              :maxlength="MAX_NAME_LENGTH"
              placeholder="새 필터"
              aria-label="필터 이름"
              autofocus
              @keydown.enter="submit"
            />
            <div class="save-filter-popup__row">
              <span v-if="errorMessage || props.errorMessage" class="save-filter-popup__error" role="alert">{{ errorMessage || props.errorMessage }}</span>
              <span class="save-filter-popup__counter" aria-live="polite">{{ filterName.length }}/{{ MAX_NAME_LENGTH }}</span>
            </div>
          </div>
          <div class="save-filter-popup__buttons">
            <button
              type="button"
              class="save-filter-popup__btn save-filter-popup__btn--primary"
              :disabled="saving"
              @click="submit"
            >
              저장하기
            </button>
            <button
              type="button"
              class="save-filter-popup__btn save-filter-popup__btn--secondary"
              :disabled="saving"
              @click="close"
            >
              닫기
            </button>
          </div>
        </div>
      </div>
    </div>
  </Teleport>
</template>

<style scoped>
.save-filter-popup-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.4);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 2000;
  padding: 24px;
}

.save-filter-popup {
  background: #fff;
  border-radius: 32px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  padding: 40px;
  width: 100%;
  max-width: 400px;
}

.save-filter-popup__contents {
  display: flex;
  flex-direction: column;
  gap: 24px;
  align-items: stretch;
}

.save-filter-popup__field-wrap {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.save-filter-popup__input {
  width: 100%;
  height: 48px;
  padding: 12px 12px 12px 12px;
  border: 1px solid #dadce0;
  border-radius: 10px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 20px;
  line-height: 1.35;
  color: #202124;
  box-sizing: border-box;
}

.save-filter-popup__input::placeholder {
  color: #bdc1c6;
}

.save-filter-popup__input:focus {
  outline: none;
  border-color: #ff5531;
}

.save-filter-popup__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
  min-height: 20px;
}

.save-filter-popup__error {
  margin: 0;
  font-family: 'Pretendard', sans-serif;
  font-size: 14px;
  color: #d93025;
  flex: 1;
  min-width: 0;
}

.save-filter-popup__counter {
  font-family: 'Pretendard', sans-serif;
  font-size: 12px;
  color: #5f6368;
  flex-shrink: 0;
  margin-left: auto;
  text-align: right;
}

.save-filter-popup__buttons {
  display: flex;
  gap: 12px;
  align-items: stretch;
}

.save-filter-popup__btn {
  flex: 1;
  height: 56px;
  padding: 0 20px;
  border-radius: 16px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  cursor: pointer;
  transition: background-color 0.2s, color 0.2s;
}

.save-filter-popup__btn--primary {
  background: #fff0ea;
  color: #ff5531;
  border: none;
}

.save-filter-popup__btn--primary:hover:not(:disabled) {
  background: #ffe4db;
}

.save-filter-popup__btn--secondary {
  background: #fff;
  color: #3c4043;
  border: 1px solid #dadce0;
}

.save-filter-popup__btn--secondary:hover:not(:disabled) {
  background: #f8f9fa;
}

.save-filter-popup__btn:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.visually-hidden {
  position: absolute;
  width: 1px;
  height: 1px;
  padding: 0;
  margin: -1px;
  overflow: hidden;
  clip: rect(0, 0, 0, 0);
  white-space: nowrap;
  border: 0;
}
</style>
