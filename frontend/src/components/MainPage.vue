<script setup>
import { ref } from 'vue'
import forkImg from '@/assets/icon-fork.svg'
import spoonImg from '@/assets/icon-spoon.svg'
import plateImg from '@/assets/icon-plate.svg'
import mainHamburgerImg from '@/assets/img-main-hero.png'
import arrowRightIcon from '@/assets/icon-arrow-right.svg'

const isMouseLeft = ref(false)
const isMouseRight = ref(false)
</script>

<template>
  <main class="main-page">
    <aside
      class="left-section"
      aria-label="내 근처 추천 버튼 영역"
      @pointerenter="isMouseLeft = true"
      @pointerleave="isMouseLeft = false"
    >
      <div class="recommend-btn-slot">
        <transition name="btn-fade-left">
          <router-link
            v-if="isMouseLeft"
            :to="{ name: 'nearby' }"
            class="recommend-btn"
            aria-label="내 근처 점심 추천 받기"
          >
            <span class="recommend-btn-text">내 근처 점심 추천 받기</span>
            <img :src="arrowRightIcon" alt="" class="recommend-btn-arrow" aria-hidden="true" />
          </router-link>
        </transition>
      </div>
    </aside>

    <!-- 중앙 섹션: 텍스트 + 일러스트 -->
    <div class="center-section">
      <h1 class="title-top" aria-label="오늘은">To day is...</h1>
      <div class="illustration-scale-wrap">
        <div class="illustration-wrapper">
          <div
            class="utensil-wrap utensil-wrap--spoon"
            :class="{ 'is-left-hover': isMouseLeft, 'is-right-hover': isMouseRight }"
          >
            <img
              :src="spoonImg"
              alt=""
              class="utensil spoon"
              aria-hidden="true"
            />
          </div>
          <div class="plate-wrapper">
            <img
              :src="plateImg"
              alt=""
              class="plate"
              aria-hidden="true"
            />
            <img
              :src="mainHamburgerImg"
              alt="햄버거"
              class="hamburger"
            />
          </div>
          <div
            class="utensil-wrap utensil-wrap--fork"
            :class="{ 'is-left-hover': isMouseLeft, 'is-right-hover': isMouseRight }"
          >
            <img
              :src="forkImg"
              alt=""
              class="utensil fork"
              aria-hidden="true"
            />
          </div>
        </div>
      </div>
      <h2 class="title-bottom" aria-hidden="true">???</h2>
    </div>

    <aside
      class="right-section"
      aria-label="AI 추천 버튼 영역"
      @pointerenter="isMouseRight = true"
      @pointerleave="isMouseRight = false"
    >
      <div class="recommend-btn-slot">
        <transition name="btn-fade-right">
          <router-link
            v-if="isMouseRight"
            :to="{ name: 'ai' }"
            class="recommend-btn recommend-btn--right"
            aria-label="AI 추천받기"
          >
            <span class="recommend-btn-text">AI 추천 받기</span>
            <img :src="arrowRightIcon" alt="" class="recommend-btn-arrow" aria-hidden="true" />
          </router-link>
        </transition>
      </div>
    </aside>
  </main>
</template>

<style scoped src="./MainPage.css"></style>
