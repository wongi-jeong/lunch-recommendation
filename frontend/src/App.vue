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
/* 뷰포트 고정, 스크롤바 방지 */
html, body {
  height: 100%;
  overflow: hidden;
  margin: 0;
}
#app {
  height: 100%;
}
</style>

<style scoped>
.app {
  width: 100%;
  height: 100%;
  min-height: 0;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.app-header {
  flex-shrink: 0;
}

.app-main {
  flex: 1;
  min-height: 0;
  overflow: auto;
  display: flex;
  flex-direction: column;
}
</style>
