<script setup>
const props = defineProps({
  options: {
    type: Array,
    required: true
  },
  selectedIndex: {
    type: Number,
    default: null
  },
  hasVoted: {
    type: Boolean,
    required: true
  },
  voteEnded: {
    type: Boolean,
    required: true
  },
  winnerIndex: {
    type: Number,
    required: true
  },
  getPercentage: {
    type: Function,
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
  getBusinessStatus: {
    type: Function,
    required: true
  },
  openExternalLink: {
    type: Function,
    required: true
  },
  showError: {
    type: Boolean,
    default: false
  }
})

const emit = defineEmits(['select'])

const handleSelect = (index) => {
  emit('select', index)
}
</script>

<template>
  <div class="vote-list-section">
    <p class="vote-subtitle">과연 오늘의 점심은...?!</p>
    <div class="vote-list">
      <div
        v-for="(option, index) in options"
        :key="index"
        class="vote-item"
        :class="{
          'vote-item-selected': selectedIndex === index && !voteEnded,
          'vote-item-winner': voteEnded && index === winnerIndex,
          'vote-item-result': (hasVoted || voteEnded) && selectedIndex === index && !voteEnded,
          'vote-item-disabled': voteEnded
        }"
        @click="handleSelect(index)"
      >
        <div class="vote-item-img">
          <img
            :src="getPhotoUrl(option)"
            :alt="option.name"
            class="vote-item-photo"
            @error="handleImageError"
          />
          <button
            v-if="option.googlePlaceId"
            type="button"
            class="vote-item-external-btn"
            @click.stop="openExternalLink(option)"
            aria-label="Google 지도에서 보기"
          >
            <img src="@/assets/external-link-icon.svg" alt="외부 링크" class="vote-item-external-icon" />
          </button>
        </div>
        <div class="vote-item-body">
          <div class="vote-item-text">
            <div class="vote-item-info">
              <span class="vote-item-name" :title="option.name">{{ option.name }}</span>
              <span class="vote-item-separator">|</span>
              <span class="vote-item-category">
                {{ (option.categories || [])[0] || '식당' }}
              </span>
              <span class="vote-item-dot">·</span>
              <span class="vote-item-rating">
                <img src="@/assets/star-icon.svg" alt="별점" class="vote-item-star-icon" />
                {{ option.rating != null ? option.rating.toFixed(1) : '0.0' }}
              </span>
              <span class="vote-item-dot">·</span>
              <span
                class="vote-item-status"
                :class="{
                  'status-open': option.openNow === true,
                  'status-closed': option.openNow === false
                }"
              >
                {{ getBusinessStatus(option) }}
              </span>
            </div>
            <span class="vote-item-pct">{{ getPercentage(index) }}%</span>
          </div>
          <div
            class="vote-progress-bar"
            :class="{ 'progress-selected': selectedIndex === index || (voteEnded && index === winnerIndex) }"
          >
            <div
              class="vote-progress-fill"
              :class="{ 'fill-selected': selectedIndex === index || (voteEnded && index === winnerIndex) }"
              :style="{ width: getPercentage(index) + '%' }"
            ></div>
          </div>
        </div>
        <div class="vote-item-check">
          <template v-if="!hasVoted && !voteEnded">
            <svg
              v-if="selectedIndex === index"
              width="32"
              height="32"
              viewBox="0 0 32 32"
              fill="none"
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
            >
              <circle cx="16" cy="16" r="15" stroke="#BDC1C6" stroke-width="2" />
            </svg>
          </template>
          <template v-else-if="hasVoted && selectedIndex === index && !voteEnded">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531" />
              <path
                d="M10 16l4 4 8-8"
                stroke="white"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </template>
          <template v-else-if="voteEnded && index === winnerIndex">
            <svg width="32" height="32" viewBox="0 0 32 32" fill="none">
              <circle cx="16" cy="16" r="15" stroke="#FF5531" stroke-width="2" fill="#FF5531" />
              <path
                d="M10 16l4 4 8-8"
                stroke="white"
                stroke-width="2.5"
                stroke-linecap="round"
                stroke-linejoin="round"
              />
            </svg>
          </template>
        </div>
      </div>
    </div>
    <p v-if="showError" class="vote-error-text">항목을 선택해주세요</p>
  </div>
</template>
