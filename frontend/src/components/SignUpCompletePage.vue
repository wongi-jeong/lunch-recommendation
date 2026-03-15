<script setup>
import { useRouter } from 'vue-router'
import logoImage from '@/assets/logo-mechu.svg'

const router = useRouter()

const goToLogin = () => {
  router.push('/login')
}

const goToHome = () => {
  router.push('/')
}
</script>

<template>
  <div class="signup-complete-page">
    <main class="signup-complete-main">
      <div class="form-section">
        <button
          class="logo-button"
          type="button"
          aria-label="메인 페이지로 이동"
          @click="goToHome"
        >
          <img :src="logoImage" alt="MECHU" class="logo-image" />
        </button>
        <div class="signup-result-card">
          <h1 class="signup-result-title">가입이 완료되었습니다!</h1>
          <div class="signup-result-actions">
            <button type="button" class="login-button" @click="goToLogin">
              로그인하러 가기
            </button>
          </div>
        </div>
      </div>
    </main>
  </div>
</template>

<style scoped src="./SignUpCompletePage.css"></style>
