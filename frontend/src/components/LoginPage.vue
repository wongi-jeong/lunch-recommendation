<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCapsLock } from '@/composables/useCapsLock'
import { useAuth } from '@/composables/useAuth'
import visibilityIcon from '@/assets/visibility-icon.svg'
import visibilityOffIcon from '@/assets/visibility-off-icon.svg'
import logoImage from '@/assets/logo-mechu.svg'

const router = useRouter()
const route = useRoute()
const { setFromMemberResponse } = useAuth()

const email = ref('')
const password = ref('')
const showPassword = ref(false)
const loginError = ref('')
const isSubmitting = ref(false)
const { isCapsLockOn, checkCapsLock } = useCapsLock()

// 입력 시 에러 메시지 초기화 (최신 UX: 실수 수정 시 안내 제거)
watch([email, password], () => {
  if (loginError.value) loginError.value = ''
})

const togglePasswordVisibility = () => {
  showPassword.value = !showPassword.value
}

const dismissError = () => {
  loginError.value = ''
}

const EMAIL_REGEX = /^[^\s@]+@[^\s@]+\.[^\s@]+$/

const isEmailValid = computed(() => EMAIL_REGEX.test(email.value))
const isPasswordFilled = computed(() => password.value.length > 0)

const isFormValid = computed(() => isEmailValid.value && isPasswordFilled.value)

const handleSubmit = async () => {
  if (!isFormValid.value || isSubmitting.value) return
  loginError.value = ''
  isSubmitting.value = true
  try {
    const res = await fetch('/api/auth/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        email: email.value.trim(),
        password: password.value
      })
    })
    const data = await res.json()
    if (!res.ok) {
      loginError.value = data.message || '로그인에 실패했습니다. 입력 정보를 확인해주세요.'
      return
    }
    setFromMemberResponse(data)
    const returnUrl = route.query.returnUrl
    if (returnUrl === '/vote/create') {
      const pendingData = sessionStorage.getItem('pendingVoteCreateData')
      if (pendingData) {
        try {
          sessionStorage.removeItem('pendingVoteCreateData')
        } catch (_) {}
        router.push({ path: '/vote/create', query: { data: pendingData } })
      } else {
        router.push('/vote/create')
      }
    } else if (returnUrl && typeof returnUrl === 'string' && returnUrl.startsWith('/')) {
      router.push(returnUrl)
    } else {
      router.push('/')
    }
  } catch (err) {
    loginError.value = '네트워크 오류가 발생했습니다. 다시 시도해주세요.'
  } finally {
    isSubmitting.value = false
  }
}

const goToForgotPassword = () => {
  // TODO: 비밀번호 찾기 페이지
  console.log('비밀번호 찾기')
}

const goToSignUp = () => {
  router.push('/signup')
}
</script>

<template>
  <div class="login-page">
    <main class="login-main">
      <div class="form-section">
        <button class="logo-button" type="button" aria-label="메인 페이지로 이동" @click="router.push('/')">
          <img :src="logoImage" alt="MECHU" class="logo-image" />
        </button>
        <div class="login-card">
        <h1 class="login-title">로그인</h1>

        <!-- 로그인 실패 시: 최신 트렌드 인라인 알림 배너 (dismissible, 아이콘, 애니메이션) -->
        <Transition name="alert">
          <div
            v-if="loginError"
            class="login-alert"
            role="alert"
            aria-live="assertive"
            aria-describedby="login-error-message"
          >
            <span class="login-alert-icon" aria-hidden="true">
              <svg width="20" height="20" viewBox="0 0 20 20" fill="none" xmlns="http://www.w3.org/2000/svg">
                <path fill-rule="evenodd" clip-rule="evenodd" d="M10 18a8 8 0 1 0 0-16 8 8 0 0 0 0 16ZM9 6a1 1 0 0 1 2 0v4a1 1 0 0 1-2 0V6Zm1 8a1.25 1.25 0 1 0 0-2.5 1.25 1.25 0 0 0 0 2.5Z" fill="currentColor"/>
              </svg>
            </span>
            <p id="login-error-message" class="login-alert-message">{{ loginError }}</p>
            <button
              type="button"
              class="login-alert-dismiss"
              aria-label="알림 닫기"
              @click="dismissError"
            >
              <svg width="16" height="16" viewBox="0 0 16 16" fill="none" xmlns="http://www.w3.org/2000/svg" aria-hidden="true">
                <path d="M4 4l8 8M12 4l-8 8" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              </svg>
            </button>
          </div>
        </Transition>

        <form class="login-form" @submit.prevent="handleSubmit" novalidate>
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
              올바른 이메일 형식을 입력해주세요
            </p>
          </div>

          <div class="form-field">
            <div class="form-label-row">
              <label for="password" class="form-label">
                비밀번호 <span class="required">*</span>
              </label>
              <button
                type="button"
                class="forgot-password-link"
                @click="goToForgotPassword"
              >
                비밀번호를 잊으셨나요?
              </button>
            </div>
            <div class="form-input-wrapper">
              <input
                id="password"
                v-model="password"
                :type="showPassword ? 'text' : 'password'"
                class="form-input form-input-with-icon"
                placeholder="비밀번호를 입력해주세요"
                autocomplete="current-password"
                aria-required="true"
                :aria-describedby="isCapsLockOn ? 'password-capslock password-error' : 'password-error'"
                @keydown="checkCapsLock"
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
            <p
              v-if="isCapsLockOn"
              id="password-capslock"
              class="capslock-warning"
              role="alert"
              aria-live="polite"
            >
              Caps Lock이 켜져 있습니다
            </p>
          </div>

          <button
            type="submit"
            class="submit-button"
            :class="{ 'submit-button--active': isFormValid && !isSubmitting }"
            :disabled="!isFormValid || isSubmitting"
            aria-disabled="!isFormValid || isSubmitting"
          >
            {{ isSubmitting ? '로그인 중...' : '로그인' }}
          </button>
        </form>

        <p class="signup-link-wrapper">
          아직 계정이 없으신가요?
          <button type="button" class="signup-link" @click="goToSignUp">
            회원가입
          </button>
        </p>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped>
