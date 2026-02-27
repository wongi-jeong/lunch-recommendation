<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import Header from './components/Header.vue'
import { RouterView } from 'vue-router'

const route = useRoute()
const showHeader = computed(() => {
  const authRoutes = ['/login', '/signup']
  return !authRoutes.includes(route.path)
})
</script>

<template>
  <div class="app">
    <Header v-if="showHeader" />
    <RouterView />
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
</style>
