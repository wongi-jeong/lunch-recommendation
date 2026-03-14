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

<style scoped>
.main-page {
  --Color-Primary-P-500: #ff5531;
  --section-gap: 48px;
  --utensil-rotate-deg: 12deg;
  --transition-duration: 0.4s;
  flex: 1;
  min-height: 0;
  display: grid;
  /* 수평 3등분: 왼쪽 1/3 | 중앙 1/3 | 오른쪽 1/3 */
  grid-template-columns: 1fr 1fr 1fr;
  grid-template-areas: 'left center right';
  align-items: center;
  justify-items: center;
  gap: 0 var(--section-gap);
  background-color: #FEF7DF;
  overflow-y: auto;
  padding: 40px 24px;
  width: 100%;
}

.title-top,
.title-bottom {
  color: var(--Color-Primary-P-500, #ff5531);
  font-family: 'Nabla', sans-serif;
  font-size: 64px;
  font-style: normal;
  font-weight: 400;
  line-height: normal;
  margin: 0;
  text-align: center;
}

.left-section {
  grid-area: left;
  justify-self: center;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  /* 고정 영역으로 레이아웃 시프트 방지 */
  position: relative;
  contain: layout;
}

.center-section {
  grid-area: center;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 24px;
  min-width: 0;
  max-width: 900px;
  width: 100%;
}

.right-section {
  grid-area: right;
  justify-self: center;
  width: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  contain: layout;
}

/* 버튼 슬롯: 고정 크기로 전환 시 레이아웃 시프트 방지 */
.recommend-btn-slot {
  width: 200px;
  height: 200px;
  min-width: 200px;
  min-height: 200px;
  max-width: 200px;
  max-height: 200px;
  flex-shrink: 0;
  display: flex;
  align-items: center;
  justify-content: center;
}

/* 일러스트 영역 */
.illustration-scale-wrap {
  position: relative;
  width: 100%;
  height: 440px;
  display: flex;
  align-items: center;
  justify-content: center;
  margin: 8px 0;
}


.recommend-btn {
  display: inline-flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12px;
  width: 200px;
  height: 200px;
  min-width: 200px;
  min-height: 200px;
  padding: 16px;
  border-radius: 50%;
  aspect-ratio: 1;
  box-sizing: border-box;
  flex-shrink: 0;
  background-color: var(--Color-Primary-P-500);
  color: #fff;
  border: none;
  font-family: 'Pretendard', sans-serif;
  font-weight: 600;
  font-size: 18px;
  line-height: 1.35;
  text-align: center;
  text-decoration: none;
  cursor: pointer;
  box-shadow: 0 4px 20px rgba(255, 85, 49, 0.4);
  transition: transform 0.2s, box-shadow 0.2s;
  overflow: hidden;
}

.recommend-btn:hover {
  transform: scale(1.02);
  box-shadow: 0 6px 20px rgba(255, 85, 49, 0.4);
}

@media (prefers-reduced-motion: reduce) {
  .recommend-btn:hover {
    transform: none;
  }
}

.recommend-btn:focus-visible {
  outline: 2px solid var(--Color-Primary-P-500);
  outline-offset: 2px;
}

.recommend-btn-text {
  max-width: 150px;
  padding: 0 4px;
  word-break: keep-all;
  line-height: 1.35;
  font-size: 18px;
  font-weight: 700;
}

.recommend-btn-arrow {
  width: 28px;
  height: 28px;
  flex-shrink: 0;
  filter: brightness(0) invert(1);
}

/* 버튼 페이드 전환 */
.btn-fade-left-enter-active,
.btn-fade-left-leave-active,
.btn-fade-right-enter-active,
.btn-fade-right-leave-active {
  transition: opacity 0.35s ease, transform 0.35s cubic-bezier(0.4, 0, 0.2, 1);
}

@media (prefers-reduced-motion: reduce) {
  .btn-fade-left-enter-active,
  .btn-fade-left-leave-active,
  .btn-fade-right-enter-active,
  .btn-fade-right-leave-active {
    transition-duration: 0.01ms;
  }
}

.btn-fade-left-enter-from,
.btn-fade-left-leave-to {
  opacity: 0;
  transform: translateX(-16px);
}

.btn-fade-left-enter-to,
.btn-fade-left-leave-from {
  opacity: 1;
  transform: translateX(0);
}

.btn-fade-right-enter-from,
.btn-fade-right-leave-to {
  opacity: 0;
  transform: translateX(16px);
}

.btn-fade-right-enter-to,
.btn-fade-right-leave-from {
  opacity: 1;
  transform: translateX(0);
}

.illustration-wrapper {
  position: absolute;
  left: 50%;
  top: 50%;
  transform: translate(-50%, -50%) scale(0.45);
  transform-origin: center center;
  display: flex;
  align-items: flex-end;
  justify-content: center;
  gap: 0;
  width: 1512px;
  height: 976px;
}

/* 스푼/포크: absolute로 문서 흐름에서 제거 → 회전 시 밀침 없음 */
.utensil-wrap {
  position: absolute;
  bottom: 0;
  display: flex;
  align-items: flex-end;
  justify-content: center;
}

.utensil-wrap--spoon {
  left: 0;
  width: 292px;
  height: 976px;
}

.utensil-wrap--fork {
  right: 0;
  width: 235px;
  height: 976px;
}

.utensil {
  flex-shrink: 0;
  object-fit: contain;
  transform-origin: bottom center;
  transition: transform var(--transition-duration) cubic-bezier(0.4, 0, 0.2, 1);
}

@media (prefers-reduced-motion: reduce) {
  .utensil {
    transition: none;
  }

  .utensil-wrap.is-left-hover .utensil,
  .utensil-wrap.is-right-hover .utensil {
    transform: none;
  }
}

.utensil.spoon {
  width: 292px;
  height: 976px;
}

.utensil.fork {
  width: 235px;
  height: 976px;
}

.utensil-wrap.is-left-hover .utensil {
  transform: rotate(calc(-1 * var(--utensil-rotate-deg)));
}

.utensil-wrap.is-right-hover .utensil {
  transform: rotate(var(--utensil-rotate-deg));
}

.plate-wrapper {
  position: absolute;
  left: 50%;
  bottom: 0;
  transform: translateX(-50%);
  display: flex;
  align-items: center;
  justify-content: center;
  width: 953px;
  height: 957px;
}

.plate {
  width: 953px;
  height: 957px;
  object-fit: contain;
  display: block;
}

.hamburger {
  position: absolute;
  width: 400px;
  height: 400px;
  object-fit: contain;
  display: block;
}
</style>
