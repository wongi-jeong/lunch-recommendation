<script setup>
import { ref, computed } from 'vue'
import { useRouter } from 'vue-router'
import { useCapsLock } from '@/composables/useCapsLock'
import { useEmailValidation } from '@/composables/useEmailValidation'
import visibilityIcon from '@/assets/icon-password-visible.svg'
import visibilityOffIcon from '@/assets/icon-password-hidden.svg'
import logoImage from '@/assets/logo-mechu.svg'

const router = useRouter()

const email = ref('')
const password = ref('')
const passwordConfirm = ref('')

const showPassword = ref(false)
const showPasswordConfirm = ref(false)
const focusedField = ref(null) // 'password' | 'password-confirm' | null
const { isCapsLockOn, checkCapsLock } = useCapsLock()

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const togglePasswordConfirmVisibility = () => {
  showPasswordConfirm.value = !showPasswordConfirm.value
}

// 비밀번호 검증: 영문 대소문자/숫자/특수문자 중 2가지 이상 조합, 8자 ~ 32자
const PASSWORD_REGEX = /^(?=.*[a-zA-Z])(?=.*[0-9])[a-zA-Z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]{8,32}$|^(?=.*[a-zA-Z])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?])[a-zA-Z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]{8,32}$|^(?=.*[0-9])(?=.*[!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?])[a-zA-Z0-9!@#$%^&*()_+\-=[\]{};':"\\|,.<>/?]{8,32}$/

const { isValid: isEmailValid, error: emailError, suggestion: emailSuggestion } = useEmailValidation(email)
const isPasswordValid = computed(() => PASSWORD_REGEX.test(password.value))
const isPasswordMatch = computed(() => password.value === passwordConfirm.value && passwordConfirm.value.length > 0)

const isFormValid = computed(() =>
  isEmailValid.value && isPasswordValid.value && isPasswordMatch.value
)

const isSubmitting = ref(false)
const submitError = ref('')
const emailDuplicateError = ref('')

const handleSubmit = async () => {
  if (!isFormValid.value || isSubmitting.value) return
  isSubmitting.value = true
  submitError.value = ''
  emailDuplicateError.value = ''
  try {
    const res = await fetch('/api/auth/signup', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: email.value.trim(),
        password: password.value
      })
    })
    const data = await res.json()
    if (!res.ok) {
      const msg = data.message || '회원가입에 실패했습니다. 다시 시도해주세요.'
      if (res.status === 409 || (msg && msg.includes('이미 등록된'))) {
        emailDuplicateError.value = msg
      } else {
        submitError.value = msg
      }
      return
    }
    router.push('/signup/complete')
  } catch (err) {
    submitError.value = '네트워크 오류가 발생했습니다. 다시 시도해주세요.'
  } finally {
    isSubmitting.value = false
  }
}

const goToLogin = () => {
  router.push('/login')
}
</script>

