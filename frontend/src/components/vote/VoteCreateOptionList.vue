<script setup>
const { restaurants, selectedItems, getPhotoUrl, handleImageError, openExternalLink, toggleSelect, getBusinessStatus } =
  defineProps({
    restaurants: {
      type: Array,
      required: true
    },
    selectedItems: {
      type: Object,
      required: true
    },
    getPhotoUrl: {
      type: Function,
      required: true
    },
    handleImageError: {
      type: Function,
      required: true
    },
    openExternalLink: {
      type: Function,
      required: true
    },
    toggleSelect: {
      type: Function,
      required: true
    },
    getBusinessStatus: {
      type: Function,
      required: true
    }
  })
</script>

<template>
  <div class="option-list-section">
    <div class="option-list">
      <div
        v-for="(restaurant, index) in restaurants"
        :key="index"
        class="option-item"
        :class="{ selected: selectedItems.has(index) }"
        @click="toggleSelect(index)"
      >
        <div class="option-img">
          <img
            :src="getPhotoUrl(restaurant)"
            :alt="restaurant.name"
            class="option-img-photo"
            @error="handleImageError"
          />
          <button
            v-if="restaurant.googlePlaceId"
            type="button"
            class="option-external-btn"
            @click.stop="openExternalLink(restaurant)"
            aria-label="Google 지도에서 보기"
          >
            <img src="@/assets/icon-external-link.svg" alt="외부 링크" class="option-external-icon" />
          </button>
        </div>
        <div class="option-text">
          <div class="option-info">
            <span class="option-store-name" :title="restaurant.name">{{ restaurant.name }}</span>
            <span class="option-separator">|</span>
            <span class="option-category">
              {{ (restaurant.categories || [])[0] || '식당' }}
            </span>
            <span class="option-dot">·</span>
            <span class="option-rating">
              <img src="@/assets/icon-star.svg" alt="별점" class="option-star-icon" />
              {{ restaurant.rating ? restaurant.rating.toFixed(1) : '0.0' }}
            </span>
            <span class="option-dot">·</span>
            <span
              class="option-status"
              :class="{
                'status-open': restaurant.openNow === true,
                'status-closed': restaurant.openNow === false
              }"
            >
              {{ getBusinessStatus(restaurant) }}
            </span>
          </div>
        </div>
        <div class="option-check">
          <svg
            v-if="selectedItems.has(index)"
            width="32"
            height="32"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531" />
            <path
              d="M10 16l4 4 8-8"
              stroke="white"
              stroke-width="2.5"
              stroke-linecap="round"
              stroke-linejoin="round"
            />
          </svg>
          <svg
            v-else
            width="32"
            height="32"
            viewBox="0 0 32 32"
            fill="none"
            xmlns="http://www.w3.org/2000/svg"
          >
            <circle cx="16" cy="16" r="15" stroke="#BDC1C6" stroke-width="2" />
          </svg>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped src="./VoteCreateOptionList.css"></style>
