<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import { RouterView } from 'vue-router'

const route = useRoute()
// 로그인·회원가입 페이지만 헤더 숨김. 마이페이지(/my) 포함 나머지 모든 페이지에 헤더 표시
const showHeader = computed(() => {
  const noHeaderPaths = ['/login', '/signup']
  return !noHeaderPaths.includes(route.path)
})
</script>

<template>
  <div class="app">
    <Header v-if="showHeader" class="app-header" />
    <main class="app-main" role="main">
      <RouterView />
    </main>
  </div>
</template>

<style>
/* 전역 레이아웃: 브라우저 기본 스크롤 사용 */
html,
body {
  min-height: 100%;
  margin: 0;
}
#app {
  min-height: 100vh;
}
</style>

<style scoped>
.app {
  width: 100%;
  min-height: 100vh;
  display: flex;
  flex-direction: column;
}

.app-header {
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  display: flex;
  flex-direction: column;
}
</style>
