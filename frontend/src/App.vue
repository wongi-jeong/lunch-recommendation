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

<style scoped>
.app {
  width: 100%;
  min-height: 100vh;
}
</style>
