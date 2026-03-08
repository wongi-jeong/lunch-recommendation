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

<style scoped>
.signup-complete-page {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  background-color: #f1f3f4;
}

.form-section {
  width: 100%;
  max-width: 486px;
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

.signup-complete-main {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 48px 24px;
  overflow-y: auto;
}

.signup-result-card {
  width: 100%;
  padding: 64px 48px 96px;
  background-color: #ffffff;
  border-radius: 56px;
  box-shadow: 0 0 48px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  gap: 32px;
  align-items: center;
}

.signup-result-title {
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 24px;
  line-height: 1.35;
  color: #202124;
  margin: 0;
  text-align: center;
}

.signup-result-actions {
  width: 100%;
  display: flex;
  flex-direction: column;
  gap: 24px;
  align-items: center;
}

.login-button {
  width: 100%;
  height: 56px;
  font-family: 'Pretendard', sans-serif;
  font-weight: 700;
  font-size: 20px;
  line-height: 1.35;
  color: #ffffff;
  background-color: #ff5531;
  border: none;
  border-radius: 16px;
  cursor: pointer;
  transition: background-color 0.2s;
}

.login-button:hover {
  background-color: #e64a28;
}

.login-button:focus-visible {
  outline: 2px solid #ff5531;
  outline-offset: 2px;
}
</style>
