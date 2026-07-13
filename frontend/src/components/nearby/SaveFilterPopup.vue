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

<style scoped src="./SaveFilterPopup.css"></style>
