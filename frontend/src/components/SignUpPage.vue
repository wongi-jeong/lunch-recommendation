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

const handleSubmit = async () => {
  if (!isFormValid.value || isSubmitting.value) return
  isSubmitting.value = true
  submitError.value = ''
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
      submitError.value = data.message || '회원가입에 실패했습니다. 다시 시도해주세요.'
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
                aria-invalid="!isEmailValid && email.length > 0"
                aria-describedby="email-error"
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

<style scoped>
.sign-up-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #e8eaed;
}

.form-section {
  width: 100%;
  max-width: 480px;
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.logo-button {
  align-self: flex-start;
  padding: 8px;
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s, transform 0.2s;
}

.logo-button:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: scale(1.02);
}

.logo-button:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.logo-button .logo-image {
  width: 92px;
  height: 35px;
  display: block;
  object-fit: contain;
}

.sign-up-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  overflow-y: auto;
}

.sign-up-card {
  width: 100%;
  max-width: 480px;
  padding: 48px 40px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
}

.sign-up-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #202124;
  margin: 0 0 32px;
}

.sign-up-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 16px;
  line-height: 1.35;
  color: #202124;
}

.required {
  color: #ff5531;
}

.form-input-wrapper {
  position: relative;
  display: flex;
  align-items: center;
}

.form-input {
  width: 100%;
  height: 48px;
  padding: 12px 16px;
  font-family: 'Pretendard', sans-serif;
  font-size: 16px;
  line-height: 1.35;
  color: #202124;
  background-color: #ffffff;
  border: 1px solid #dadce0;
  border-radius: 12px;
  outline: none;
  transition: border-color 0.2s, box-shadow 0.2s;
}

.form-input::placeholder {
  color: #9aa0a6;
}

.form-input:hover {
  border-color: #bdc1c6;
}

.form-input:focus {
  border-color: #ff5531;
  box-shadow: 0 0 0 2px rgba(255, 85, 49, 0.2);
}

.form-input-with-icon {
  padding-right: 48px;
}

.visibility-toggle {
  position: absolute;
  right: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s;
}

.visibility-toggle:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.visibility-toggle:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.form-hint {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #9aa0a6;
  margin: 0;
}

.form-error {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.5;
  color: #ff5531;
  margin: 0;
}

.submit-error {
  margin-top: 4px;
}

.suggestion-link {
  display: block;
  margin-top: 6px;
  padding: 0;
  font-family: inherit;
  font-size: 13px;
  font-weight: 600;
  color: #1a73e8;
  background: none;
  border: none;
  text-decoration: underline;
  cursor: pointer;
  transition: color 0.2s;
}

.suggestion-link:hover {
  color: #1557b0;
}

.capslock-warning {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #ea8600;
  margin: 4px 0 0;
}

.submit-button {
  width: 100%;
  height: 52px;
  margin-top: 8px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 18px;
  line-height: 1.35;
  color: #ffffff;
  background-color: #dadce0;
  border: none;
  border-radius: 14px;
  cursor: not-allowed;
  transition: background-color 0.2s;
}

.submit-button--active {
  background-color: #ff5531;
  cursor: pointer;
}

.submit-button--active:hover {
  background-color: #e64a28;
}

.submit-button--active:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}

.login-link-wrapper {
  margin: 24px 0 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #5f6368;
  text-align: center;
}

.login-link {
  margin-left: 4px;
  padding: 0;
  font-family: inherit;
  font-weight: 600;
  font-size: inherit;
  color: #1a73e8;
  background: none;
  border: none;
  border-bottom: 1px solid #1a73e8;
  cursor: pointer;
  transition: color 0.2s;
}

.login-link:hover {
  color: #1557b0;
  border-bottom-color: #1557b0;
}

.login-link:focus-visible {
  outline: 2px solid #1a73e8;
  outline-offset: 2px;
}
</style>