.login-page {
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

.login-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  overflow-y: auto;
}

.login-card {
  width: 100%;
  max-width: 480px;
  padding: 48px 40px;
  background-color: #ffffff;
  border-radius: 16px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
}

.login-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #202124;
  margin: 0 0 20px;
}

/* 로그인 실패 알림 배너 — 인라인, 닫기 가능, 부드러운 등장/퇴장 */
.login-alert {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 14px 16px;
  margin-bottom: 20px;
  background: #fef2f2;
  border: 1px solid #fecaca;
  border-radius: 12px;
  box-sizing: border-box;
}

.login-alert-icon {
  flex-shrink: 0;
  color: #dc2626;
  margin-top: 2px;
}

.login-alert-message {
  flex: 1;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.45;
  color: #991b1b;
  margin: 0;
  min-width: 0;
}

.login-alert-dismiss {
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  padding: 0;
  border: none;
  background: transparent;
  color: #b91c1c;
  cursor: pointer;
  border-radius: 8px;
  transition: background-color 0.2s, color 0.2s;
}

.login-alert-dismiss:hover {
  background: rgba(185, 28, 28, 0.1);
  color: #991b1b;
}

.login-alert-dismiss:focus-visible {
  outline: 2px solid #dc2626;
  outline-offset: 2px;
}

/* 알림 등장/퇴장 트랜지션 */
.alert-enter-active,
.alert-leave-active {
  transition: opacity 0.2s ease, transform 0.2s ease;
}

.alert-enter-from,
.alert-leave-to {
  opacity: 0;
  transform: translateY(-8px);
}

/* 실패 시 카드 흔들림 (주의 유도) */
.login-card--shake {
  animation: login-shake 0.5s ease-in-out;
}

@keyframes login-shake {
  0%, 100% { transform: translateX(0); }
  10%, 30%, 50%, 70%, 90% { transform: translateX(-6px); }
  20%, 40%, 60%, 80% { transform: translateX(6px); }
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.form-field {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.form-label-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
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

.forgot-password-link {
  padding: 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #9aa0a6;
  background: none;
  border: none;
  border-bottom: 1px solid #9aa0a6;
  cursor: pointer;
  white-space: nowrap;
  transition: color 0.2s, border-color 0.2s;
}

.forgot-password-link:hover {
  color: #5f6368;
  border-bottom-color: #5f6368;
}

.forgot-password-link:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
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

.form-error {
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #ff5531;
  margin: 0;
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

.signup-link-wrapper {
  margin: 24px 0 0;
  font-family: 'Pretendard', sans-serif;
  font-weight: 500;
  font-size: 14px;
  line-height: 1.35;
  color: #5f6368;
  text-align: center;
}

.signup-link {
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

.signup-link:hover {
  color: #1557b0;
  border-bottom-color: #1557b0;
}

.signup-link:focus-visible {
  outline: 2px solid #1a73e8;
  outline-offset: 2px;
}
</style>
