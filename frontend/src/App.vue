<script setup>
import Header from './components/Header.vue'
import NearbyRecommendation from './components/NearbyRecommendation.vue'
</script>

<template>
  <div class="app">
    <Header />
    <NearbyRecommendation />
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