<template>
  <div class="sign-up-page">
    <main class="sign-up-main">
      <div class="form-section">
        <button class="logo-button" type="button" aria-label="메인 페이지로 이동" @click="router.push('/')">
          <img :src="logoImage" alt="MECHU" class="logo-image" />
        </button>
        <div class="sign-up-card">
        <h1 class="sign-up-title">회원가입</h1>

        <form class="sign-up-form" @submit.prevent="handleSubmit" novalidate>
          <div class="form-field">
            <label for="email" class="form-label">
              이메일 <span class="required">*</span>
            </label>
            <div class="form-input-wrapper">
              <input
                id="email"
                v-model="email"
                type="email"
                class="form-input"
                placeholder="이메일을 입력해주세요"
                autocomplete="email"
                aria-required="true"
                aria-invalid="(!isEmailValid && email.length > 0) || !!emailDuplicateError"
                aria-describedby="email-error email-duplicate-error"
                @input="emailDuplicateError = ''"
              />
            </div>
            <p v-if="email.length > 0 && !isEmailValid" id="email-error" class="form-error" role="alert">
              {{ emailError }}
              <button
                v-if="emailSuggestion && emailSuggestion.includes('@')"
                type="button"
                class="suggestion-link"
                :aria-label="`${emailSuggestion}로 변경`"
                @click="email = emailSuggestion"
              >
                {{ emailSuggestion }}로 적용
              </button>
            </p>
            <p v-if="emailDuplicateError" id="email-duplicate-error" class="form-error" role="alert">
              {{ emailDuplicateError }}
            </p>
          </div>

          <div class="form-field">
            <label for="password" class="form-label">
              비밀번호 <span class="required">*</span>
            </label>
            <div class="form-input-wrapper">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                class="form-input form-input-with-icon"
                placeholder="비밀번호를 입력해주세요"
                autocomplete="new-password"
                aria-required="true"
                aria-invalid="!isPasswordValid && password.length > 0"
                :aria-describedby="isCapsLockOn && focusedField === 'password' ? 'password-hint password-capslock password-error' : 'password-hint password-error'"
                @keydown="checkCapsLock"
                @focus="focusedField = 'password'"
                @blur="focusedField = null"
              />
              <button
                type="button"
                class="visibility-toggle"
                :aria-label="showPassword ? '비밀번호 숨기기' : '비밀번호 보기'"
                @click="togglePasswordVisibility"
              >
                <img
                  :src="showPassword ? visibilityOffIcon : visibilityIcon"
                  alt=""
                  width="24"
                  height="24"
                  aria-hidden="true"
                />
              </button>
            </div>
            <p id="password-hint" class="form-hint">
              영문 대소문자/숫자/특수문자 중 2가지 이상 조합, 8자 ~ 32자
            </p>
            <p
              v-if="isCapsLockOn && focusedField === 'password'"
              id="password-capslock"
              class="capslock-warning"
              role="alert"
              aria-live="polite"
            >
              Caps Lock이 켜져 있습니다
            </p>
            <p v-if="password.length > 0 && !isPasswordValid" id="password-error" class="form-error" role="alert">
              비밀번호 조건을 확인해주세요
            </p>
          </div>

          <div class="form-field">
            <label for="password-confirm" class="form-label">
              비밀번호 확인 <span class="required">*</span>
            </label>
            <div class="form-input-wrapper">
              <input
                id="password-confirm"
                v-model="passwordConfirm"
                :type="showPasswordConfirm ? 'text' : 'password'"
                class="form-input form-input-with-icon"
                placeholder="비밀번호를 입력해주세요"
                autocomplete="new-password"
                aria-required="true"
                aria-invalid="!isPasswordMatch && passwordConfirm.length > 0"
                :aria-describedby="isCapsLockOn && focusedField === 'password-confirm' ? 'password-confirm-capslock password-confirm-error' : 'password-confirm-error'"
                @keydown="checkCapsLock"
                @focus="focusedField = 'password-confirm'"
                @blur="focusedField = null"
              />
              <button
                type="button"
                class="visibility-toggle"
                :aria-label="showPasswordConfirm ? '비밀번호 숨기기' : '비밀번호 보기'"
                @click="togglePasswordConfirmVisibility"
              >
                <img
                  :src="showPasswordConfirm ? visibilityOffIcon : visibilityIcon"
                  alt=""
                  width="24"
                  height="24"
                  aria-hidden="true"
                />
              </button>
            </div>
            <p
              v-if="isCapsLockOn && focusedField === 'password-confirm'"
              id="password-confirm-capslock"
              class="capslock-warning"
              role="alert"
              aria-live="polite"
            >
              Caps Lock이 켜져 있습니다
            </p>
            <p v-if="passwordConfirm.length > 0 && !isPasswordMatch" id="password-confirm-error" class="form-error" role="alert">
              비밀번호가 일치하지 않습니다
            </p>
          </div>

          <p v-if="submitError" class="form-error submit-error" role="alert">
            {{ submitError }}
          </p>
          <button
            type="submit"
            class="submit-button"
            :class="{ 'submit-button--active': isFormValid && !isSubmitting }"
            :disabled="!isFormValid || isSubmitting"
            aria-disabled="!isFormValid || isSubmitting"
          >
            {{ isSubmitting ? '가입 중...' : '회원가입' }}
          </button>
        </form>

        <p class="login-link-wrapper">
          이미 계정이 있으신가요?
          <button type="button" class="login-link" @click="goToLogin">
            로그인
          </button>
        </p>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped src="./SignUpPage.css"></style>
