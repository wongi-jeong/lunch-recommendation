import { ref } from 'vue'

/**
 * Caps Lock 상태 감지 composable
 * KeyboardEvent.getModifierState('CapsLock') API 사용 (모든 최신 브라우저 지원)
 * @returns {{ isCapsLockOn: Ref<boolean>, checkCapsLock: (e: KeyboardEvent) => void }}
 */
export function useCapsLock() {
  const isCapsLockOn = ref(false)

  const checkCapsLock = (event) => {
    if (!event || typeof event.getModifierState !== 'function') return

    // Ctrl/Meta/Alt 조합 키(예: Ctrl+V 붙여넣기)에서는 CapsLock 상태를 갱신하지 않는다.
    if (event.ctrlKey || event.metaKey || event.altKey) return

    const key = event.key
    const isLetterKey =
      typeof key === 'string' &&
      key.length === 1 &&
      key.toLowerCase() !== key.toUpperCase()

    // 문자 키를 입력하거나 CapsLock 키 자체를 눌렀을 때만 상태를 갱신한다.
    if (isLetterKey || key === 'CapsLock') {
      isCapsLockOn.value = event.getModifierState('CapsLock')
    }
  }

  return {
    isCapsLockOn,
    checkCapsLock
  }
}
