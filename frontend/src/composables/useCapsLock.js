import { ref } from 'vue'

/**
 * Caps Lock 상태 감지 composable
 * KeyboardEvent.getModifierState('CapsLock') API 사용 (모든 최신 브라우저 지원)
 * @returns {{ isCapsLockOn: Ref<boolean>, checkCapsLock: (e: KeyboardEvent) => void }}
 */
export function useCapsLock() {
  const isCapsLockOn = ref(false)

  const checkCapsLock = (event) => {
    if (event?.getModifierState) {
      isCapsLockOn.value = event.getModifierState('CapsLock')
    }
  }

  return {
    isCapsLockOn,
    checkCapsLock
  }
}
