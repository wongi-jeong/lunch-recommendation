<script setup>
import { ref, computed, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useCapsLock } from '@/composables/useCapsLock'
import { useAuth } from '@/composables/useAuth'
import visibilityIcon from '@/assets/icon-password-visible.svg'
import visibilityOffIcon from '@/assets/icon-password-hidden.svg'
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

<style scoped src="./LoginPage.css"></style>
